package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;

public interface ConsultaRepositorio {

    // E3-5: protege as operacoes de escrita da agenda.
    void bloquearAgenda();

    Consulta salvar(Consulta consulta);

    Optional<Consulta> buscarPorId(Long id);

    List<Consulta> doDia(LocalDate data);

    List<Consulta> doProfissionalNoDia(
            Long profissionalId, LocalDate data);

    List<Consulta> doPacienteNoDia(
            Long pacienteId, LocalDate data);

    List<Consulta> doPeriodo(LocalDate de, LocalDate ate);

    List<Consulta> doPaciente(Long pacienteId);
}