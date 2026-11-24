package br.edu.ifpa.saudecerta.agenda.api;

import java.time.LocalDate;
import java.util.List;

/** O que os outros modulos (prontuario, painel) podem pedir a agenda. */
public interface AgendaFachada {

    ConsultaInfo consulta(Long id);

    List<ConsultaInfo> consultasDoDia(LocalDate data, Long profissionalId);

    /** Consultas entre as duas datas (inclusive), ja com nomes e especialidade. Usado pelos relatorios. */
    List<ConsultaInfo> consultasDoPeriodo(LocalDate de, LocalDate ate);

    /** CONFIRMADA -> EM_ATENDIMENTO. Chamado pelo prontuario ao abrir o atendimento. */
    void iniciarAtendimento(Long consultaId);

    /** EM_ATENDIMENTO -> REALIZADA. Chamado pelo prontuario ao finalizar o atendimento. */
    void concluir(Long consultaId);
}