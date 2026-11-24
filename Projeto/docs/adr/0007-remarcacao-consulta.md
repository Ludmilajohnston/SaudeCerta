# ADR 0007 — Remarcação de consultas preservando a identidade

- **Situação:** aceita
- **Data:** 2026-11-24
- **Autores:** Júlia Rodrigues, Marielli Macedo, Ludmila Johnston e Ronaldo Ângelo.

## Contexto

A missão E3-1 estabelece a implementação da remarcação de consultas, permitindo alterar o horário de uma consulta agendada ou confirmada, respeitando as regras de funcionamento da clínica e evitando conflitos de horários.

A solução deve preservar a consistência dos dados, manter a rastreabilidade das alterações e respeitar os limites arquiteturais definidos para o módulo de agenda.

Os requisitos envolvidos são RF06, RF07, RF14, RF15, RNF04, RNF05 e RNF08, conforme a matriz de rastreabilidade.

## Opções consideradas

1. **Opção A — Alterar o horário da consulta existente:** manter o identificador da consulta, atualizar seu horário e registrar os horários anterior e novo na auditoria.
2. **Opção B — Criar uma nova consulta vinculada à anterior:** cancelar a consulta original, criar uma nova consulta e estabelecer um vínculo entre os registros.

## Decisão

Foi escolhida a opção A, pois permite preservar a identidade da consulta e reduzir a complexidade da implementação, evitando a criação de registros adicionais para representar uma remarcação.

O atributo `inicio` da entidade de domínio `Consulta` passou a permitir alterações controladas pelo método `remarcar`.

As regras de validação de horário são compartilhadas entre o agendamento e a remarcação, evitando duplicação de lógica.

O `AgendaService` coordena a verificação de conflitos do paciente e do profissional, a persistência da consulta e o registro da alteração na auditoria.

A consulta remarcada retorna à situação `AGENDADA`, exigindo uma nova confirmação de chegada quando necessário.

## Custo e benefício

| Critério | Opção A — Alterar consulta existente | Opção B — Criar nova consulta vinculada |
|---|---|---|
| Benefício principal | Preserva a identidade e simplifica a operação | Preserva os registros históricos separadamente |
| Custo de implementar | Baixo: reutiliza o domínio e a persistência existentes | Maior: exige novos vínculos e regras de relacionamento |
| Custo de manter | Menor quantidade de registros e regras | Maior complexidade nas consultas e nos relatórios |
| Risco | Dependência da auditoria para recuperar horários anteriores | Duplicidade de registros e inconsistências entre consultas vinculadas |

## Consequências

- A consulta mantém seu identificador, paciente, profissional e demais informações.
- As regras de horário permanecem centralizadas no domínio.
- Somente consultas agendadas ou confirmadas podem ser remarcadas.
- A operação verifica conflitos de horários do paciente e do profissional.
- A auditoria registra o horário anterior, o novo horário e o usuário responsável pela alteração.
- A interface consulta os horários disponíveis antes de confirmar a remarcação.
- Não foi necessária uma alteração na estrutura da tabela de consultas.
- A recuperação dos horários anteriores depende dos registros de auditoria.
- A solução preserva a arquitetura hexagonal definida na ADR 0002 e os limites do monólito modular definidos na ADR 0001.
- A possibilidade de conflitos decorrentes de requisições simultâneas permanece como questão a ser tratada na missão E3-5.

A decisão deverá ser reavaliada caso surja a necessidade de manter versões independentes de cada consulta ou de garantir um histórico de alterações separado da auditoria.