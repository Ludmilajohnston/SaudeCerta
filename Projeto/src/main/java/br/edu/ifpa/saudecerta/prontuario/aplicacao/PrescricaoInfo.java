package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.util.List;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;

public record PrescricaoInfo(
        Long atendimentoId,
        List<Prescricao.Item> itens) {

    public static PrescricaoInfo de(Prescricao prescricao) {
        return new PrescricaoInfo(
                prescricao.getAtendimentoId(),
                prescricao.getItens());
    }
}