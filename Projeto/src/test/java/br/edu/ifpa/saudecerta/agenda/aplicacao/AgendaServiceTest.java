package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.cadastro.api.*;
import br.edu.ifpa.saudecerta.compartilhado.erro.*;
import br.edu.ifpa.saudecerta.seguranca.api.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgendaServiceTest {

    private ConsultaRepositorioEmMemoria repositorio;
    private AgendaService service;
    private List<EventoDeAuditoria> eventos;

    private final Clock relogio = Clock.fixed(
            Instant.parse("2026-11-23T08:00:00Z"),
            ZoneOffset.UTC
    );

    private final LocalDateTime horario =
            LocalDateTime.of(2026, 11, 24, 9, 0);

    private final UsuarioLogado recepcao =
            new UsuarioLogado(
                    "recepcao", "Recepcao",
                    Perfil.RECEPCAO, null, null
            );

    @BeforeEach
    void preparar() {
        repositorio = new ConsultaRepositorioEmMemoria();
        eventos = new ArrayList<>();

        CadastroFachada cadastro =
                mock(CadastroFachada.class);

        PacienteResumo paciente1 = new PacienteResumo(
                1L, "Paciente A", "00000000000",
                LocalDate.of(1990, 1, 1), null, true
        );

        PacienteResumo paciente2 = new PacienteResumo(
                2L, "Paciente B", "11111111111",
                LocalDate.of(1995, 1, 1), null, true
        );

        ProfissionalResumo profissional1 =
                new ProfissionalResumo(
                        10L, "Profissional A",
                        "CRM-001", "Clinica Geral", true
                );

        ProfissionalResumo profissional2 =
                new ProfissionalResumo(
                        20L, "Profissional B",
                        "CRM-002", "Clinica Geral", true
                );

        when(cadastro.paciente(1L))
                .thenReturn(Optional.of(paciente1));

        when(cadastro.paciente(2L))
                .thenReturn(Optional.of(paciente2));

        when(cadastro.profissional(10L))
                .thenReturn(Optional.of(profissional1));

        when(cadastro.profissional(20L))
                .thenReturn(Optional.of(profissional2));

        when(cadastro.pacientes(anyCollection()))
                .thenReturn(Map.of(
                        1L, paciente1,
                        2L, paciente2
                ));

        when(cadastro.profissionais(anyCollection()))
                .thenReturn(Map.of(
                        10L, profissional1,
                        20L, profissional2
                ));

        service = new AgendaService(
                repositorio,
                cadastro,
                eventos::add,
                relogio
        );
    }

    private ConsultaInfo agendar(
            Long paciente,
            Long profissional,
            LocalDateTime inicio) {

        return service.agendar(
                new NovaConsulta(
                        paciente, profissional,
                        inicio, "Consulta de teste"
                ),
                recepcao
        );
    }

    @Test
    void agendaConsultaEAudita() {
        ConsultaInfo consulta =
                agendar(1L, 10L, horario);

        assertThat(consulta.id()).isNotNull();

        assertThat(consulta.status())
                .isEqualTo("AGENDADA");

        assertThat(consulta.inicio())
                .isEqualTo(horario);

        assertThat(repositorio.buscarPorId(consulta.id()))
                .isPresent();

        assertThat(eventos)
                .extracting(EventoDeAuditoria::acao)
                .containsExactly("CONSULTA_AGENDADA");
    }

    @Test
    void recusaConflitoDoProfissional() {
        agendar(1L, 10L, horario);

        assertThatThrownBy(() ->
                agendar(2L, 10L, horario))
                .isInstanceOf(ConflitoException.class);

        assertThat(repositorio.doDia(
                horario.toLocalDate())).hasSize(1);

        assertThat(eventos).hasSize(1);
    }

    @Test
    void recusaConflitoDoPaciente() {
        agendar(1L, 10L, horario);

        assertThatThrownBy(() ->
                agendar(1L, 20L, horario))
                .isInstanceOf(ConflitoException.class);

        assertThat(repositorio.doDia(
                horario.toLocalDate())).hasSize(1);
    }

    @Test
    void recusaHorarioPassado() {
        LocalDateTime passado =
                LocalDateTime.now(relogio).minusHours(1);

        assertThatThrownBy(() ->
                agendar(1L, 10L, passado))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThat(eventos).isEmpty();
    }

    @Test
    void cancelamentoLiberaHorario() {
        ConsultaInfo primeira =
                agendar(1L, 10L, horario);

        service.cancelar(
                primeira.id(), "Desistencia", recepcao
        );

        ConsultaInfo segunda =
                agendar(2L, 10L, horario);

        assertThat(segunda.status())
                .isEqualTo("AGENDADA");

        assertThat(service.consulta(primeira.id()).status())
                .isEqualTo("CANCELADA");
    }

    @Test
    void remarcaPreservandoIdentidade() {
        ConsultaInfo original =
                agendar(1L, 10L, horario);

        LocalDateTime novoHorario =
                horario.plusHours(1);

        ConsultaInfo remarcada = service.remarcar(
                original.id(), novoHorario, recepcao
        );

        assertThat(remarcada.id())
                .isEqualTo(original.id());

        assertThat(remarcada.inicio())
                .isEqualTo(novoHorario);

        assertThat(eventos)
                .extracting(EventoDeAuditoria::acao)
                .contains("CONSULTA_REMARCADA");
    }

    @Test
    void remarcacaoComConflitoPreservaHorarioOriginal() {
        agendar(1L, 10L, horario);

        ConsultaInfo segunda =
                agendar(2L, 10L, horario.plusHours(1));

        assertThatThrownBy(() ->
                service.remarcar(
                        segunda.id(), horario, recepcao))
                .isInstanceOf(ConflitoException.class);

        assertThat(service.consulta(segunda.id()).inicio())
                .isEqualTo(horario.plusHours(1));

        assertThat(eventos).hasSize(2);
    }

    @Test
    void consultaOcupadaNaoApareceNosHorariosLivres() {
        agendar(1L, 10L, horario);

        assertThat(service.horariosLivres(
                10L, horario.toLocalDate()))
                .doesNotContain(horario.toLocalTime())
                .contains(horario.plusMinutes(30).toLocalTime());
    }
}