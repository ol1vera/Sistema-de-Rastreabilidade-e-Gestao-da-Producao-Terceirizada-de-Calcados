package br.edu.senai.rastreio.controller;

import br.edu.senai.rastreio.dto.Entradas.*;
import br.edu.senai.rastreio.entity.Status;
import br.edu.senai.rastreio.service.LoteService;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Entrada HTTP; as permissões e regras são executadas pelo serviço. */
@RestController
@RequestMapping("/api/lotes")
public class LoteController {
  private final LoteService s;

  public LoteController(LoteService s) {
    this.s = s;
  }

  @GetMapping
  public Object listar(
      @RequestParam(required = false) String busca,
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) Long bancaId,
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return s.listar(busca, status, bancaId, inicio, fim, page, size);
  }

  @GetMapping("/{codigo}")
  public Object detalhe(@PathVariable String codigo) {
    return s.detalhe(codigo);
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object criar(@Valid @RequestBody LoteInput d) {
    return s.salvar(null, d);
  }

  @PutMapping("/{codigo}")
  public Object editar(@PathVariable String codigo, @Valid @RequestBody LoteInput d) {
    return s.salvar(codigo, d);
  }

  @PostMapping("/{codigo}/cancelar")
  public Object cancelar(@PathVariable String codigo, @Valid @RequestBody Cancelamento d) {
    return s.movimentar(codigo, "cancelar", d.motivo());
  }

  @PostMapping("/leitura/{acao}")
  public Object ler(@PathVariable String acao, @Valid @RequestBody Leitura d) {
    if (!java.util.List.of("enviar", "receber", "devolver").contains(acao))
      throw new br.edu.senai.rastreio.exception.RegraException(400, "Ação de leitura inválida.");
    return s.movimentar(d.codigo(), acao, null);
  }

  @PostMapping("/{codigo}/perdas")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object perda(@PathVariable String codigo, @Valid @RequestBody PerdaInput d) {
    return s.perda(codigo, d);
  }

  @GetMapping(value = "/{codigo}/qrcode", produces = "image/png")
  public byte[] qr(@PathVariable String codigo) throws Exception {
    s.detalhe(codigo); // Consulta autorizada antes de revelar ou gerar qualquer imagem.
    var matriz =
        new MultiFormatWriter()
            .encode(
                codigo,
                BarcodeFormat.QR_CODE,
                480,
                480,
                Map.of(
                    EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN,
                    4));
    var bytes = new java.io.ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(matriz, "PNG", bytes);
    return bytes.toByteArray();
  }
}
