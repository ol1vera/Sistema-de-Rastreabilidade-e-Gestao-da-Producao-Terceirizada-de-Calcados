package br.edu.senai.rastreio.entity;

import jakarta.persistence.*;

/** Registro persistido de usuario; alterações são controladas pela camada de serviço. */
@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String nome;

  @Column(nullable = false, unique = true, length = 180)
  public String email;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Column(nullable = false)
  public String senha;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Column(nullable = false, length = 30)
  public Perfil perfil;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "banca_id")
  public Banca banca;

  @Column(nullable = false)
  public boolean ativo = true;
}
