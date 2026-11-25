# E3-6 — Testes dos casos de uso sem banco

## 1. Objetivo

Validar os casos de uso dos módulos de agenda e prontuário de forma isolada, sem inicializar o Spring Boot ou utilizar o banco de dados H2, demonstrando os benefícios da arquitetura hexagonal adotada no projeto SaúdeCerta.

## 2. Implementação

Foram desenvolvidas implementações em memória das portas de saída da aplicação:

- `ConsultaRepositorioEmMemoria`: implementação da porta `ConsultaRepositorio`.
- `AtendimentoRepositorioEmMemoria`: implementação da porta `AtendimentoRepositorio`.

Os repositórios utilizam estruturas de dados Java para armazenar os registros, gerar identificadores e realizar consultas.

Os serviços são instanciados diretamente por meio de seus construtores, utilizando injeção de dependências manual.

As dependências complementares são simuladas com Mockito, enquanto os eventos de auditoria são armazenados em uma lista em memória.

Também foi utilizado `Clock.fixed()` para controlar a data e a hora dos testes, garantindo resultados determinísticos e reproduzíveis.

## 3. Cenários testados

### AgendaServiceTest

1. Agendamento de consulta e registro de auditoria.
2. Recusa de conflito de horário do profissional.
3. Recusa de conflito de horário do paciente.
4. Recusa de agendamento em horário passado.
5. Liberação de horário após cancelamento.
6. Remarcação preservando a identidade da consulta.
7. Preservação do horário original em caso de conflito na remarcação.
8. Exclusão de horários ocupados da lista de horários disponíveis.

### ProntuarioServiceTest

1. Início de atendimento e registro de auditoria.
2. Reabertura de atendimento sem duplicação.
3. Impedimento de início por outro profissional.
4. Registro da evolução clínica.
5. Impedimento de alteração por profissional não responsável.
6. Impedimento de finalização sem evolução clínica.
7. Finalização de atendimento e registro de auditoria.
8. Impedimento de alteração após finalização.
9. Consulta ao histórico do paciente com auditoria.

## 4. Resultados

| Classe de teste | Testes | Falhas | Erros | Tempo |
|---|---:|---:|---:|---:|
| AgendaServiceTest | 8 | 0 | 0 | 1,128 s |
| ProntuarioServiceTest | 9 | 0 | 0 | 0,119 s |
| Total sem banco | 17 | 0 | 0 | 1,247 s |
| FluxoDaClinicaTest | 18 | 0 | 0 | 12,460 s |

O tempo total do Maven foi de 4,249 segundos para os testes sem banco e 15,569 segundos para o teste de integração.

A inicialização do contexto Spring Boot no teste de integração levou 8,833 segundos.

Considerando os tempos reportados pelas classes de teste, observou-se uma redução de aproximadamente 90% no tempo de execução.

## 5. Comparação arquitetural

| Característica | Testes dos casos de uso | FluxoDaClinicaTest |
|---|---|---|
| Tipo | Testes unitários da camada de aplicação | Testes de integração |
| Spring Boot | Não inicializado | Inicializado |
| Persistência | Repositórios em memória | JPA e H2 |
| Injeção de dependências | Manual | Contêiner Spring |
| Comunicação HTTP | Não utilizada | MockMvc |
| Relógio | Fixo | Configuração da aplicação |
| Isolamento | Maior | Menor |

## 6. Análise dos resultados

A arquitetura hexagonal permite separar as regras de negócio e os casos de uso dos detalhes de infraestrutura por meio de portas e adaptadores.

A inversão de dependência possibilita substituir os adaptadores de persistência JPA por implementações em memória, sem modificar os serviços da aplicação.

Essa separação reduz o acoplamento com frameworks e mecanismos de persistência, aumentando a testabilidade e facilitando a manutenção.

Os testes unitários possuem menor custo de execução porque não precisam inicializar o contêiner de injeção de dependências, o Hibernate, o banco de dados ou a infraestrutura HTTP.

Os testes de integração continuam necessários para validar a comunicação entre as camadas, a persistência, as transações, a segurança e o controle de concorrência.

A comparação representa os tempos observados em execuções separadas no mesmo ambiente, não constituindo um benchmark estatístico. As suítes possuem quantidades de testes e coberturas diferentes.

## 7. Conclusão

A missão E3-6 demonstra, na prática, os benefícios da arquitetura hexagonal, da inversão de dependência, do baixo acoplamento e da separação de responsabilidades.

Os 17 novos testes foram executados com sucesso, sem falhas ou erros, validando os casos de uso da agenda e do prontuário sem depender de infraestrutura de banco de dados.

As duas estratégias de teste são complementares e contribuem para a qualidade, confiabilidade e evolução do sistema SaúdeCerta.

## 8. Validação final

Após a implementação dos testes dos casos de uso, foi executado o comando `mvn clean test` para verificar a integridade de todo o projeto.

- Total de testes: 95.
- Falhas: 0.
- Erros: 0.
- Testes ignorados: 0.
- Testes de arquitetura aprovados: 11.
- Testes de concorrência aprovados: 3.
- Tempo total de execução do Maven: 23,682 segundos.
- Resultado: BUILD SUCCESS.

A implementação preservou o funcionamento das funcionalidades existentes e respeitou as restrições arquiteturais do projeto.