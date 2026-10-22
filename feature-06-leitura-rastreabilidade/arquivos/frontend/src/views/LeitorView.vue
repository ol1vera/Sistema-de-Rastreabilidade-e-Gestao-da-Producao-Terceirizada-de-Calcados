<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { Html5Qrcode } from "html5-qrcode";
import { api } from "../api";
import { useAuth } from "../stores/auth";
import { criarTrava } from "../scanGate";
import { statusNomes, mensagem } from "../utils";
const auth = useAuth(),
  acao = ref(auth.perfil === "OPERADOR_FABRICA" ? "enviar" : "receber"),
  codigo = ref(""),
  busy = ref(false),
  erro = ref(""),
  sucesso = ref(""),
  cameraErro = ref(""),
  ultimo = ref(null),
  duracao = ref(null),
  cameraPronta = ref(false);
let scanner,
  disposed = false,
  iniciando = false;
const trava = criarTrava();
async function processar(valor, manual = false) {
  const c = valor.trim().toUpperCase();
  await trava.executar(
    c,
    async () => {
      busy.value = true;
      erro.value = "";
      sucesso.value = "";
      ultimo.value = null;
      const inicio = performance.now();
      try {
        if (!/^L-\d{4}-[A-Z0-9-]{1,43}$/.test(c)) {
          erro.value =
            "QR Code inválido. Use a etiqueta de um lote do sistema.";
          return;
        }
        const { data } = await api.post("/lotes/leitura/" + acao.value, {
          codigo: c,
        });
        ultimo.value = data;
        sucesso.value = `${data.codigo}: ${statusNomes[data.status]}.`;
        duracao.value = Math.round(performance.now() - inicio);
      } catch (e) {
        erro.value = mensagem(e);
      } finally {
        busy.value = false;
      }
    },
    manual,
  );
}
/** O componente libera a câmera ao sair, inclusive se a permissão terminar depois da navegação. */
async function iniciar() {
  if (iniciando || cameraPronta.value) return;
  iniciando = true;
  cameraErro.value = "";
  try {
    if (!window.isSecureContext) throw new Error("HTTPS");
    scanner = new Html5Qrcode("camera");
    await scanner.start(
      { facingMode: "environment" },
      { fps: 10, qrbox: { width: 230, height: 230 } },
      (txt) => processar(txt),
      () => {},
    );
    if (disposed) {
      await scanner.stop();
      scanner.clear();
    } else cameraPronta.value = true;
  } catch (e) {
    cameraErro.value = String(e).includes("HTTPS")
      ? "A câmera precisa de HTTPS ou localhost. Use o código manual enquanto isso."
      : "Não foi possível abrir a câmera. Verifique a permissão do navegador, conecte uma webcam e tente novamente.";
  } finally {
    iniciando = false;
  }
}
onMounted(iniciar);
onBeforeUnmount(async () => {
  disposed = true;
  if (scanner?.isScanning) {
    await scanner.stop();
    scanner.clear();
  }
});
</script>
<template>
  <div class="page-head">
    <div>
      <span class="eyebrow">OPERAÇÃO POR QR CODE</span>
      <h1>Leitura de lotes</h1>
      <p>Aponte a câmera para a etiqueta. A leitura é automática.</p>
    </div>
  </div>
  <div class="reader-grid">
    <section class="panel">
      <label
        >Operação<select v-model="acao" :disabled="busy">
          <option v-if="auth.perfil === 'OPERADOR_FABRICA'" value="enviar">
            Saída da fábrica
          </option>
          <template v-else
            ><option value="receber">Recebimento na banca</option>
            <option value="devolver">Devolução à fábrica</option></template
          >
        </select></label
      >
      <div v-if="acao === 'devolver'" class="notice warning">
        Registre todas as perdas no detalhe do lote antes de apontar a câmera. A
        devolução conclui o lote e bloqueia novas perdas.
      </div>
      <div id="camera" class="camera"></div>
      <div v-if="cameraErro" class="notice error" role="alert">
        {{ cameraErro
        }}<button class="secondary" @click="iniciar">
          Tentar câmera novamente
        </button>
      </div>
      <p class="muted">Mantenha o QR Code inteiro dentro do enquadramento.</p>
    </section>
    <section class="panel">
      <h3>Código manual</h3>
      <p class="muted">
        Etiqueta danificada? Digite o código para executar a mesma operação.
      </p>
      <form @submit.prevent="processar(codigo, true)">
        <label
          >Código do lote<input
            v-model="codigo"
            placeholder="L-2026-…"
            required
            maxlength="50"
            autocomplete="off" /></label
        ><button :disabled="busy">
          {{ busy ? "Registrando…" : "Registrar leitura" }}
        </button>
      </form>
      <div v-if="busy" class="loading" role="status">Validando lote…</div>
      <div v-if="erro" class="notice error" role="alert">{{ erro }}</div>
      <div v-if="sucesso" class="notice success" role="status">
        {{ sucesso
        }}<small
          >Resposta da API e interface: {{ duracao }} ms. Não inclui o tempo de
          detecção da câmera.</small
        >
      </div>
      <RouterLink v-if="ultimo" :to="'/lotes/' + ultimo.codigo"
        >Abrir detalhes e perdas do lote →</RouterLink
      >
      <div class="reader-help">
        <h3>Uma leitura, um registro</h3>
        <p>
          O sistema confere a banca, o perfil e a ordem das etapas antes de
          atualizar o lote.
        </p>
        <p>Retire a etiqueta do enquadramento após a confirmação.</p>
      </div>
    </section>
  </div>
</template>
