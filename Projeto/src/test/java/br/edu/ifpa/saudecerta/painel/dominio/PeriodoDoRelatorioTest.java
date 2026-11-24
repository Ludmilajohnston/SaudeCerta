package br.edu.ifpa.saudecerta.painel.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

class PeriodoDoRelatorioTest {

    @Test
    void aceitaPeriodoValido() {
        assertThatCode(() -> new PeriodoDoRelatorio(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .doesNotThrowAnyException();
    }

    @Test
    void aceitaUmUnicoDia() {
        assertThatCode(() -> new PeriodoDoRelatorio(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10)))
                .doesNotThrowAnyException();
    }

    @Test
    void aceitaExatamenteUmAno() {
        assertThatCode(() -> new PeriodoDoRelatorio(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaFimAntesDoInicio() {
        assertThatThrownBy(() -> new PeriodoDoRelatorio(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void recusaPeriodoMaiorQueUmAno() {
        assertThatThrownBy(() -> new PeriodoDoRelatorio(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("1 ano");
    }

    @Test
    void recusaDatasEmBranco() {
        assertThatThrownBy(() -> new PeriodoDoRelatorio(null, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}