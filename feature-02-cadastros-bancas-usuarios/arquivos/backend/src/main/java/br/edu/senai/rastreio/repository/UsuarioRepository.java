package br.edu.senai.rastreio.repository;

import br.edu.senai.rastreio.entity.Usuario;
import org.springframework.data.jpa.repository.*;

/** Acesso parametrizado aos registros; lotes usam bloqueio ao movimentar. */
public interface UsuarioRepository
    extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
  java.util.Optional<Usuario> findByEmail(String email);
}
