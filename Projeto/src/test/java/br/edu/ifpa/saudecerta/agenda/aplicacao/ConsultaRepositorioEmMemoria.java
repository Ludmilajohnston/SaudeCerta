package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;

public class ConsultaRepositorioEmMemoria
        implements ConsultaRepositorio {

    private final Map<Long, Consulta> dados =
            new LinkedHashMap<>();

    private long proximoId = 1L;

    @Override
    public void bloquearAgenda() {
        // Os testes unitarios sao sequenciais.
        // Nao existe transacao de banco neste adaptador.
    }

    private Consulta copiar(Consulta c, Long id) {
        return Consulta.reconstituir(
                id,
                c.getPacienteId(),
                c.getProfissionalId(),
                c.getInicio(),
                c.getDuracaoMinutos(),
                c.getStatus(),
                c.getObservacao(),
                c.getMotivoCancelamento()
        );
    }

    @Override
    public Consulta salvar(Consulta consulta) {
        Long id = consulta.getId() == null
                ? proximoId++
                : consulta.getId();

        Consulta salva = copiar(consulta, id);

        dados.put(id, salva);

        return copiar(salva, id);
    }

    @Override
    public Optional<Consulta> buscarPorId(Long id) {
        return Optional.ofNullable(dados.get(id))
                .map(c -> copiar(c, c.getId()));
    }

    @Override
    public List<Consulta> doDia(LocalDate data) {
        return dados.values().stream()
                .filter(c -> c.getInicio()
                        .toLocalDate().equals(data))
                .map(c -> copiar(c, c.getId()))
                .toList();
    }

    @Override
    public List<Consulta> doProfissionalNoDia(
            Long profissionalId, LocalDate data) {

        return doDia(data).stream()
                .filter(c -> profissionalId.equals(
                        c.getProfissionalId()))
                .toList();
    }

    @Override
    public List<Consulta> doPacienteNoDia(
            Long pacienteId, LocalDate data) {

        return doDia(data).stream()
                .filter(c -> pacienteId.equals(
                        c.getPacienteId()))
                .toList();
    }

    @Override
    public List<Consulta> doPeriodo(
            LocalDate de, LocalDate ate) {

        return dados.values().stream()
                .filter(c -> {
                    LocalDate dia =
                            c.getInicio().toLocalDate();

                    return !dia.isBefore(de)
                            && !dia.isAfter(ate);
                })
                .map(c -> copiar(c, c.getId()))
                .toList();
    }

    @Override
    public List<Consulta> doPaciente(Long pacienteId) {
        return dados.values().stream()
                .filter(c -> pacienteId.equals(
                        c.getPacienteId()))
                .map(c -> copiar(c, c.getId()))
                .toList();
    }
}