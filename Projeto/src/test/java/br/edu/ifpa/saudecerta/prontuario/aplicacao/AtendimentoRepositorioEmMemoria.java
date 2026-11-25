package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.ifpa.saudecerta.prontuario.dominio.Atendimento;

public class AtendimentoRepositorioEmMemoria
        implements AtendimentoRepositorio {

    private final Map<Long, Atendimento> dados =
            new LinkedHashMap<>();

    private long proximoId = 1L;

    private Atendimento copiar(Atendimento a, Long id) {
        return Atendimento.reconstituir(
                id,
                a.getConsultaId(),
                a.getPacienteId(),
                a.getProfissionalId(),
                a.getInicio(),
                a.getFim(),
                a.getQueixaPrincipal(),
                a.getEvolucao(),
                a.getConduta(),
                a.getStatus()
        );
    }

    @Override
    public Atendimento salvar(Atendimento atendimento) {
        Long id = atendimento.getId() == null
                ? proximoId++
                : atendimento.getId();

        Atendimento salvo = copiar(atendimento, id);

        dados.put(id, salvo);

        return copiar(salvo, id);
    }

    @Override
    public Optional<Atendimento> buscarPorId(Long id) {
        return Optional.ofNullable(dados.get(id))
                .map(a -> copiar(a, a.getId()));
    }

    @Override
    public Optional<Atendimento> buscarPorConsulta(
            Long consultaId) {

        return dados.values().stream()
                .filter(a -> consultaId.equals(
                        a.getConsultaId()))
                .findFirst()
                .map(a -> copiar(a, a.getId()));
    }

    @Override
    public List<Atendimento> doPaciente(Long pacienteId) {
        return dados.values().stream()
                .filter(a -> pacienteId.equals(
                        a.getPacienteId()))
                .sorted(Comparator.comparing(
                        Atendimento::getInicio).reversed())
                .map(a -> copiar(a, a.getId()))
                .toList();
    }
}