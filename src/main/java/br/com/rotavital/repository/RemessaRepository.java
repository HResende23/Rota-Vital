package br.com.rotavital.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import br.com.rotavital.model.Remessa;

public interface RemessaRepository extends JpaRepository<Remessa, Long> {
    boolean existsByRequisicaoId(Long requisicaoId);

    Optional<Remessa> findByRequisicaoId(Long requisicaoId);

    @Query("select r from Remessa r where r.requisicao.usuario.id = :usuarioId order by r.id desc")
    List<Remessa> listarDoUsuario(@Param("usuarioId") Long usuarioId);

    /** Serializa leituras e mudanças de situação concorrentes da mesma remessa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Remessa r where r.id = :id")
    Optional<Remessa> buscarParaAtualizacao(@Param("id") Long id);
}
