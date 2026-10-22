package br.edu.senai.rastreio.service;

import br.edu.senai.rastreio.dto.Entradas.*;
import br.edu.senai.rastreio.entity.*;
import br.edu.senai.rastreio.exception.RegraException;
import br.edu.senai.rastreio.repository.*;
import br.edu.senai.rastreio.security.Acesso;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras centrais. Cada mutação bloqueia o lote e grava o histórico na mesma transação. */
@Service
public class LoteService {
  private final LoteRepository lotes;
  private final BancaRepository bancas;
  private final ServicoRepository servicos;
  private final MovimentacaoRepository movimentos;
  private final PerdaRepository perdas;
  private final Acesso acesso;
  public static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

  public LoteService(
      LoteRepository l,
      BancaRepository b,
      ServicoRepository s,
      MovimentacaoRepository m,
      PerdaRepository p,
      Acesso a) {
    lotes = l;
    bancas = b;
    servicos = s;
    movimentos = m;
    perdas = p;
    acesso = a;
  }

  public Specification<Lote> filtro(
      String busca, Status status, Long banca, LocalDate inicio, LocalDate fim) {
    var u = acesso.atual();
    if (inicio != null && fim != null && fim.isBefore(inicio))
      throw new RegraException(400, "Período inválido.");
    return (r, q, c) -> {
      var ps = new ArrayList<jakarta.persistence.criteria.Predicate>();
      if (u.perfil == Perfil.OPERADOR_BANCA) ps.add(c.equal(r.get("banca").get("id"), u.banca.id));
      if (busca != null && !busca.isBlank())
        ps.add(c.like(c.upper(r.get("codigo")), "%" + busca.toUpperCase() + "%"));
      if (status != null) ps.add(c.equal(r.get("status"), status));
      if (banca != null) ps.add(c.equal(r.get("banca").get("id"), banca));
      if (inicio != null)
        ps.add(c.greaterThanOrEqualTo(r.get("criadoEm"), inicio.atStartOfDay(BRASIL).toInstant()));
      if (fim != null)
        ps.add(c.lessThan(r.get("criadoEm"), fim.plusDays(1).atStartOfDay(BRASIL).toInstant()));
      return c.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };
  }

  @Transactional(readOnly = true)
  public Object listar(
      String busca,
      Status status,
      Long banca,
      LocalDate inicio,
      LocalDate fim,
      int page,
      int size) {
    return br.edu.senai.rastreio.dto.Pagina.de(
        lotes
            .findAll(
                filtro(busca, status, banca, inicio, fim),
                PageRequest.of(
                    Math.max(0, page),
                    Math.min(100, Math.max(1, size)),
                    Sort.by(Sort.Direction.DESC, "criadoEm")))
            .map(this::resumo));
  }

