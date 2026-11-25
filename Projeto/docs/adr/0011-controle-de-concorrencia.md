# ADR 0011 — Controle de concorrência na agenda

- **Situação:** aceita
- **Data:** 2026-11-24
- **Autores:** [Nome dos responsáveis pela implementação]

## Contexto

O SaúdeCerta verificava a disponibilidade do profissional e do paciente antes de persistir uma consulta. Entretanto, duas requisições simultâneas poderiam passar pela mesma verificação antes que uma delas concluísse a gravação.

Essa condição de corrida permitia a criação de consultas conflitantes, comprometendo a consistência dos dados.

Requisitos envolvidos: RF06 e RF15.

## Opções consideradas

1. Restrição de unicidade no banco de dados — impedir registros duplicados mediante restrições de integridade.
2. Bloqueio otimista — utilizar controle de versões e detectar conflitos durante a gravação.
3. Bloqueio pessimista — serializar as operações de escrita da agenda por meio de um bloqueio transacional.

## Decisão

Foi adotado o bloqueio pessimista utilizando `PESSIMISTIC_WRITE` do JPA.

Foi criado um registro compartilhado na tabela `bloqueio_agenda`, utilizado pelas operações de escrita da agenda.

O contrato `bloquearAgenda()` foi definido na porta `ConsultaRepositorio` e implementado no adaptador `ConsultaRepositorioJpa`.

O `AgendaService` solicita o bloqueio antes das operações de escrita. Dessa forma, uma requisição concorrente deve aguardar a conclusão da transação anterior antes de verificar a disponibilidade.

A decisão considera a simplicidade da implementação e a necessidade de preservar a arquitetura de portas e adaptadores.

## Custo e benefício

| Critério | Restrição de unicidade | Bloqueio otimista | Bloqueio pessimista |
|---|---|---|---|
| Benefício principal | Integridade no banco | Maior paralelismo | Consistência transacional |
| Custo de implementar | Médio | Médio | Baixo a médio |
| Custo de manter | Médio | Médio | Baixo |
| Risco | Não contemplar sobreposições e cancelamentos com uma restrição simples | Não detectar inserções concorrentes sem um recurso compartilhado | Espera e redução do paralelismo |

## Evidências

A condição de corrida foi reproduzida inicialmente pelo teste de integração `AgendaConcorrenciaTest`, utilizando duas threads e uma barreira de sincronização.

Antes da correção, ambas as requisições retornavam HTTP 201, permitindo agendamentos conflitantes.

Após a implementação do bloqueio pessimista, foram validados os seguintes cenários:

| Cenário | Resultado | Situação |
|---|---|---|
| Agendamento simultâneo do profissional | HTTP 201 e 409 | Aprovado |
| Agendamento simultâneo do paciente | HTTP 201 e 409 | Aprovado |
| Remarcação simultânea | HTTP 200 e 409 | Aprovado |

A execução final de `mvn clean test` apresentou:

- 78 testes executados.
- 0 falhas.
- 0 erros.
- 0 testes ignorados.
- 3 testes de concorrência aprovados.
- 11 testes de arquitetura aprovados.
- Tempo total de 24,654 segundos.
- BUILD SUCCESS.

Os resultados demonstram que o bloqueio pessimista impede os conflitos nos cenários concorrentes testados, preservando as regras de negócio e a arquitetura do sistema.