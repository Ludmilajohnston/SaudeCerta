# ADR 0008 — Persistência de prescrições

**Situação:** Proposta para validação da equipe  
**Data:** 24/11/2026  
**Autores:** Júlia Rodrigues, Marielli Macedo, Ludmila Johnston e Ronaldo Ângelo.

## Contexto

A missão E3-2 (RF16) exige que o profissional responsável possa emitir prescrições durante um atendimento em aberto, respeitando os alertas do paciente e registrando as operações na auditoria.

A implementação deve preservar a arquitetura hexagonal do módulo de prontuário.

## Opções consideradas

**Opção A:** Persistir as prescrições em tabelas relacionais, utilizando JPA e `@ElementCollection` para os medicamentos.

**Opção B:** Armazenar os medicamentos da prescrição em uma estrutura JSON.

## Decisão

Foi adotada a opção A, mantendo as prescrições e seus itens em tabelas relacionais.

O domínio `Prescricao` permanece independente do framework, utilizando a porta de saída `PrescricaoRepositorio` e o adaptador `PrescricaoRepositorioJpa`.

As validações de medicamentos, duplicidade e alertas são realizadas no domínio. O caso de uso verifica a responsabilidade do profissional e a situação do atendimento.

## Custo e benefício

| Critério | Opção A — Relacional | Opção B — JSON |
|---|---|---|
| Benefício principal | Dados estruturados e integração com JPA | Flexibilidade da estrutura |
| Custo de implementação | Moderado | Baixo a moderado |
| Custo de manutenção | Mapeamento das tabelas | Validação e consultas específicas |
| Risco | Consultas adicionais ao carregar históricos | Maior dificuldade de garantir consistência dos itens |

## Consequências

- Preservação da arquitetura hexagonal.
- Persistência das prescrições vinculadas aos atendimentos.
- Separação entre domínio e infraestrutura.
- Controle de acesso pelo profissional responsável.
- Registro de emissão e leitura na auditoria.
- Disponibilização das prescrições no histórico do paciente.
- Possibilidade de consultas adicionais ao carregar históricos extensos.
- A identificação de alergias utiliza comparação textual dos alertas, não substituindo uma base clínica especializada.

## Validação

A implementação foi validada por testes manuais e automatizados.

- 61 testes executados e aprovados.
- Cinco testes específicos do domínio de prescrições aprovados.
- Onze testes de arquitetura aprovados.
- Nenhuma falha, erro ou teste ignorado.
- Emissão, leitura, persistência, histórico, auditoria e restrições de acesso validados.