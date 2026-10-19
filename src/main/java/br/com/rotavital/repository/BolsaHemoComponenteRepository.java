package br.com.rotavital.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import br.com.rotavital.model.*;

public interface BolsaHemoComponenteRepository extends JpaRepository<BolsaHemoComponente, String> {
    List<BolsaHemoComponente> findByComponenteAndStatusAndValidadeGreaterThanEqualOrderByValidadeAscCodigoAsc(
        TipoComponente componente, StatusBolsa status, LocalDate validade);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BolsaHemoComponente b where b.codigo in :codigos order by b.codigo")
    List<BolsaHemoComponente> buscarParaReserva(@Param("codigos") List<String> codigos);
}
