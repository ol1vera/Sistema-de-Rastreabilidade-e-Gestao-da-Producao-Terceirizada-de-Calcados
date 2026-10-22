package br.edu.senai.rastreio.service;

import br.edu.senai.rastreio.entity.*;
import br.edu.senai.rastreio.exception.RegraException;
import br.edu.senai.rastreio.security.Acesso;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agregações no banco: o painel não carrega todos os lotes na memória do servidor. */
@Service
@Transactional(readOnly = true)
public class PainelService {
  private final NamedParameterJdbcTemplate jdbc;
  private final Acesso acesso;

  public PainelService(NamedParameterJdbcTemplate j, Acesso a) {
    jdbc = j;
    acesso = a;
  }

  private record Filtro(String sql, MapSqlParameterSource params) {}

  private Filtro filtro(LocalDate inicio, LocalDate fim, Long banca, Status status, String data) {
    if (inicio != null && fim != null && fim.isBefore(inicio))
      throw new RegraException(400, "Período inválido.");
    String sql = " WHERE 1=1";
    var p = new MapSqlParameterSource();
    if (inicio != null) {
      sql += " AND l." + data + ">=:inicio";
      p.addValue("inicio", Timestamp.from(inicio.atStartOfDay(LoteService.BRASIL).toInstant()));
    }
    if (fim != null) {
      sql += " AND l." + data + "<:fim";
      p.addValue(
          "fim", Timestamp.from(fim.plusDays(1).atStartOfDay(LoteService.BRASIL).toInstant()));
    }
    if (banca != null) {
      sql += " AND l.banca_id=:banca";
      p.addValue("banca", banca);
    }
    if (status != null) {
      sql += " AND l.status=:status";
      p.addValue("status", status.name());
    }
    return new Filtro(sql, p);
  }

  public Map<String, Object> painel(LocalDate inicio, LocalDate fim, Long banca, Status status) {
    acesso.exigir(Perfil.GESTOR, Perfil.ADMIN);
    var f = filtro(inicio, fim, banca, status, "criado_em");
    var p = f.params();
    p.addValue("hoje", LocalDate.now(LoteService.BRASIL));
    var d = new LinkedHashMap<String, Object>();
    d.put(
        "indicadores",
        jdbc.queryForMap(
            "SELECT COUNT(*) AS lotes, COALESCE(SUM(total_pares),0) AS pares,"
                + " COALESCE(SUM(total_perdas),0) AS perdas,"
                + " COALESCE(ROUND(100.0*SUM(total_perdas)/NULLIF(SUM(total_pares),0),2),0) AS"
                + " percentual, COALESCE(SUM(CASE WHEN status='CONCLUIDO' THEN"
                + " (total_pares-total_perdas)*valor_par ELSE 0 END),0) AS devido,"
                + " COALESCE(AVG(CASE WHEN status='CONCLUIDO' AND enviado_em IS NOT NULL THEN"
                + " TIMESTAMPDIFF(SECOND,enviado_em,concluido_em)/3600.0 END),0) AS cicloHoras,"
                + " COALESCE(SUM(CASE WHEN prazo<:hoje AND status NOT IN ('CONCLUIDO','CANCELADO')"
                + " THEN 1 ELSE 0 END),0) AS atrasados FROM lotes l"
                + f.sql(),
            p));
    d.put(
        "status",
        jdbc.queryForList(
            "SELECT status AS rotulo,COUNT(*) AS valor FROM lotes l" + f.sql() + " GROUP BY status",
            p));
    d.put(
        "bancas",
        jdbc.queryForList(
            "SELECT b.nome AS rotulo,COUNT(*) AS valor FROM lotes l JOIN bancas b ON"
                + " b.id=l.banca_id"
                + f.sql()
                + " GROUP BY b.id,b.nome",
            p));
    d.put(
        "tiposPerda",
        jdbc.queryForList(
            "SELECT pe.tipo AS rotulo,SUM(pe.quantidade) AS valor FROM perdas pe JOIN lotes l ON"
                + " l.id=pe.lote_id"
                + f.sql()
                + " GROUP BY pe.tipo",
            p));
    d.put(
        "evolucao",
        jdbc.queryForList(
            "SELECT CAST(TIMESTAMPADD(HOUR,-3,criado_em) AS DATE) AS rotulo,COUNT(*) AS valor FROM"
                + " lotes l"
                + f.sql()
                + " GROUP BY CAST(TIMESTAMPADD(HOUR,-3,criado_em) AS DATE) ORDER BY rotulo DESC"
                + " LIMIT 30",
            p));
    d.put(
        "valores",
        jdbc.queryForList(
            "SELECT b.nome AS rotulo,SUM((total_pares-total_perdas)*valor_par) AS valor FROM lotes"
                + " l JOIN bancas b ON b.id=l.banca_id"
                + f.sql()
                + " AND l.status='CONCLUIDO' GROUP BY b.id,b.nome",
            p));
    d.put(
        "atrasados",
        jdbc.queryForList(
            "SELECT codigo,modelo,b.nome AS banca,prazo FROM lotes l JOIN bancas b ON"
                + " b.id=l.banca_id"
                + f.sql()
                + " AND prazo<:hoje AND status NOT IN ('CONCLUIDO','CANCELADO') ORDER BY prazo"
                + " LIMIT 20",
            p));
    d.put("atualizadoEm", Instant.now());
    return d;
  }

  public Object financeiro(LocalDate inicio, LocalDate fim, Long banca, int page, int size) {
    acesso.exigir(Perfil.ADMIN, Perfil.GESTOR);
    var f = filtro(inicio, fim, banca, Status.CONCLUIDO, "concluido_em");
    String ano = "EXTRACT(YEAR FROM TIMESTAMPADD(HOUR,-3,concluido_em))",
        mes = "EXTRACT(MONTH FROM TIMESTAMPADD(HOUR,-3,concluido_em))";
    String base = " FROM lotes l JOIN bancas b ON b.id=l.banca_id" + f.sql(),
        grupo = " GROUP BY b.id,b.nome," + ano + "," + mes;
    var p =
        f.params()
            .addValue("limite", Math.min(100, Math.max(1, size)))
            .addValue("offset", Math.max(0, page) * Math.min(100, Math.max(1, size)));
    var itens =
        jdbc.queryForList(
            "SELECT b.nome AS banca,"
                + ano
                + " AS ano,"
                + mes
                + " AS mes,COUNT(*) AS lotes,SUM(total_pares-total_perdas) AS"
                + " aprovados,SUM((total_pares-total_perdas)*valor_par) AS valor"
                + base
                + grupo
                + " ORDER BY ano DESC,mes DESC,b.nome LIMIT :limite OFFSET :offset",
            p);
    var total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM (SELECT b.id" + base + grupo + ") grupos", p, Long.class);
    var soma =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM((total_pares-total_perdas)*valor_par),0)" + base,
            p,
            java.math.BigDecimal.class);
    return Map.of(
        "content",
        itens,
        "totalElements",
        total,
        "totalPages",
        (total + Math.min(100, Math.max(1, size)) - 1) / Math.min(100, Math.max(1, size)),
        "number",
        Math.max(0, page),
        "totalDevido",
        soma);
  }
}
