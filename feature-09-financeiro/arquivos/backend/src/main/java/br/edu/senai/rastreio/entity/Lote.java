package br.edu.senai.rastreio.entity;

import jakarta.persistence.*;

/** Registro persistido de lote; alterações são controladas pela camada de serviço. */
@Entity
@Table(name = "lotes")
public class Lote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 50)
  public String codigo;

  @Column(nullable = false, length = 100)
  public String programacao;

  @Column(nullable = false, length = 120)
  public String modelo;

  @Column(nullable = false, length = 80)
  public String cor;

  @ManyToOne(optional = false)
  @JoinColumn(name = "banca_id")
  public Banca banca;

  @ManyToOne(optional = false)
  @JoinColumn(name = "servico_id")
  public Servico servico;

  @Column(nullable = false)
  public java.time.LocalDate prazo;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "lote_grade", joinColumns = @JoinColumn(name = "lote_id"))
  @MapKeyColumn(name = "numeracao", length = 20)
  @Column(name = "quantidade", nullable = false)
  public java.util.Map<String, Integer> grade = new java.util.TreeMap<>();

  @Column(nullable = false)
  public int totalPares;

  @Column(nullable = false)
  public int totalPerdas;

  @Column(nullable = false, precision = 12, scale = 2)
  public java.math.BigDecimal valorPar;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Column(nullable = false, length = 30)
  public Status status = Status.AGUARDANDO_ENVIO;

  @Column(nullable = false)
  public java.time.Instant criadoEm = java.time.Instant.now();

  public java.time.Instant enviadoEm;
  public java.time.Instant concluidoEm;

  @Column(length = 500)
  public String motivoCancelamento;

  @Version public Long versao;
}
