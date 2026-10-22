package br.edu.senai.rastreio.security;

import br.edu.senai.rastreio.entity.*;
import br.edu.senai.rastreio.exception.RegraException;
import br.edu.senai.rastreio.repository.UsuarioRepository;
import java.util.Arrays;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reconsulta o usuário: desativação e mudança de perfil valem mesmo com token emitido. */
@Component
public class Acesso {
  private final UsuarioRepository usuarios;

  public Acesso(UsuarioRepository usuarios) {
    this.usuarios = usuarios;
  }

  public Usuario atual() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    try {
      var u = usuarios.findById(Long.parseLong(a.getName())).orElseThrow();
      if (!u.ativo) throw new RuntimeException();
      return u;
    } catch (Exception e) {
      throw new RegraException(401, "Sessão inválida. Entre novamente.");
    }
  }

  public Usuario exigir(Perfil... perfis) {
    var u = atual();
    if (!Arrays.asList(perfis).contains(u.perfil))
      throw new RegraException(403, "Seu perfil não permite esta operação.");
    return u;
  }

  public void lote(Usuario u, Lote lote) {
    if (u.perfil == Perfil.OPERADOR_BANCA && (u.banca == null || !u.banca.id.equals(lote.banca.id)))
      throw new RegraException(404, "Lote não encontrado ou indisponível para seu acesso.");
  }
}
