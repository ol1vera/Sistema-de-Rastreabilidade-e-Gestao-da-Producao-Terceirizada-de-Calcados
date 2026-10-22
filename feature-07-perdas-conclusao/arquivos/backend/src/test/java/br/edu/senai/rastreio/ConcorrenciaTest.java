package br.edu.senai.rastreio;

import static org.junit.jupiter.api.Assertions.*;

import br.edu.senai.rastreio.dto.Entradas.PerdaInput;
import br.edu.senai.rastreio.entity.*;
import br.edu.senai.rastreio.service.LoteService;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/** Duas perdas concorrentes não podem consumir os mesmos pares disponíveis. */
@ActiveProfiles("test")
class ConcorrenciaTest extends FluxoBase {
  @Autowired LoteService service;

  @Test
  void perdasConcorrentesRespeitamLimite() throws Exception {
    String c = criar();
    processar(c);
    var inicio = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    Callable<Boolean> registrar =
        () -> {
          inicio.await();
          var ctx = SecurityContextHolder.createEmptyContext();
          ctx.setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  operador.id.toString(), null, java.util.List.of()));
          SecurityContextHolder.setContext(ctx);
          try {
            service.perda(c, new PerdaInput(TipoPerda.DANIFICADA, 200, "Teste concorrente"));
            return true;
          } catch (br.edu.senai.rastreio.exception.RegraException e) {
            assertEquals(409, e.status);
            return false;
          } finally {
            SecurityContextHolder.clearContext();
          }
        };
    try {
      var a = pool.submit(registrar);
      var b = pool.submit(registrar);
      inicio.countDown();
      assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
      assertEquals(200, lotes.findByCodigo(c).orElseThrow().totalPerdas);
    } finally {
      pool.shutdownNow();
    }
  }
}
