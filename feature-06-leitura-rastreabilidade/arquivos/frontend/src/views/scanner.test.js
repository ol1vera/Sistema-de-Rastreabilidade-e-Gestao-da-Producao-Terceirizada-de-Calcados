import { mount, flushPromises } from "@vue/test-utils";
import { beforeEach, describe, it, expect, vi } from "vitest";
const mock = vi.hoisted(() => ({
  decode: null,
  stop: vi.fn(),
  clear: vi.fn(),
}));
vi.mock("html5-qrcode", () => ({
  Html5Qrcode: class {
    isScanning = true;
    async start(a, b, decode) {
      mock.decode = decode;
    }
    async stop() {
      mock.stop();
      this.isScanning = false;
    }
    clear() {
      mock.clear();
    }
  },
}));
vi.mock("../stores/auth", () => ({
  useAuth: () => ({ perfil: "OPERADOR_FABRICA" }),
}));
import { api } from "../api";
import Leitor from "./LeitorView.vue";
beforeEach(() => {
  Object.defineProperty(window, "isSecureContext", {
    value: true,
    configurable: true,
  });
  vi.restoreAllMocks();
});
describe("leitor com câmera simulada", () => {
  it("lê automaticamente e mostra sucesso sem captura", async () => {
    vi.spyOn(api, "post").mockResolvedValue({
      data: { codigo: "L-2026-ABC", status: "ENVIADO_BANCA" },
    });
    const w = mount(Leitor, { global: { stubs: { RouterLink: true } } });
    await flushPromises();
    mock.decode("L-2026-ABC");
    await flushPromises();
    expect(api.post).toHaveBeenCalledWith("/lotes/leitura/enviar", {
      codigo: "L-2026-ABC",
    });
    expect(w.get('[role="status"]').text()).toContain("Enviado à banca");
    w.unmount();
    await flushPromises();
    expect(mock.stop).toHaveBeenCalled();
  });
  it("digitação manual usa a mesma rota e mostra erro", async () => {
    vi.spyOn(api, "post").mockRejectedValue({
      response: { data: { mensagem: "Leitura repetida." } },
    });
    const w = mount(Leitor, { global: { stubs: { RouterLink: true } } });
    await flushPromises();
    await w.get("input").setValue("L-2026-ABC");
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(w.get('[role="alert"]').text()).toContain("Leitura repetida");
    w.unmount();
  });
  it("não envia QR estranho ao servidor", async () => {
    const post = vi.spyOn(api, "post");
    const w = mount(Leitor, { global: { stubs: { RouterLink: true } } });
    await flushPromises();
    mock.decode("https://site-externo.example");
    await flushPromises();
    expect(post).not.toHaveBeenCalled();
    expect(w.get('[role="alert"]').text()).toContain("QR Code inválido");
    w.unmount();
  });
});
