package br.edu.ifpa.saudecerta.agenda.adaptador.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bloqueio_agenda")
public class BloqueioAgendaJpa {

    @Id
    private Long id;

    protected BloqueioAgendaJpa() {
    }

    public BloqueioAgendaJpa(Long id) {
        this.id = id;
    }
}