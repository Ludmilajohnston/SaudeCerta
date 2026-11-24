package br.edu.ifpa.saudecerta.agenda.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

/** Regras da E3-4 (portal do paciente) na Consulta. */
class ConsultaPortalTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 24, 10, 0);

    private static Consulta consulta(LocalDateTime inicio, StatusConsulta status) {
        return Consulta.reconstituir(10L, 1L, 2L, inicio, 30, status, null, null);
    }

    @Test
    void pertenceSomenteAoProprioPaciente() {
        Consulta c = consulta(AGORA.plusDays(1), StatusConsulta.AGENDADA);
        assertThat(c.pertenceA(1L)).isTrue();
        assertThat(c.pertenceA(2L)).isFalse();
        assertThat(c.pertenceA(null)).isFalse();
    }

    @Test
    void pacienteCancelaConsultaFutura() {
        Consulta c = consulta(AGORA.plusDays(1), StatusConsulta.AGENDADA);
        c.cancelarPeloPaciente("Viagem", AGORA);
        assertThat(c.getStatus()).isEqualTo(StatusConsulta.CANCELADA);
        assertThat(c.getMotivoCancelamento()).isEqualTo("Viagem");
    }

    @Test
    void pacienteNaoCancelaConsultaQueJaPassou() {
        Consulta c = consulta(AGORA.minusHours(1), StatusConsulta.AGENDADA);
        assertThatThrownBy(() -> c.cancelarPeloPaciente("Viagem", AGORA))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("futuras");
    }

    @Test
    void pacienteNaoCancelaSemMotivo() {
        Consulta c = consulta(AGORA.plusDays(1), StatusConsulta.AGENDADA);
        assertThatThrownBy(() -> c.cancelarPeloPaciente(" ", AGORA))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void indicaQuandoOPortalPodeOferecerCancelamento() {
        assertThat(consulta(AGORA.plusDays(1), StatusConsulta.AGENDADA).podeSerCanceladaPeloPaciente(AGORA)).isTrue();
        assertThat(consulta(AGORA.plusDays(1), StatusConsulta.CANCELADA).podeSerCanceladaPeloPaciente(AGORA)).isFalse();
        assertThat(consulta(AGORA.minusDays(1), StatusConsulta.AGENDADA).podeSerCanceladaPeloPaciente(AGORA)).isFalse();
    }
}