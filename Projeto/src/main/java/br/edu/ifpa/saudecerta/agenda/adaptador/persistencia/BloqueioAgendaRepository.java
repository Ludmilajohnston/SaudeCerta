package br.edu.ifpa.saudecerta.agenda.adaptador.persistencia;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface BloqueioAgendaRepository
        extends JpaRepository<BloqueioAgendaJpa, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BloqueioAgendaJpa b where b.id = 1")
    Optional<BloqueioAgendaJpa> bloquear();
}