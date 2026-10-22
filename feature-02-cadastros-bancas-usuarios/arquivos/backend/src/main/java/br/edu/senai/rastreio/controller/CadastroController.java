package br.edu.senai.rastreio.controller;

import br.edu.senai.rastreio.dto.Entradas.*;
import br.edu.senai.rastreio.entity.*;
import br.edu.senai.rastreio.repository.*;
import br.edu.senai.rastreio.security.Acesso;
import br.edu.senai.rastreio.service.CadastroService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

/** Catálogos paginados; operadores de banca só consultam sua própria banca. */
@RestController
@RequestMapping("/api")
public class CadastroController {
  private final CadastroService s;
  private final Acesso a;
  // Repositórios injetados direto: CadastroService é um proxy (@Transactional) e seus
  // campos públicos ficam nulos quando lidos de fora do proxy.
  private final BancaRepository bancas;
  private final ServicoRepository servicos;
  private final UsuarioRepository usuarios;

  public CadastroController(
      CadastroService s,
      Acesso a,
      BancaRepository bancas,
      ServicoRepository servicos,
      UsuarioRepository usuarios) {
    this.s = s;
    this.a = a;
    this.bancas = bancas;
    this.servicos = servicos;
    this.usuarios = usuarios;
  }

  private PageRequest pagina(int p, int tamanho) {
    return PageRequest.of(Math.max(0, p), Math.min(100, Math.max(1, tamanho)), Sort.by("nome"));
  }

  @GetMapping("/bancas")
  public Object bancas(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String busca) {
    var u = a.atual();
    Specification<Banca> f =
        (r, q, c) -> c.like(c.lower(r.get("nome")), "%" + busca.toLowerCase() + "%");
    if (u.perfil == Perfil.OPERADOR_BANCA) f = f.and((r, q, c) -> c.equal(r.get("id"), u.banca.id));
    return br.edu.senai.rastreio.dto.Pagina.de(bancas.findAll(f, pagina(page, size)));
  }

  @GetMapping("/servicos")
  public Object servicos(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String busca) {
    a.exigir(Perfil.ADMIN, Perfil.GESTOR);
    return br.edu.senai.rastreio.dto.Pagina.de(
        servicos.findAll(
            (r, q, c) -> c.like(c.lower(r.get("nome")), "%" + busca.toLowerCase() + "%"),
            pagina(page, size)));
  }

  @GetMapping("/usuarios")
  public Object usuarios(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String busca) {
    a.exigir(Perfil.ADMIN);
    return br.edu.senai.rastreio.dto.Pagina.de(
        usuarios.findAll(
            (r, q, c) -> c.like(c.lower(r.get("nome")), "%" + busca.toLowerCase() + "%"),
            pagina(page, size)));
  }

  @PostMapping("/bancas")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object banca(@Valid @RequestBody BancaInput d) {
    return s.banca(null, d);
  }

  @PutMapping("/bancas/{id}")
  public Object banca(@PathVariable Long id, @Valid @RequestBody BancaInput d) {
    return s.banca(id, d);
  }

  @PostMapping("/servicos")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object servico(@Valid @RequestBody ServicoInput d) {
    return s.servico(null, d);
  }

  @PutMapping("/servicos/{id}")
  public Object servico(@PathVariable Long id, @Valid @RequestBody ServicoInput d) {
    return s.servico(id, d);
  }

  @PostMapping("/usuarios")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object usuario(@Valid @RequestBody UsuarioInput d) {
    return s.usuario(null, d);
  }

  @PutMapping("/usuarios/{id}")
  public Object usuario(@PathVariable Long id, @Valid @RequestBody UsuarioInput d) {
    return s.usuario(id, d);
  }
}
