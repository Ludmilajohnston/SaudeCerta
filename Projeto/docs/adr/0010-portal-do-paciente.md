# ADR 0010 — Portal do paciente: identificação pelo token e 404 para consulta alheia

- **Situação:** aceita
- **Data:** 2026-11-24
- **Autores:** Ludmila Johnston

## Contexto

A missão E3-4 (RF18 — portal do paciente) pede que o paciente veja apenas as próprias consultas e possa cancelar uma consulta futura. O critério mais importante é de segurança e privacidade: não pode existir forma de ver ou mexer na consulta de outro paciente trocando um número na URL, e nenhum dado clínico pode aparecer no portal.

O token JWT já traz o `pacienteId` do usuário logado (`UsuarioLogado.pacienteId()`), e o `SecurityConfig` libera `/api/portal/**` apenas para o perfil PACIENTE.

## Opções consideradas

1. **Endpoints próprios do portal (`/api/portal/consultas`), com o paciente vindo do token.** A URL não tem o id do paciente. Consulta de outro paciente responde **404**, como se não existisse.
2. **Reaproveitar os endpoints da recepção (`/api/consultas/{id}`), com o id do paciente na URL**, e conferir no servidor se ele é o dono, respondendo **403** quando não for.

## Decisão

Escolhemos a **opção 1**.

O paciente nunca informa quem ele é: o servidor usa o `pacienteId` do token, que é assinado e não pode ser alterado. Assim, não existe nenhum número na URL que possa ser trocado para ver a lista de outro paciente. No cancelamento, o id da consulta vem na URL, mas o `AgendaService` só a encontra se `Consulta.pertenceA(pacienteId do token)`; caso contrário, responde 404. Preferimos o 404 ao 403 porque o 403 confirmaria que aquela consulta existe.

O portal devolve um formato próprio (`ConsultaDoPortal`), sem alertas, observações, evolução ou conduta. A regra "só cancela consulta futura" ficou no domínio (`Consulta.cancelarPeloPaciente`), com testes em `ConsultaPortalTest`. O teste de integração `PortalDoPacienteTest` prova que trocar o id na URL resulta em 404 e que a consulta do outro paciente continua intacta.

## Custo e benefício

| | Opção 1 — portal próprio + token + 404 | Opção 2 — endpoints da recepção + 403 |
|---|---|---|
| Benefício principal | Impossível pedir dados de outro paciente pela URL; resposta sem dados clínicos | Menos código: reaproveita endpoints existentes |
| Custo de implementar | Médio: novo controller, DTO e dois casos de uso | Baixo, mas exige verificação de dono em cada endpoint |
| Custo de manter | Baixo: o portal evolui sem afetar a recepção | Alto: qualquer endpoint novo da recepção precisa lembrar da verificação |
| Risco | Duplicar um pouco da lógica de cancelamento (mitigado: o domínio reaproveita `cancelar`) | Esquecer a verificação em um endpoint e vazar dados; o 403 revela que o recurso existe |

## Consequências

- O portal e a recepção ficam separados: mudanças na tela da recepção não expõem dados ao paciente.
- O cancelamento pelo paciente fica registrado na auditoria, como "Cancelada pelo paciente no portal".
- Se o portal ganhar novas funções (por exemplo, remarcar), elas devem seguir o mesmo padrão: paciente vindo do token, verificação `pertenceA` e 404 para recursos de outros pacientes.