package br.edu.senai.rastreio.entity;

import jakarta.persistence.*;

/** Registro persistido de servico; alterações são controladas pela camada de serviço. */
@Entity
@Table(name = "servicos")
public class Servico {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String nome;

  @Column(length = 500)
  public String descricao;

  @Column(nullable = false, precision = 12, scale = 2)
  public java.math.BigDecimal valorPar;

  @Column(nullable = false)
  public boolean ativo = true;
}
