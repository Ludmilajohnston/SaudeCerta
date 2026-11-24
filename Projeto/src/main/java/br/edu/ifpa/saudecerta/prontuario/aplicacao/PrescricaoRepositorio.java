package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.util.List;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;

public interface PrescricaoRepositorio {

    Prescricao salvar(Prescricao prescricao);

    List<Prescricao> doAtendimento(Long atendimentoId);
}