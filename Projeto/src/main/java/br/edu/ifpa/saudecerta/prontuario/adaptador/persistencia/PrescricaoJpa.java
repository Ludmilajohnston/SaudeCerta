package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;
import jakarta.persistence.*;

@Entity
@Table(name = "prescricao")
public class PrescricaoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long atendimentoId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "prescricao_item",
        joinColumns = @JoinColumn(name = "prescricao_id")
    )
    @OrderColumn(name = "ordem")
    private List<ItemJpa> itens = new ArrayList<>();

    protected PrescricaoJpa() {
    }

    @Embeddable
    public static class ItemJpa {

        @Column(nullable = false, length = 255)
        private String medicamento;

        @Column(nullable = false, length = 255)
        private String dose;

        @Column(nullable = false, length = 1000)
        private String posologia;

        protected ItemJpa() {
        }

        ItemJpa(Prescricao.Item item) {
            this.medicamento = item.medicamento();
            this.dose = item.dose();
            this.posologia = item.posologia();
        }

        Prescricao.Item paraDominio() {
            return new Prescricao.Item(
                medicamento, dose, posologia);
        }
    }

    static PrescricaoJpa de(Prescricao prescricao) {

        PrescricaoJpa jpa = new PrescricaoJpa();

        jpa.atendimentoId = prescricao.getAtendimentoId();

        for (Prescricao.Item item : prescricao.getItens()) {
            jpa.itens.add(new ItemJpa(item));
        }

        return jpa;
    }

    Prescricao paraDominio() {

        Prescricao prescricao = new Prescricao(atendimentoId);

        for (ItemJpa item : itens) {
            prescricao.adicionar(item.paraDominio(), null);
        }

        return prescricao;
    }
}