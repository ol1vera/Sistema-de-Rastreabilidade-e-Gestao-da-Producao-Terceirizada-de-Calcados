package br.edu.senai.rastreio.service;

import br.edu.senai.rastreio.entity.Status;
import br.edu.senai.rastreio.exception.RegraException;
import java.math.*;

/** Regras puras: podem ser testadas sem servidor ou banco. */
public class Calculos {
  public static int aprovados(int total, int perdas) {
    if (total <= 0 || perdas < 0 || perdas > total)
      throw new RegraException(409, "As perdas não podem superar o total do lote.");
    return total - perdas;
  }

  public static BigDecimal percentual(int total, int perdas) {
    aprovados(total, perdas);
    return BigDecimal.valueOf(perdas)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }

  public static BigDecimal valor(int total, int perdas, BigDecimal preco) {
    return preco
        .multiply(BigDecimal.valueOf(aprovados(total, perdas)))
        .setScale(2, RoundingMode.HALF_UP);
  }

  public static Status transicao(Status atual, String acao) {
    Status esperado =
        switch (acao) {
          case "enviar", "cancelar" -> Status.AGUARDANDO_ENVIO;
          case "receber" -> Status.ENVIADO_BANCA;
          case "devolver" -> Status.EM_PROCESSAMENTO;
          default -> throw new RegraException(400, "Ação inválida.");
        };
    if (atual != esperado)
      throw new RegraException(
          409, "Leitura repetida ou fora de ordem. Status atual: " + atual + ".");
    return switch (acao) {
      case "enviar" -> Status.ENVIADO_BANCA;
      case "receber" -> Status.EM_PROCESSAMENTO;
      case "devolver" -> Status.CONCLUIDO;
      default -> Status.CANCELADO;
    };
  }
}
