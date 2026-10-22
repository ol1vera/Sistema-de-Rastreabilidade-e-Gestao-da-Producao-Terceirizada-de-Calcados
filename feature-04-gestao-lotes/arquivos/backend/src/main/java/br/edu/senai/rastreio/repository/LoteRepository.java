package br.edu.senai.rastreio.repository;

import br.edu.senai.rastreio.entity.Lote;
import org.springframework.data.jpa.repository.*;

/** Acesso parametrizado aos registros; lotes usam bloqueio ao movimentar. */
public interface LoteRepository extends JpaRepository<Lote, Long>, JpaSpecificationExecutor<Lote> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select l from Lote l where l.codigo = :codigo")
  java.util.Optional<Lote> bloquear(
      @org.springframework.data.repository.query.Param("codigo") String codigo);

  java.util.Optional<Lote> findByCodigo(String codigo);
}
