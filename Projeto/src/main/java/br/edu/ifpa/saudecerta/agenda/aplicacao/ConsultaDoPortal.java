package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.LocalDateTime;

/**
 * O que o paciente ve de uma consulta no portal (E3-4).
 * De proposito NAO tem dados clinicos nem observacoes internas:
 * nada de alertas, evolucao, conduta ou observacao da recepcao.
 */
public record ConsultaDoPortal(Long id, LocalDateTime inicio, int duracaoMinutos, String status,
        String profissional, String especialidade, String motivoCancelamento, boolean podeCancelar) {
}