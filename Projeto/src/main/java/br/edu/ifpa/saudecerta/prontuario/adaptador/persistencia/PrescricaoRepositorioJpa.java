package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.util.List;
import org.springframework.stereotype.Component;

import br.edu.ifpa.saudecerta.prontuario.aplicacao.PrescricaoRepositorio;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;

@Component
public class PrescricaoRepositorioJpa
        implements PrescricaoRepositorio {

    private final PrescricaoJpaRepository jpa;

    public PrescricaoRepositorioJpa(
            PrescricaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Prescricao salvar(Prescricao prescricao) {

        prescricao.validarParaEmissao();

        return jpa.save(PrescricaoJpa.de(prescricao))
                .paraDominio();
    }

    @Override
    public List<Prescricao> doAtendimento(Long atendimentoId) {

        return jpa.findByAtendimentoIdOrderByIdAsc(atendimentoId)
                .stream()
                .map(PrescricaoJpa::paraDominio)
                .toList();
    }
}