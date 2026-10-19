package br.com.rotavital.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import br.com.rotavital.model.Requisicao;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Requisicao r where r.id = :id")
    Optional<Requisicao> buscarParaReserva(@Param("id") Long id);
}
