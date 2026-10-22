package br.edu.senai.rastreio.controller;

import br.edu.senai.rastreio.entity.Status;
import br.edu.senai.rastreio.service.PainelService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;

/** Painel por criação e financeiro por conclusão: períodos com semântica explícita. */
@RestController
@RequestMapping("/api")
public class PainelController {
  private final PainelService s;

  public PainelController(PainelService s) {
    this.s = s;
  }

  @GetMapping("/dashboard")
  public Object painel(
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim,
      @RequestParam(required = false) Long bancaId,
      @RequestParam(required = false) Status status) {
    return s.painel(inicio, fim, bancaId, status);
  }

  @GetMapping("/relatorios/financeiro")
  public Object financeiro(
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim,
      @RequestParam(required = false) Long bancaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return s.financeiro(inicio, fim, bancaId, page, size);
  }
}
