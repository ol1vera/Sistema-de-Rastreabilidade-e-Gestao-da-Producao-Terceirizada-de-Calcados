package br.edu.senai.rastreio.entity;

import jakarta.persistence.*;

/** Registro persistido de banca; alterações são controladas pela camada de serviço. */
@Entity
@Table(name = "bancas")
public class Banca {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String nome;

  @Column(nullable = false, unique = true, length = 14)
  public String documento;

  @Column(nullable = false, length = 120)
  public String responsavel;

  @Column(nullable = false, length = 20)
  public String telefone;

  @Column(nullable = false)
  public boolean ativo = true;
}
