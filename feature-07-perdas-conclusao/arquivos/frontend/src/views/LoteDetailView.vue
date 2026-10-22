<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import { useAuth } from "../stores/auth";
import { useTask } from "../composables/useTask";
import { moeda, dataBR, tipos, numero, statusNomes } from "../utils";
import Feedback from "../components/Feedback.vue";
import StatusBadge from "../components/StatusBadge.vue";
const codigo = useRoute().params.codigo,
  auth = useAuth(),
  lote = ref(null),
  qr = ref(""),
  cancelarAberto = ref(false),
  motivo = ref("");
const perda = ref({ tipo: "DANIFICADA", quantidade: 1, observacao: "" });
const { busy, erro, sucesso, campos, run } = useTask();
let disposed = false;
async function carregar() {
  lote.value = (await api.get("/lotes/" + codigo)).data;
  if (!qr.value) {
    const { data } = await api.get("/lotes/" + codigo + "/qrcode", {
      responseType: "blob",
    });
    if (!disposed) qr.value = URL.createObjectURL(data);
  }
}
onMounted(() => run(carregar));
onBeforeUnmount(() => {
  disposed = true;
  if (qr.value) URL.revokeObjectURL(qr.value);
});
function cancelar() {
  run(async () => {
    await api.post("/lotes/" + codigo + "/cancelar", { motivo: motivo.value });
    cancelarAberto.value = false;
    await carregar();
    sucesso.value = "Lote cancelado. O histórico foi preservado.";
  });
}
function registrarPerda() {
  if (
    !confirm(
      "Confirmar o registro desta perda? O histórico não poderá ser apagado.",
    )
  )
    return;
  run(async () => {
    await api.post("/lotes/" + codigo + "/perdas", perda.value);
    await carregar();
    perda.value = { tipo: "DANIFICADA", quantidade: 1, observacao: "" };
    sucesso.value = "Perda registrada.";
  });
}
function imprimir() {
  window.print();
}
</script>
<template>
  <Feedback
    :erro="erro"
    :busy="busy"
    :sucesso="sucesso"
    :campos="campos"
  /><template v-if="lote"
    ><div class="page-head">
      <div>
        <RouterLink to="/lotes">← Lotes de produção</RouterLink>
        <h1>
          {{ lote.modelo }} <span class="muted">/ {{ lote.cor }}</span>
        </h1>
        <p class="code">{{ lote.codigo }}</p>
      </div>
      <StatusBadge :status="lote.status" />
    </div>
    <div class="actions" v-if="auth.perfil === 'GESTOR'">
      <button class="secondary" :disabled="!qr" @click="imprimir">
        Imprimir etiqueta</button
      ><RouterLink
        v-if="lote.status === 'AGUARDANDO_ENVIO'"
        class="secondary button"
        :to="'/lotes/' + codigo + '/editar'"
        >Editar lote</RouterLink
      ><button
        v-if="lote.status === 'AGUARDANDO_ENVIO'"
        class="danger secondary"
        @click="cancelarAberto = !cancelarAberto"
      >
        Cancelar lote
      </button>
    </div>
    <form v-if="cancelarAberto" class="panel" @submit.prevent="cancelar">
      <label
        >Motivo do cancelamento<textarea
          v-model="motivo"
          required
          maxlength="500"
        ></textarea></label
      ><button class="danger" :disabled="busy">Confirmar cancelamento</button>
    </form>
    <div class="detail-grid">
      <section class="panel">
        <h3>Dados do lote</h3>
        <dl>
          <dt>Programação</dt>
          <dd>{{ lote.programacao }}</dd>
          <dt>Banca</dt>
          <dd>{{ lote.banca.nome }}</dd>
          <dt>Serviço</dt>
          <dd>{{ lote.servico.nome }}</dd>
          <dt>Prazo</dt>
          <dd>{{ dataBR(lote.prazo) }}</dd>
          <dt>Criado em</dt>
          <dd>{{ dataBR(lote.criadoEm, true) }}</dd>
          <dt>Valor por par contratado</dt>
          <dd>{{ moeda(lote.valorPar) }}</dd>
        </dl>
        <div class="grade-pills">
          <span v-for="(q, n) in lote.grade" :key="n"
            >{{ n }} <strong>{{ q }} pares</strong></span
          >
        </div>
        <div v-if="lote.motivoCancelamento" class="notice error">
          Cancelamento: {{ lote.motivoCancelamento }}
        </div>
      </section>
      <section class="panel label-card">
        <h3>Identificação do lote</h3>
        <img v-if="qr" :src="qr" class="qr" alt="QR Code do lote" />
        <p class="code">{{ lote.codigo }}</p>
        <strong>{{ lote.banca.nome }}</strong>
        <p>{{ numero(lote.totalPares) }} pares</p>
      </section>
    </div>
    <div class="stats">
      <article class="stat">
        <span>Total</span><strong>{{ numero(lote.totalPares) }}</strong
        ><small>pares enviados</small>
      </article>
      <article class="stat">
        <span>Perdas</span><strong>{{ numero(lote.totalPerdas) }}</strong
        ><small>{{ numero(lote.percentualPerdas) }}%</small>
      </article>
      <article class="stat">
        <span>Aprovados</span><strong>{{ numero(lote.aprovados) }}</strong
        ><small>pares sem perdas</small>
      </article>
      <article class="stat" v-if="lote.valorPrevisto != null">
        <span>{{
          lote.status === "CONCLUIDO"
            ? "Valor devido"
            : "Previsão · não consolidada"
        }}</span
        ><strong>{{ moeda(lote.valorDevido ?? lote.valorPrevisto) }}</strong
        ><small>Preço contratado × aprovados</small>
      </article>
    </div>
    <section
      v-if="
        auth.perfil === 'OPERADOR_BANCA' && lote.status === 'EM_PROCESSAMENTO'
      "
      class="panel"
    >
      <h3>Registrar perda antes da devolução</h3>
      <form @submit.prevent="registrarPerda">
        <div class="form-grid">
          <label
            >Tipo<select v-model="perda.tipo">
              <option v-for="(n, t) in tipos" :value="t" :key="t">
                {{ n }}
              </option>
            </select></label
          ><label
            >Pares perdidos<input
              type="number"
              v-model.number="perda.quantidade"
              min="1"
              :max="lote.aprovados"
              step="1"
              required /></label
          ><label class="span2"
            >Observação<textarea
              v-model="perda.observacao"
              required
              maxlength="500"
            ></textarea>
          </label>
        </div>
        <button :disabled="busy || lote.aprovados === 0">
          Registrar perda
        </button>
      </form>
      <p>
        <RouterLink to="/leitor"
          >Ir ao leitor para registrar a devolução →</RouterLink
        >
      </p>
    </section>
    <div class="detail-grid">
      <section class="panel">
        <h3>Linha do tempo</h3>
        <ol class="timeline">
          <li v-for="m in lote.historico" :key="m.id">
            <strong>{{
              m.tipo === "CRIACAO"
                ? "Lote criado"
                : m.tipo === "EDICAO"
                  ? "Dados atualizados"
                  : statusNomes[m.posterior]
            }}</strong>
            <p>{{ m.responsavel }} · {{ dataBR(m.ocorridoEm, true) }}</p>
            <small v-if="m.anterior"
              >{{ statusNomes[m.anterior] }} →
              {{ statusNomes[m.posterior] }}</small
            >
            <p v-if="m.observacao">{{ m.observacao }}</p>
          </li>
        </ol>
      </section>
      <section class="panel">
        <h3>Histórico de perdas</h3>
        <p class="empty" v-if="!lote.perdas.length">
          Nenhuma perda registrada.
        </p>
        <article class="loss" v-for="p in lote.perdas" :key="p.id">
          <strong>{{ p.quantidade }} pares · {{ tipos[p.tipo] }}</strong>
          <p>{{ p.observacao }}</p>
          <small>{{ p.responsavel }} · {{ dataBR(p.ocorridoEm, true) }}</small>
        </article>
      </section>
    </div>
    <div class="print-label">
      <img :src="qr" alt="QR Code" /><strong>{{ lote.codigo }}</strong>
      <p>{{ lote.banca.nome }}</p>
      <b>{{ lote.totalPares }} pares</b>
    </div></template
  >
</template>
