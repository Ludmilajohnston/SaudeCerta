package br.edu.ifpa.saudecerta.painel.aplicacao;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.edu.ifpa.saudecerta.agenda.api.AgendaFachada;
import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.painel.dominio.PeriodoDoRelatorio;
import br.edu.ifpa.saudecerta.seguranca.api.Perfil;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;

/**
 * Resumo do dia. Nao tem dados proprios: so compoe o que agenda e cadastro
 * publicam em suas fachadas (um modulo "de leitura").
 */
@Service
public class PainelService {

    private final AgendaFachada agenda;
    private final CadastroFachada cadastro;
    private final Clock relogio;

    public PainelService(AgendaFachada agenda, CadastroFachada cadastro, Clock relogio) {
        this.agenda = agenda;
        this.cadastro = cadastro;
        this.relogio = relogio;
    }

    /** Profissionais veem apenas a propria agenda; recepcao e administracao veem a clinica toda. */
    public PainelDoDia hoje(UsuarioLogado quem) {
        LocalDate hoje = LocalDate.now(relogio);
        Long filtro = quem.tem(Perfil.PROFISSIONAL) ? quem.profissionalId() : null;
        List<ConsultaInfo> consultas = agenda.consultasDoDia(hoje, filtro);
        return new PainelDoDia(hoje, consultas.size(), contar(consultas, "AGENDADA"),
                contar(consultas, "CONFIRMADA"), contar(consultas, "EM_ATENDIMENTO"),
                contar(consultas, "REALIZADA"), contar(consultas, "CANCELADA"),
                cadastro.totalDePacientesAtivos(), cadastro.profissionaisAtivos().size(), consultas);
    }

    /**
     * E3-3: totais de consultas por situacao, profissional e especialidade no periodo.
     * Os dados vem pela AgendaFachada: o painel nao acessa a persistencia da agenda.
     * Agregacao feita em memoria (ver ADR do relatorio).
     */
    public RelatorioDeConsultas relatorioDeConsultas(LocalDate de, LocalDate ate) {
        PeriodoDoRelatorio periodo = new PeriodoDoRelatorio(de, ate);
        List<ConsultaInfo> consultas = agenda.consultasDoPeriodo(periodo.de(), periodo.ate());
        return new RelatorioDeConsultas(periodo.de(), periodo.ate(), consultas.size(),
                agrupar(consultas, ConsultaInfo::status),
                agrupar(consultas, ConsultaInfo::profissional),
                agrupar(consultas, c -> c.especialidade() == null ? "(sem especialidade)" : c.especialidade()));
    }

    private static Map<String, Long> agrupar(List<ConsultaInfo> consultas, Function<ConsultaInfo, String> chave) {
        return consultas.stream().collect(Collectors.groupingBy(chave, TreeMap::new, Collectors.counting()));
    }

    private static int contar(List<ConsultaInfo> consultas, String status) {
        return (int) consultas.stream().filter(c -> c.status().equals(status)).count();
    }
}