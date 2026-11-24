# ADR 0009 — Agregação do relatório de consultas em memória

- **Situação:** aceita
- **Data:** 2026-11-24
- **Autores:** Ludmila Johnston

## Contexto

A missão E3-3 (RF17 — emitir relatórios de consultas) pede totais de consultas por situação, por profissional e por especialidade em um período de até 1 ano. O relatório fica no módulo `painel`, que é um módulo de leitura: ele não tem dados próprios e não pode acessar a persistência da agenda diretamente (regra verificada pelo `ArquiteturaTest`). Os dados precisam chegar pela `AgendaFachada`.

Também está em jogo o desempenho (RNF de tempo de resposta): o relatório não deve deixar o sistema lento.

## Opções consideradas

1. **Agregar em memória (Java):** a `AgendaFachada` devolve as consultas do período (`consultasDoPeriodo`) e o `PainelService` conta com `Collectors.groupingBy`.
2. **Agregar no banco (`GROUP BY`):** criar consultas SQL/JPQL com `GROUP BY` na agenda e expor os totais prontos pela `AgendaFachada`.

## Decisão

Escolhemos a **opção 1, agregação em memória**.

Medimos com o Raio-X: o relatório de um mês (19 consultas) respondeu em **cerca de 21 ms**. Para o volume da clínica (cerca de 20 profissionais, 30 minutos por consulta), um ano tem no máximo algumas dezenas de milhares de consultas, o que ainda é viável em memória. Além disso, a opção 1 reaproveita o `detalhar(...)` do `AgendaService`, que já traz o nome do profissional e a especialidade vindos do `cadastro`. Com `GROUP BY`, a agenda precisaria cruzar dados com as tabelas do cadastro, o que quebraria a separação entre os módulos.

A regra do período (fim não anterior ao início e no máximo 1 ano) ficou no domínio, em `PeriodoDoRelatorio`, com teste próprio. Um período inválido é recusado com HTTP 422 antes de chegar à persistência (confirmado no Raio-X: a requisição passa só por Apresentação e Aplicação).

## Custo e benefício

| | Opção 1 — em memória | Opção 2 — `GROUP BY` no banco |
|---|---|---|
| Benefício principal | Simples e respeita os módulos: o painel só usa a `AgendaFachada` | Mais rápido com muitos dados: o banco devolve só os totais |
| Custo de implementar | Baixo: um método novo na porta e na fachada, contagem no `PainelService` | Médio: consultas novas e um tipo de resposta só para totais na agenda |
| Custo de manter | Baixo: um novo agrupamento é uma linha no `PainelService` | Maior: cada agrupamento novo exige outra consulta no banco |
| Risco | Ficar lento se o volume de consultas crescer muito | Acoplar a agenda ao cadastro (nomes e especialidades) e ao formato do relatório |

## Consequências

- Fica fácil acrescentar novos agrupamentos ao relatório, sem mexer na agenda.
- O período máximo de 1 ano limita a quantidade de consultas carregadas por requisição.
- Se a clínica crescer muito ou o tempo do relatório passar de 2 segundos, a decisão deve ser revista: trocar para `GROUP BY` atrás da mesma `AgendaFachada` (o `PainelService` não precisaria mudar), ou adicionar paginação/cache.