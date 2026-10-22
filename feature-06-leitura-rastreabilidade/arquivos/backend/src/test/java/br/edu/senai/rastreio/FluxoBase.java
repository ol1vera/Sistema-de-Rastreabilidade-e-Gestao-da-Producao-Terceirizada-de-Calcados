package br.edu.senai.rastreio;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import br.edu.senai.rastreio.entity.*;
import br.edu.senai.rastreio.repository.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Mesmos cenários HTTP no H2 auxiliar e no MySQL real com Testcontainers. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class FluxoBase {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UsuarioRepository usuarios;
  @Autowired BancaRepository bancas;
  @Autowired ServicoRepository servicos;
  @Autowired PasswordEncoder encoder;
  @Autowired LoteRepository lotes;
  Banca banca, outra;
  Servico servico;
  Map<Perfil, String> tokens;
  Usuario operador;
  String senha = "SenhaDeTeste123!";

  @BeforeEach
  void preparar() throws Exception {
    banca = banca("Principal");
    outra = banca("Outra");
    servico = new Servico();
    servico.nome = "Costura";
    servico.valorPar = new BigDecimal("2.50");
    servicos.save(servico);
    tokens = new EnumMap<>(Perfil.class);
    for (var p : Perfil.values()) {
      var u = new Usuario();
      u.nome = p.name();
      u.email = UUID.randomUUID() + "@teste.local";
      u.senha = encoder.encode(senha);
      u.perfil = p;
      if (p == Perfil.OPERADOR_BANCA) {
        u.banca = banca;
        operador = u;
      }
      usuarios.save(u);
      tokens.put(p, login(u.email, senha));
    }
  }

  Banca banca(String nome) {
    var b = new Banca();
    b.nome = nome;
    b.documento = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    b.telefone = "37999999999";
    b.responsavel = "Teste";
    return bancas.save(b);
  }

  String login(String email, String senha) throws Exception {
    return chamada(post("/api/auth/login"), null, Map.of("email", email, "senha", senha), 200)
        .path("token")
        .asText();
  }

  JsonNode chamada(MockHttpServletRequestBuilder req, String token, Object body, int status)
      throws Exception {
    if (token != null) req.header("Authorization", "Bearer " + token);
    if (body != null)
      req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
    var r = mvc.perform(req).andReturn().getResponse();
    assertEquals(status, r.getStatus(), r.getContentAsString());
    return r.getContentAsByteArray().length == 0
        ? json.nullNode()
        : json.readTree(r.getContentAsByteArray());
  }

  Map<String, Object> entrada(Long bancaId) {
    return Map.of(
        "programacao",
        "P-001",
        "modelo",
        "Tênis",
        "cor",
        "Preto",
        "bancaId",
        bancaId,
        "servicoId",
        servico.id,
        "prazo",
        LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(2).toString(),
        "grade",
        Map.of("38", 100, "39", 200));
  }

  String criar() throws Exception {
    return chamada(post("/api/lotes"), tokens.get(Perfil.GESTOR), entrada(banca.id), 201)
        .path("codigo")
        .asText();
  }

  JsonNode mover(String c, String acao, Perfil p, int status) throws Exception {
    return chamada(post("/api/lotes/leitura/" + acao), tokens.get(p), Map.of("codigo", c), status);
  }

  void processar(String c) throws Exception {
    mover(c, "enviar", Perfil.OPERADOR_FABRICA, 200);
    mover(c, "receber", Perfil.OPERADOR_BANCA, 200);
  }

  @Test
  void fluxoCompletoFinanceiroHistoricoEPainel() throws Exception {
    String c = criar();
    processar(c);
    chamada(
        post("/api/lotes/" + c + "/perdas"),
        tokens.get(Perfil.OPERADOR_BANCA),
        Map.of("tipo", "DANIFICADA", "quantidade", 8, "observacao", "Costura danificada"),
        201);
    var retorno = mover(c, "devolver", Perfil.OPERADOR_BANCA, 200);
    assertEquals(292, retorno.path("aprovados").asInt());
    assertEquals(730, retorno.path("valorDevido").asDouble());
    assertEquals(2.67, retorno.path("percentualPerdas").asDouble());
    var detalhe = chamada(get("/api/lotes/" + c), tokens.get(Perfil.GESTOR), null, 200);
    assertEquals(4, detalhe.path("historico").size());
    assertEquals(1, detalhe.path("perdas").size());
    var fin =
        chamada(
            get("/api/relatorios/financeiro").param("bancaId", banca.id.toString()),
            tokens.get(Perfil.GESTOR),
            null,
            200);
    assertEquals(730, fin.path("totalDevido").asDouble());
    var painel =
        chamada(
            get("/api/dashboard").param("bancaId", banca.id.toString()),
            tokens.get(Perfil.GESTOR),
            null,
            200);
    assertEquals(8, painel.path("indicadores").path("perdas").asInt());
    assertEquals(1, painel.path("indicadores").path("lotes").asInt());
  }

  @Test
  void loginInvalidoEUsuarioDesativado() throws Exception {
    chamada(post("/api/auth/login"), null, Map.of("email", operador.email, "senha", "errada"), 401);
    operador.ativo = false;
    usuarios.save(operador);
    chamada(post("/api/auth/login"), null, Map.of("email", operador.email, "senha", senha), 401);
    chamada(get("/api/lotes"), tokens.get(Perfil.OPERADOR_BANCA), null, 401);
  }

  @Test
  void tokenAusenteOuAdulterado() throws Exception {
    chamada(get("/api/lotes"), null, null, 401);
    chamada(get("/api/lotes"), "token-invalido", null, 401);
  }

  @Test
  void isolamentoBancaEmTodosOsAcessos() throws Exception {
    String c =
        chamada(post("/api/lotes"), tokens.get(Perfil.GESTOR), entrada(outra.id), 201)
            .path("codigo")
            .asText();
    mover(c, "enviar", Perfil.OPERADOR_FABRICA, 200);
    chamada(get("/api/lotes/" + c), tokens.get(Perfil.OPERADOR_BANCA), null, 404);
    chamada(get("/api/lotes/" + c + "/qrcode"), tokens.get(Perfil.OPERADOR_BANCA), null, 404);
    mover(c, "receber", Perfil.OPERADOR_BANCA, 404);
    chamada(
        post("/api/lotes/" + c + "/perdas"),
        tokens.get(Perfil.OPERADOR_BANCA),
        Map.of("tipo", "DANIFICADA", "quantidade", 1, "observacao", "Teste"),
        404);
    var lista =
        chamada(
            get("/api/lotes").param("bancaId", outra.id.toString()),
            tokens.get(Perfil.OPERADOR_BANCA),
            null,
            200);
    assertEquals(0, lista.path("totalElements").asInt());
  }

  @Test
  void perfisNaoPodemBurlarRotas() throws Exception {
    chamada(post("/api/lotes"), tokens.get(Perfil.ADMIN), entrada(banca.id), 403);
    chamada(get("/api/usuarios"), tokens.get(Perfil.GESTOR), null, 403);
    chamada(get("/api/dashboard"), tokens.get(Perfil.OPERADOR_BANCA), null, 403);
    mover(criar(), "enviar", Perfil.OPERADOR_BANCA, 403);
  }

  @Test
  void edicaoCancelamentoEStatus() throws Exception {
    String c = criar();
    chamada(put("/api/lotes/" + c), tokens.get(Perfil.GESTOR), entrada(banca.id), 200);
    chamada(
        post("/api/lotes/" + c + "/cancelar"),
        tokens.get(Perfil.GESTOR),
        Map.of("motivo", ""),
        400);
    chamada(
        post("/api/lotes/" + c + "/cancelar"),
        tokens.get(Perfil.GESTOR),
        Map.of("motivo", "Programação suspensa"),
        200);
    mover(c, "enviar", Perfil.OPERADOR_FABRICA, 409);
    chamada(put("/api/lotes/" + c), tokens.get(Perfil.GESTOR), entrada(banca.id), 409);
  }

  @Test
  void repeticaoEOrdem() throws Exception {
    String c = criar();
    mover(c, "receber", Perfil.OPERADOR_BANCA, 409);
    mover(c, "enviar", Perfil.OPERADOR_FABRICA, 200);
    mover(c, "enviar", Perfil.OPERADOR_FABRICA, 409);
    chamada(put("/api/lotes/" + c), tokens.get(Perfil.GESTOR), entrada(banca.id), 409);
    chamada(
        post("/api/lotes/" + c + "/cancelar"),
        tokens.get(Perfil.GESTOR),
        Map.of("motivo", "Teste"),
        409);
  }

  @Test
  void precoCongeladoENaoConcluidosSemDivida() throws Exception {
    String c = criar();
    servico.valorPar = new BigDecimal("3.00");
    servicos.save(servico);
    chamada(put("/api/lotes/" + c), tokens.get(Perfil.GESTOR), entrada(banca.id), 200);
    var d = chamada(get("/api/lotes/" + c), tokens.get(Perfil.GESTOR), null, 200);
    assertEquals(2.5, d.path("valorPar").asDouble());
    assertTrue(d.path("valorDevido").isNull());
    String novo = criar();
    assertNotEquals(c, novo);
    assertEquals(
        3,
        chamada(get("/api/lotes/" + novo), tokens.get(Perfil.GESTOR), null, 200)
            .path("valorPar")
            .asDouble());
    assertEquals(
        0,
        chamada(
                get("/api/relatorios/financeiro").param("bancaId", banca.id.toString()),
                tokens.get(Perfil.GESTOR),
                null,
                200)
            .path("totalDevido")
            .asInt());
  }

  @Test
  void perdaAcimaDoTotalENaConclusao() throws Exception {
    String c = criar();
    processar(c);
    var corpo = Map.of("tipo", "EXTRAVIADA", "quantidade", 301, "observacao", "Teste");
    chamada(post("/api/lotes/" + c + "/perdas"), tokens.get(Perfil.OPERADOR_BANCA), corpo, 409);
    mover(c, "devolver", Perfil.OPERADOR_BANCA, 200);
    chamada(
        post("/api/lotes/" + c + "/perdas"),
        tokens.get(Perfil.OPERADOR_BANCA),
        Map.of("tipo", "DANIFICADA", "quantidade", 1, "observacao", "Teste"),
        409);
  }

  @Test
  void validacaoGradeFiltrosEPaginacao() throws Exception {
    var d = new HashMap<>(entrada(banca.id));
    d.put("grade", Map.of("38", -1));
    chamada(post("/api/lotes"), tokens.get(Perfil.GESTOR), d, 400);
    d.put("grade", Map.of("38", 0));
    chamada(post("/api/lotes"), tokens.get(Perfil.GESTOR), d, 400);
    criar();
    criar();
    var lista =
        chamada(
            get("/api/lotes").param("bancaId", banca.id.toString()).param("size", "1"),
            tokens.get(Perfil.GESTOR),
            null,
            200);
    assertEquals(2, lista.path("totalElements").asInt());
    assertEquals(1, lista.path("content").size());
    chamada(
        get("/api/lotes").param("inicio", "2026-10-20").param("fim", "2026-10-01"),
        tokens.get(Perfil.GESTOR),
        null,
        400);
  }

  @Test
  void qrContemSomenteCodigoEReimpressaoPreserva() throws Exception {
    String c = criar();
    var r =
        mvc.perform(
                get("/api/lotes/" + c + "/qrcode")
                    .header("Authorization", "Bearer " + tokens.get(Perfil.GESTOR)))
            .andReturn()
            .getResponse();
    assertEquals(200, r.getStatus());
    var img =
        javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(r.getContentAsByteArray()));
    var bitmap =
        new com.google.zxing.BinaryBitmap(
            new com.google.zxing.common.HybridBinarizer(
                new com.google.zxing.client.j2se.BufferedImageLuminanceSource(img)));
    assertEquals(c, new com.google.zxing.MultiFormatReader().decode(bitmap).getText());
    var outraImagem =
        mvc.perform(
                get("/api/lotes/" + c + "/qrcode")
                    .header("Authorization", "Bearer " + tokens.get(Perfil.GESTOR)))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertArrayEquals(r.getContentAsByteArray(), outraImagem);
  }

  @Test
  void cadastrosEHashSeguro() throws Exception {
    assertTrue(operador.senha.startsWith("$2"));
    var body =
        Map.of(
            "nome",
            "Nova banca",
            "documento",
            "52998224725",
            "responsavel",
            "Maria",
            "telefone",
            "37999990000",
            "ativo",
            true);
    var existente = bancas.findAll().stream().anyMatch(b -> b.documento.equals("52998224725"));
    if (!existente) chamada(post("/api/bancas"), tokens.get(Perfil.ADMIN), body, 201);
    chamada(
        post("/api/bancas"),
        tokens.get(Perfil.ADMIN),
        Map.of(
            "nome",
            "Inválida",
            "documento",
            "11111111111",
            "responsavel",
            "Teste",
            "telefone",
            "37999990000",
            "ativo",
            true),
        400);
    chamada(
        post("/api/servicos"),
        tokens.get(Perfil.ADMIN),
        Map.of("nome", "Colagem", "valorPar", 2.75, "ativo", true),
        201);
    chamada(
        post("/api/usuarios"),
        tokens.get(Perfil.ADMIN),
        Map.of(
            "nome",
            "Sem banca",
            "email",
            "teste@exemplo.com",
            "senha",
            senha,
            "perfil",
            "OPERADOR_BANCA",
            "ativo",
            true),
        400);
    var me = chamada(get("/api/auth/me"), tokens.get(Perfil.OPERADOR_BANCA), null, 200);
    assertFalse(me.has("senha"));
  }
}
