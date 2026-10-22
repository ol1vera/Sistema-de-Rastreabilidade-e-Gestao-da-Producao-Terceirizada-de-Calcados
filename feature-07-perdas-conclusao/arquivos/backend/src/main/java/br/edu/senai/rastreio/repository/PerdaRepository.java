package br.edu.senai.rastreio.repository;

import br.edu.senai.rastreio.entity.Perda;
import org.springframework.data.jpa.repository.*;

/** Acesso parametrizado aos registros; lotes usam bloqueio ao movimentar. */
public interface PerdaRepository
    extends JpaRepository<Perda, Long>, JpaSpecificationExecutor<Perda> {
  java.util.List<Perda> findByLoteIdOrderByOcorridoEmAscIdAsc(Long id);
}
