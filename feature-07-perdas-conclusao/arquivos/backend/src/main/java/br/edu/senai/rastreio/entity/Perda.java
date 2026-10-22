package br.edu.senai.rastreio.entity;

import jakarta.persistence.*;

/** Registro persistido de perda; alterações são controladas pela camada de serviço. */
@Entity
@Table(name = "perdas")
@org.hibernate.annotations.Immutable
public class Perda {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "lote_id", updatable = false)
  public Lote lote;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id", updatable = false)
  public Usuario usuario;

  @Column(nullable = false, updatable = false, length = 120)
  public String responsavel;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Column(nullable = false, updatable = false, length = 30)
  public TipoPerda tipo;

  @Column(nullable = false, updatable = false)
  public int quantidade;

  @Column(nullable = false, updatable = false, length = 500)
  public String observacao;

  @Column(nullable = false, updatable = false)
  public java.time.Instant ocorridoEm = java.time.Instant.now();
}
