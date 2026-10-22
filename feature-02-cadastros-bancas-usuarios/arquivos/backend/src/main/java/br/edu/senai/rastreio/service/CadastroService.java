package br.edu.senai.rastreio.service;

import br.edu.senai.rastreio.dto.Entradas.*;
import br.edu.senai.rastreio.entity.*;
import br.edu.senai.rastreio.exception.RegraException;
import br.edu.senai.rastreio.repository.*;
import br.edu.senai.rastreio.security.Acesso;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastros administrativos com desativação lógica e vínculos preservados. */
@Service
public class CadastroService {
  public final BancaRepository bancas;
  public final ServicoRepository servicos;
  public final UsuarioRepository usuarios;
  private final Acesso acesso;
  private final PasswordEncoder senhas;

  public CadastroService(
      BancaRepository b, ServicoRepository s, UsuarioRepository u, Acesso a, PasswordEncoder p) {
    bancas = b;
    servicos = s;
    usuarios = u;
    acesso = a;
    senhas = p;
  }

  @Transactional
  public Banca banca(Long id, BancaInput d) {
    acesso.exigir(Perfil.ADMIN);
    String doc = d.documento().replaceAll("[^0-9]", "");
    if (!Documentos.valido(doc)) throw new RegraException(400, "CPF/CNPJ inválido.");
    if (!d.telefone().replaceAll("[^0-9]", "").matches("[0-9]{10,13}"))
      throw new RegraException(400, "Informe telefone com DDD.");
    var b =
        id == null
            ? new Banca()
            : bancas
                .findById(id)
                .orElseThrow(() -> new RegraException(404, "Banca não encontrada."));
    b.nome = d.nome().trim();
    b.documento = doc;
    b.responsavel = d.responsavel().trim();
    b.telefone = d.telefone();
    b.ativo = d.ativo();
    return bancas.save(b);
  }

  @Transactional
  public Servico servico(Long id, ServicoInput d) {
    acesso.exigir(Perfil.ADMIN);
    var s =
        id == null
            ? new Servico()
            : servicos
                .findById(id)
                .orElseThrow(() -> new RegraException(404, "Serviço não encontrado."));
    s.nome = d.nome().trim();
    s.descricao = d.descricao();
    s.valorPar = d.valorPar();
    s.ativo = d.ativo();
    return servicos.save(s);
  }

  @Transactional
  public Usuario usuario(Long id, UsuarioInput d) {
    var atual = acesso.exigir(Perfil.ADMIN);
    if (id != null && id.equals(atual.id) && (!d.ativo() || d.perfil() != Perfil.ADMIN))
      throw new RegraException(409, "Você não pode remover seu próprio acesso administrativo.");
    var u =
        id == null
            ? new Usuario()
            : usuarios
                .findById(id)
                .orElseThrow(() -> new RegraException(404, "Usuário não encontrado."));
    if (id == null && (d.senha() == null || d.senha().isBlank()))
      throw new RegraException(400, "Informe a senha inicial.");
    if (d.senha() != null && !d.senha().isBlank()) {
      if (d.senha().length() < 10
          || d.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        throw new RegraException(
            400, "Senha deve ter no mínimo 10 caracteres e no máximo 72 bytes.");
      u.senha = senhas.encode(d.senha());
    }
    u.banca = null;
    if (d.perfil() == Perfil.OPERADOR_BANCA) {
      if (d.bancaId() == null)
        throw new RegraException(400, "Operador de banca precisa de uma banca.");
      u.banca =
          bancas
              .findById(d.bancaId())
              .filter(b -> b.ativo)
              .orElseThrow(() -> new RegraException(400, "Banca inexistente ou inativa."));
    }
    u.nome = d.nome().trim();
    u.email = d.email().trim().toLowerCase();
    u.perfil = d.perfil();
    u.ativo = d.ativo();
    return usuarios.save(u);
  }
}
