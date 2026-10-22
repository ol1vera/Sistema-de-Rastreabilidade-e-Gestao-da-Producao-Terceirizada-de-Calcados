package br.edu.senai.rastreio.repository;

import br.edu.senai.rastreio.entity.Banca;
import org.springframework.data.jpa.repository.*;

/** Acesso parametrizado aos registros; lotes usam bloqueio ao movimentar. */
public interface BancaRepository
    extends JpaRepository<Banca, Long>, JpaSpecificationExecutor<Banca> {}
