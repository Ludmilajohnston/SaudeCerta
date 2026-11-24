package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescricaoJpaRepository
        extends JpaRepository<PrescricaoJpa, Long> {

    List<PrescricaoJpa> findByAtendimentoIdOrderByIdAsc(
            Long atendimentoId);
}