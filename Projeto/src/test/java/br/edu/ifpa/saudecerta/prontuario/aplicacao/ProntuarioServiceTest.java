package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.time.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.agenda.api.*;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.cadastro.api.*;
import br.edu.ifpa.saudecerta.compartilhado.erro.*;
import br.edu.ifpa.saudecerta.seguranca.api.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProntuarioServiceTest {

    private AtendimentoRepositorioEmMemoria repositorio;
    private ProntuarioService service;
    private AgendaFachada agenda;
    private List<EventoDeAuditoria> eventos;

    private final Clock relogio = Clock.fixed(
            Instant.parse("2026-11-24T09:00:00Z"),
            ZoneOffset.UTC
    );

    private final UsuarioLogado profissional =
            new UsuarioLogado(
                    "profissional", "Profissional A",
                    Perfil.PROFISSIONAL, 10L, null
            );

    private final UsuarioLogado outroProfissional =
            new UsuarioLogado(
                    "outro", "Profissional B",
                    Perfil.PROFISSIONAL, 20L, null
            );

    @BeforeEach
    void preparar() {
        repositorio = new AtendimentoRepositorioEmMemoria();
        eventos = new ArrayList<>();

        agenda = mock(AgendaFachada.class);

        CadastroFachada cadastro =
                mock(CadastroFachada.class);

        PrescricaoRepositorio prescricoes =
                mock(PrescricaoRepositorio.class);

        ConsultaInfo consulta = new ConsultaInfo(
                100L,
                LocalDateTime.now(relogio),
                30,
                "CONFIRMADA",
                1L,
                "Paciente A",
                null,
                10L,
                "Profissional A",
                "Clinica Geral",
                null,
                null
        );

        when(agenda.consulta(100L))
                .thenReturn(consulta);

        PacienteResumo paciente = new PacienteResumo(
                1L, "Paciente A", "00000000000",
                LocalDate.of(1990, 1, 1), null, true
        );

        ProfissionalResumo resumo =
                new ProfissionalResumo(
                        10L, "Profissional A",
                        "CRM-001", "Clinica Geral", true
                );

        when(cadastro.paciente(1L))
                .thenReturn(Optional.of(paciente));

        when(cadastro.profissional(10L))
                .thenReturn(Optional.of(resumo));

        when(cadastro.profissionais(anyCollection()))
                .thenReturn(Map.of(10L, resumo));

        when(prescricoes.doAtendimento(anyLong()))
                .thenReturn(List.of());

        service = new ProntuarioService(
                repositorio,
                agenda,
                cadastro,
                eventos::add,
                relogio,
                prescricoes
        );
    }

    private AtendimentoInfo iniciar() {
        return service.iniciar(100L, profissional);
    }

    private RegistroClinico registro() {
        return new RegistroClinico(
                "Queixa de teste",
                "Evolucao de teste",
                "Conduta de teste"
        );
    }

    @Test
    void iniciaAtendimentoEAudita() {
        AtendimentoInfo atendimento = iniciar();

        assertThat(atendimento.id()).isNotNull();

        assertThat(atendimento.status())
                .isEqualTo("ABERTO");

        assertThat(atendimento.inicio())
                .isEqualTo(LocalDateTime.now(relogio));

        assertThat(repositorio.buscarPorConsulta(100L))
                .isPresent();

        verify(agenda).iniciarAtendimento(100L);

        assertThat(eventos)
                .extracting(EventoDeAuditoria::acao)
                .containsExactly("ATENDIMENTO_INICIADO");
    }

    @Test
    void reabreAtendimentoSemDuplicar() {
        AtendimentoInfo primeiro = iniciar();
        AtendimentoInfo segundo = iniciar();

        assertThat(segundo.id())
                .isEqualTo(primeiro.id());

        assertThat(repositorio.doPaciente(1L))
                .hasSize(1);

        verify(agenda, times(1))
                .iniciarAtendimento(100L);

        assertThat(eventos)
                .extracting(EventoDeAuditoria::acao)
                .contains("PRONTUARIO_LIDO");
    }

    @Test
    void impedeInicioPorOutroProfissional() {
        assertThatThrownBy(() ->
                service.iniciar(100L, outroProfissional))
                .isInstanceOf(AcessoNegadoException.class);

        assertThat(repositorio.buscarPorConsulta(100L))
                .isEmpty();

        verify(agenda, never()).iniciarAtendimento(100L);

        assertThat(eventos).isEmpty();
    }

    @Test
    void registraEvolucaoClinica() {
        AtendimentoInfo atendimento = iniciar();

        AtendimentoInfo atualizado = service.registrar(
                atendimento.id(), registro(), profissional
        );

        assertThat(atualizado.evolucao())
                .isEqualTo("Evolucao de teste");

        assertThat(service.ler(
                atendimento.id(), profissional).evolucao())
                .isEqualTo("Evolucao de teste");

        assertThat(eventos)
                .extracting(EventoDeAuditoria::acao)
                .contains("PRONTUARIO_ALTERADO");
    }

    @Test
    void impedeAlteracaoPorOutroProfissional() {
        AtendimentoInfo atendimento = iniciar();

        assertThatThrownBy(() ->
                service.registrar(
                        atendimento.id(),
                        registro(),
                        outroProfissional))
                .isInstanceOf(AcessoNegadoException.class);

        assertThat(repositorio.buscarPorId(atendimento.id())
                .orElseThrow().getEvolucao()).isNull();
    }

    @Test
    void impedeFinalizacaoSemEvolucao() {
        AtendimentoInfo atendimento = iniciar();

        assertThatThrownBy(() ->
                service.finalizar(
                        atendimento.id(), profissional))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(agenda, never()).concluir(100L);

        assertThat(repositorio.buscarPorId(atendimento.id())
                .orElseThrow().aberto()).isTrue();
    }

    @Test
    void finalizaAtendimentoEAudita() {
        AtendimentoInfo atendimento = iniciar();

        service.registrar(
                atendimento.id(), registro(), profissional
        );

        AtendimentoInfo finalizado = service.finalizar(
                atendimento.id(), profissional
        );

        assertThat(finalizado.status())
                .isEqualTo("FINALIZADO");

        assertThat(finalizado.fim())
                .isEqualTo(LocalDateTime.now(relogio));

        verify(agenda).concluir(100L);

        assertThat(eventos)
                .extracting(EventoDeAuditoria::acao)
                .contains("ATENDIMENTO_FINALIZADO");
    }

    @Test
    void impedeAlteracaoAposFinalizacao() {
        AtendimentoInfo atendimento = iniciar();

        service.registrar(
                atendimento.id(), registro(), profissional
        );

        service.finalizar(atendimento.id(), profissional);

        assertThatThrownBy(() ->
                service.registrar(
                        atendimento.id(),
                        registro(),
                        profissional))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void consultaHistoricoDoPacienteEAudita() {
        AtendimentoInfo atendimento = iniciar();

        ProntuarioInfo historico =
                service.doPaciente(1L, profissional);

        assertThat(historico.paciente().id())
                .isEqualTo(1L);

        assertThat(historico.atendimentos())
                .extracting(AtendimentoInfo::id)
                .containsExactly(atendimento.id());

        assertThat(eventos)
                .extracting(EventoDeAuditoria::acao)
                .contains("PRONTUARIO_LIDO");
    }
}