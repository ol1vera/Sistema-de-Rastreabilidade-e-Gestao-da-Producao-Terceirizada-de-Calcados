import { test, expect } from "@playwright/test";
// Executar contra uma instalação de demonstração com MySQL; não substitui teste físico de câmera.
test("produção completa com quatro acessos e valores reais", async ({
  page,
}) => {
  const senha = process.env.DEMO_PASSWORD;
  if (!senha)
    throw new Error("Defina DEMO_PASSWORD com a senha da demonstração.");
  async function login(email) {
    await page.goto("/#/login");
    await page.getByLabel("E-mail", { exact: true }).fill(email);
    await page.getByLabel("Senha", { exact: true }).fill(senha);
    await page.getByRole("button", { name: "Entrar no sistema" }).click();
    await expect(
      page.getByRole("button", { name: "Sair", exact: true }),
    ).toBeVisible();
  }
  async function sair() {
    await page.getByRole("button", { name: "Sair", exact: true }).click();
  }
  async function leitura(codigo, acao) {
    await page.goto("/#/leitor");
    await page.getByLabel("Operação", { exact: true }).selectOption(acao);
    await page.getByLabel("Código do lote", { exact: true }).fill(codigo);
    await page
      .getByRole("button", { name: "Registrar leitura", exact: true })
      .click();
    await expect(page.locator(".notice.success")).toBeVisible();
  }
  await login("gestor@demo.local");
  await page.goto("/#/lotes/novo");
  await page
    .getByLabel("Número da programação", { exact: true })
    .fill("E2E-" + Date.now());
  await page.getByLabel("Modelo do calçado", { exact: true }).fill("Tênis E2E");
  await page.getByLabel("Cor", { exact: true }).fill("Preto");
  const prazo = new Date();
  prazo.setDate(prazo.getDate() + 7);
  await page
    .getByLabel("Prazo de retorno", { exact: true })
    .fill(prazo.toISOString().slice(0, 10));
  await page
    .getByLabel("Banca responsável", { exact: true })
    .selectOption({ label: "Banca Horizonte (demonstração)" });
  await page
    .getByLabel("Serviço contratado", { exact: true })
    .selectOption({ label: "Pesponto · R$ 2,50 / par" });
  await page.getByLabel("Numeração", { exact: true }).fill("38");
  await page.getByLabel("Quantidade de pares", { exact: true }).fill("300");
  await page
    .getByRole("button", { name: "Criar lote e gerar QR Code" })
    .click();
  await expect(page.locator(".page-head .code")).toBeVisible();
  const codigo = await page.locator(".page-head .code").innerText();
  await expect(page.locator("img.qr")).toBeVisible();
  await sair();
  await login("fabrica@demo.local");
  await leitura(codigo, "enviar");
  await sair();
  await login("banca@demo.local");
  await leitura(codigo, "receber");
  await page
    .getByRole("link", { name: "Abrir detalhes e perdas do lote" })
    .click();
  await page.getByLabel("Pares perdidos", { exact: true }).fill("8");
  await page.getByLabel("Observação", { exact: true }).fill("Teste integrado");
  page.once("dialog", (d) => d.accept());
  await page
    .getByRole("button", { name: "Registrar perda", exact: true })
    .click();
  await expect(page.locator(".notice.success")).toContainText(
    "Perda registrada",
  );
  await leitura(codigo, "devolver");
  await sair();
  await login("gestor@demo.local");
  await page.goto("/#/lotes/" + codigo);
  await expect(
    page.locator(".stat").filter({ hasText: "Valor devido" }),
  ).toContainText("730,00");
  await expect(page.locator(".timeline li")).toHaveCount(4);
  await page.goto("/#/");
  await expect(page.locator(".stats")).toBeVisible();
});