  private Lote consultar(String codigo, boolean bloquear) {
    var u = acesso.atual();
    var l =
        (bloquear ? lotes.bloquear(codigo) : lotes.findByCodigo(codigo))
            .orElseThrow(
                () ->
                    new RegraException(
                        404, "Lote não encontrado ou indisponível para seu acesso."));
    acesso.lote(u, l);
    return l;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detalhe(String codigo) {
    var l = consultar(codigo, false);
    var d = resumo(l);
    d.put(
        "historico",
        movimentos.findByLoteIdOrderByOcorridoEmAscIdAsc(l.id).stream()
            .map(
                m -> {
                  var x = new LinkedHashMap<String, Object>();
                  x.put("id", m.id);
                  x.put("anterior", m.anterior);
                  x.put("posterior", m.posterior);
                  x.put("tipo", m.tipo);
                  x.put("responsavel", m.responsavel);
                  x.put("ocorridoEm", m.ocorridoEm);
                  x.put("observacao", m.observacao);
                  return x;
                })
            .toList());
    d.put(
        "perdas",
        perdas.findByLoteIdOrderByOcorridoEmAscIdAsc(l.id).stream()
            .map(
                p ->
                    Map.of(
                        "id",
                        p.id,
                        "tipo",
                        p.tipo,
                        "quantidade",
                        p.quantidade,
                        "observacao",
                        p.observacao,
                        "responsavel",
                        p.responsavel,
                        "ocorridoEm",
                        p.ocorridoEm))
            .toList());
    return d;
  }

  public Map<String, Object> resumo(Lote l) {
    var d = new LinkedHashMap<String, Object>();
    d.put("id", l.id);
    d.put("codigo", l.codigo);
    d.put("programacao", l.programacao);
    d.put("modelo", l.modelo);
    d.put("cor", l.cor);
    d.put("banca", Map.of("id", l.banca.id, "nome", l.banca.nome));
    d.put("servico", Map.of("id", l.servico.id, "nome", l.servico.nome));
    d.put("prazo", l.prazo);
    d.put("grade", l.grade);
    d.put("totalPares", l.totalPares);
    d.put("totalPerdas", l.totalPerdas);
    d.put("aprovados", Calculos.aprovados(l.totalPares, l.totalPerdas));
    d.put("percentualPerdas", Calculos.percentual(l.totalPares, l.totalPerdas));
    d.put("status", l.status);
    d.put("criadoEm", l.criadoEm);
    d.put("enviadoEm", l.enviadoEm);
    d.put("concluidoEm", l.concluidoEm);
    d.put("motivoCancelamento", l.motivoCancelamento);
    if (acesso.atual().perfil != Perfil.OPERADOR_FABRICA) {
      d.put("valorPar", l.valorPar);
      d.put("valorPrevisto", Calculos.valor(l.totalPares, l.totalPerdas, l.valorPar));
      d.put(
          "valorDevido",
          l.status == Status.CONCLUIDO
              ? Calculos.valor(l.totalPares, l.totalPerdas, l.valorPar)
              : null);
    }
    return d;
  }

  @Transactional
  public Map<String, Object> salvar(String codigo, LoteInput d) {
    var u = acesso.exigir(Perfil.GESTOR);
    var l = codigo == null ? new Lote() : consultar(codigo, true);
    if (l.status != Status.AGUARDANDO_ENVIO)
      throw new RegraException(409, "Somente lotes aguardando envio podem ser editados.");
    var b =
        bancas
            .findById(d.bancaId())
            .filter(x -> x.ativo)
            .orElseThrow(() -> new RegraException(400, "Banca inexistente ou inativa."));
    var s =
        servicos
            .findById(d.servicoId())
            .filter(x -> x.ativo)
            .orElseThrow(() -> new RegraException(400, "Serviço inexistente ou inativo."));
    // A troca do serviço depois da criação confundiria o preço contratado. Crie outro lote.
    if (codigo != null && !l.servico.id.equals(s.id))
      throw new RegraException(
          409, "O serviço contratado não pode mudar. Cancele e crie outro lote.");
    int total = d.grade().values().stream().mapToInt(Integer::intValue).sum();
    if (total <= 0 || total > 100000000)
      throw new RegraException(400, "Informe entre 1 e 100 milhões de pares.");
    if (d.prazo().isBefore(LocalDate.now(BRASIL)))
      throw new RegraException(400, "Prazo não pode estar no passado.");
    l.programacao = d.programacao().trim();
    l.modelo = d.modelo().trim();
    l.cor = d.cor().trim();
    l.banca = b;
    l.servico = s;
    l.prazo = d.prazo();
    l.grade.clear();
    l.grade.putAll(d.grade());
    l.totalPares = total;
    if (codigo == null) {
      // UUID completo evita disputa por contadores anuais; a restrição UNIQUE é a última defesa.
      l.codigo =
          "L-"
              + LocalDate.now(BRASIL).getYear()
              + "-"
              + UUID.randomUUID().toString().replace("-", "").toUpperCase();
      l.valorPar = s.valorPar;
    }
    lotes.save(l);
    historico(l, u, codigo == null ? null : l.status, codigo == null ? "CRIACAO" : "EDICAO", null);
    return resumo(l);
  }

  @Transactional
  public Map<String, Object> movimentar(String codigo, String acao, String motivo) {
    var u =
        switch (acao) {
          case "enviar" -> acesso.exigir(Perfil.OPERADOR_FABRICA);
          case "receber", "devolver" -> acesso.exigir(Perfil.OPERADOR_BANCA);
          case "cancelar" -> acesso.exigir(Perfil.GESTOR);
          default -> throw new RegraException(400, "Ação inválida.");
        };
    var l = consultar(codigo, true);
    var anterior = l.status;
    var novo = Calculos.transicao(anterior, acao);
    if (acao.equals("cancelar") && (motivo == null || motivo.isBlank()))
      throw new RegraException(400, "Informe o motivo.");
    l.status = novo;
    if (acao.equals("enviar")) l.enviadoEm = Instant.now();
    if (acao.equals("devolver")) l.concluidoEm = Instant.now();
    if (acao.equals("cancelar")) l.motivoCancelamento = motivo;
    historico(l, u, anterior, acao.toUpperCase(), motivo);
    return resumo(l);
  }

  @Transactional
  public Map<String, Object> perda(String codigo, PerdaInput d) {
    var u = acesso.exigir(Perfil.OPERADOR_BANCA);
    var l = consultar(codigo, true);
    if (l.status != Status.EM_PROCESSAMENTO)
      throw new RegraException(
          409, "Perdas só podem ser registradas durante o processamento, antes da devolução.");
    Calculos.aprovados(l.totalPares, l.totalPerdas + d.quantidade());
    var p = new Perda();
    p.lote = l;
    p.usuario = u;
    p.responsavel = u.nome;
    p.tipo = d.tipo();
    p.quantidade = d.quantidade();
    p.observacao = d.observacao();
    perdas.save(p);
    l.totalPerdas += d.quantidade();
    return resumo(l);
  }

  private void historico(Lote l, Usuario u, Status anterior, String tipo, String obs) {
    var m = new Movimentacao();
    m.lote = l;
    m.usuario = u;
    m.responsavel = u.nome;
    m.anterior = anterior;
    m.posterior = l.status;
    m.tipo = tipo;
    m.observacao = obs;
    movimentos.saveAndFlush(m);
  }
}
