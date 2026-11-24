package br.edu.ifpa.saudecerta.painel.dominio;

import java.time.LocalDate;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

/**
 * Periodo de um relatorio. Regras (E3-3):
 * - inicio e fim sao obrigatorios;
 * - o fim nao pode ser antes do inicio;
 * - o periodo nao pode passar de 1 ano.
 */
public record PeriodoDoRelatorio(LocalDate de, LocalDate ate) {

    public PeriodoDoRelatorio {
        if (de == null || ate == null) {
            throw new RegraDeNegocioException("Informe a data inicial e a data final do período.");
        }
        if (ate.isBefore(de)) {
            throw new RegraDeNegocioException("A data final não pode ser anterior à data inicial.");
        }
        if (ate.isAfter(de.plusYears(1))) {
            throw new RegraDeNegocioException("O período do relatório não pode ser maior que 1 ano.");
        }
    }
}