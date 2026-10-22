package br.edu.senai.rastreio;

import static org.junit.jupiter.api.Assertions.*;

import br.edu.senai.rastreio.entity.Status;
import br.edu.senai.rastreio.exception.RegraException;
import br.edu.senai.rastreio.service.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Testa regras e arredondamentos com exemplos de aceitação oficiais. */
class CalculosTest {
  @Test
  void exemploOficial() {
    assertEquals(292, Calculos.aprovados(300, 8));
    assertEquals(new BigDecimal("2.67"), Calculos.percentual(300, 8));
    assertEquals(new BigDecimal("730.00"), Calculos.valor(300, 8, new BigDecimal("2.50")));
  }

  @Test
  void perdaTotal() {
    assertEquals(new BigDecimal("0.00"), Calculos.valor(300, 300, new BigDecimal("2.50")));
  }

  @Test
  void rejeitaExcesso() {
    assertThrows(RegraException.class, () -> Calculos.aprovados(300, 301));
  }

  @Test
  void rejeitaNegativos() {
    assertThrows(RegraException.class, () -> Calculos.aprovados(300, -1));
    assertThrows(RegraException.class, () -> Calculos.aprovados(0, 0));
  }

  @Test
  void matrizCompletaDeTransicoes() {
    for (var s : Status.values())
      for (var a : new String[] {"enviar", "receber", "devolver", "cancelar"}) {
        boolean ok =
            (s == Status.AGUARDANDO_ENVIO && (a.equals("enviar") || a.equals("cancelar")))
                || (s == Status.ENVIADO_BANCA && a.equals("receber"))
                || (s == Status.EM_PROCESSAMENTO && a.equals("devolver"));
        if (ok) assertNotNull(Calculos.transicao(s, a));
        else assertThrows(RegraException.class, () -> Calculos.transicao(s, a));
      }
  }

  @Test
  void documentos() {
    assertTrue(Documentos.valido("11222333000181"));
    assertTrue(Documentos.valido("52998224725"));
    assertFalse(Documentos.valido("11111111111"));
    assertFalse(Documentos.valido("11222333000182"));
  }
}
