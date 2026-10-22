<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { api, parametros } from "../api";
import { useTask } from "../composables/useTask";
import { numero, moeda, dataBR, statusNomes } from "../utils";
import Feedback from "../components/Feedback.vue";
import Filtros from "../components/Filtros.vue";
import Grafico from "../components/Grafico.vue";
const dados = ref(null),
  filtros = ref({ inicio: "", fim: "", bancaId: "", status: "" }),
  aplicados = ref({});
const { busy, erro, run } = useTask();
let timer;
function carregar() {
  return run(async () => {
    dados.value = (
      await api.get("/dashboard", { params: parametros(aplicados.value) })
    ).data;
  });
}
function filtrar() {
  aplicados.value = { ...filtros.value };
  carregar();
}
onMounted(() => {
  carregar();
  timer = setInterval(carregar, Number(import.meta.env.VITE_POLL_MS) || 15000);
});
onBeforeUnmount(() => clearInterval(timer));
</script>
<template>
  <div class="page-head">
    <div>
      <span class="eyebrow">ACOMPANHAMENTO DA OPERAÇÃO</span>
      <h1>Visão geral</h1>
      <p>Da saída da fábrica ao retorno de cada lote.</p>
    </div>
    <span class="live"><span class="dot"></span> Atualização automática</span>
  </div>
  <Filtros v-model="filtros" @apply="filtrar" /><Feedback
    :erro="erro"
    :busy="busy && !dados"
  /><template v-if="dados"
    ><div class="stats">
      <article class="stat">
        <span>Lotes no período</span
        ><strong>{{ numero(dados.indicadores.lotes) }}</strong
        ><small>{{ numero(dados.indicadores.pares) }} pares registrados</small>
      </article>
      <article class="stat">
        <span>Lotes atrasados</span
        ><strong class="amber">{{ numero(dados.indicadores.atrasados) }}</strong
        ><small>Prazo de retorno ultrapassado</small>
      </article>
      <article class="stat">
        <span>Perdas registradas</span
        ><strong
          >{{ numero(dados.indicadores.perdas) }} <small>pares</small></strong
        ><small>{{ numero(dados.indicadores.percentual) }}% do total</small>
      </article>
      <article class="stat accent">
        <span>Valor devido</span
        ><strong>{{ moeda(dados.indicadores.devido) }}</strong
        ><small>Somente lotes concluídos</small>
      </article>
    </div>
    <div class="status-strip">
      <div v-for="(nome, status) in statusNomes" :key="status">
        <strong>{{
          dados.status.find((x) => x.rotulo === status)?.valor || 0
        }}</strong
        ><span>{{ nome }}</span>
      </div>
    </div>
    <div class="charts-grid">
      <Grafico
        :dados="dados.status"
        titulo="Distribuição por status"
        tipo="doughnut"
      /><Grafico :dados="dados.bancas" titulo="Lotes por banca" /><Grafico
        :dados="[...dados.evolucao].reverse()"
        titulo="Evolução dos lotes · últimos 30 dias com registros"
        tipo="line"
      /><Grafico :dados="dados.tiposPerda" titulo="Perdas por tipo" /><Grafico
        :dados="dados.valores"
        titulo="Valores devidos por banca (R$)"
      />
      <section class="panel cycle">
        <span class="eyebrow">TEMPO MÉDIO DE CICLO</span
        ><strong
          >{{
            numero(dados.indicadores.cicloHoras ?? dados.indicadores.ciclohoras)
          }}
          <small>horas</small></strong
        >
        <p>
          Intervalo entre envio e conclusão, considerando os lotes concluídos do
          filtro.
        </p>
        <small>Atualizado em {{ dataBR(dados.atualizadoEm, true) }}</small>
      </section>
    </div>
    <section class="panel">
      <h3>Lotes que precisam de atenção</h3>
      <p class="muted">
        Até 20 lotes atrasados, ordenados pelo prazo mais antigo.
      </p>
      <div v-if="!dados.atrasados.length" class="empty">
        Nenhum lote atrasado neste filtro.
      </div>
      <div class="table-wrap" v-else>
        <table>
          <thead>
            <tr>
              <th>Lote</th>
              <th>Modelo</th>
              <th>Banca</th>
              <th>Prazo</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in dados.atrasados" :key="l.codigo">
              <td>
                <RouterLink :to="'/lotes/' + l.codigo">{{
                  l.codigo
                }}</RouterLink>
              </td>
              <td>{{ l.modelo }}</td>
              <td>{{ l.banca }}</td>
              <td>{{ dataBR(l.prazo) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section></template
  >
</template>
