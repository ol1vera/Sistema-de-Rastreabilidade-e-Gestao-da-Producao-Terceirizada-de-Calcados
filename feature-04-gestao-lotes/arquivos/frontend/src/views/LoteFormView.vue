<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, catalogo } from "../api";
import { useTask } from "../composables/useTask";
import { hoje, moeda } from "../utils";
import Feedback from "../components/Feedback.vue";
const route = useRoute(),
  router = useRouter(),
  codigo = route.params.codigo,
  bancas = ref([]),
  servicos = ref([]),
  pronto = ref(false);
const { busy, erro, campos, run } = useTask();
const form = ref({
    programacao: "",
    modelo: "",
    cor: "",
    bancaId: "",
    servicoId: "",
    prazo: "",
  }),
  grade = ref([{ numero: "", quantidade: 0 }]);
const total = computed(() =>
  grade.value.reduce((s, g) => s + Number(g.quantidade || 0), 0),
);
onMounted(() =>
  run(async () => {
    [bancas.value, servicos.value] = await Promise.all([
      catalogo("bancas"),
      catalogo("servicos"),
    ]);
    if (codigo) {
      const { data: l } = await api.get("/lotes/" + codigo);
      form.value = {
        programacao: l.programacao,
        modelo: l.modelo,
        cor: l.cor,
        bancaId: l.banca.id,
        servicoId: l.servico.id,
        prazo: l.prazo,
      };
      grade.value = Object.entries(l.grade).map(([numero, quantidade]) => ({
        numero,
        quantidade,
      }));
    }
    pronto.value = true;
  }),
);
function salvar() {
  run(async () => {
    const numeros = grade.value.map((x) => x.numero.trim());
    if (new Set(numeros).size !== numeros.length) {
      erro.value = "Cada numeração deve aparecer apenas uma vez.";
      return;
    }
    if (total.value <= 0) {
      erro.value = "Informe ao menos um par.";
      return;
    }
    const body = {
      ...form.value,
      grade: Object.fromEntries(
        grade.value.map((g) => [g.numero.trim(), Number(g.quantidade)]),
      ),
    };
    const { data } = codigo
      ? await api.put("/lotes/" + codigo, body)
      : await api.post("/lotes", body);
    router.push("/lotes/" + data.codigo);
  });
}
</script>
<template>
  <div class="page-head">
    <div>
      <RouterLink to="/lotes">← Voltar aos lotes</RouterLink>
      <h1>{{ codigo ? "Editar lote" : "Novo lote de produção" }}</h1>
      <p>
        Informe a programação. O código e o QR Code serão gerados ao salvar.
      </p>
    </div>
  </div>
  <Feedback :erro="erro" :campos="campos" :busy="busy" />
  <form v-if="pronto" @submit.prevent="salvar">
    <section class="panel">
      <h3>01 · Dados da programação</h3>
      <div class="form-grid">
        <label
          >Número da programação<input
            v-model="form.programacao"
            required
            maxlength="100" /></label
        ><label
          >Modelo do calçado<input
            v-model="form.modelo"
            required
            maxlength="120" /></label
        ><label>Cor<input v-model="form.cor" required maxlength="80" /></label
        ><label
          >Prazo de retorno<input
            v-model="form.prazo"
            type="date"
            :min="hoje()"
            required /></label
        ><label
          >Banca responsável<select v-model="form.bancaId" required>
            <option value="" disabled>Selecione a banca</option>
            <option
              v-for="b in bancas.filter((x) => x.ativo)"
              :key="b.id"
              :value="b.id"
            >
              {{ b.nome }}
            </option>
          </select></label
        ><label
          >Serviço contratado<select
            v-model="form.servicoId"
            required
            :disabled="!!codigo"
          >
            <option value="" disabled>Selecione o serviço</option>
            <option
              v-for="s in servicos.filter(
                (x) => x.ativo || x.id === form.servicoId,
              )"
              :key="s.id"
              :value="s.id"
            >
              {{ s.nome }} · {{ moeda(s.valorPar) }} / par
            </option></select
          ><small v-if="codigo">O preço contratado é preservado.</small></label
        >
      </div>
    </section>
    <section class="panel">
      <h3>02 · Grade de numeração</h3>
      <p class="muted">Use as numerações necessárias para esta programação.</p>
      <div class="grade-row" v-for="(g, i) in grade" :key="i">
        <label
          >Numeração<input
            v-model="g.numero"
            required
            maxlength="20"
            placeholder="Ex.: 38" /></label
        ><label
          >Quantidade de pares<input
            v-model.number="g.quantidade"
            required
            type="number"
            min="0"
            max="1000000"
            step="1" /></label
        ><button
          type="button"
          class="ghost"
          :disabled="grade.length === 1"
          @click="grade.splice(i, 1)"
          :aria-label="'Remover numeração ' + (i + 1)"
        >
          Remover
        </button>
      </div>
      <button
        class="secondary"
        type="button"
        :disabled="grade.length >= 100"
        @click="grade.push({ numero: '', quantidade: 0 })"
      >
        + Adicionar numeração
      </button>
      <div class="total-bar">
        Total da programação <strong>{{ total }} pares</strong>
      </div>
    </section>
    <div class="actions">
      <RouterLink to="/lotes" class="secondary button">Cancelar</RouterLink
      ><button :disabled="busy">
        {{
          busy
            ? "Salvando…"
            : codigo
              ? "Salvar alterações"
              : "Criar lote e gerar QR Code"
        }}
      </button>
    </div>
  </form>
</template>
