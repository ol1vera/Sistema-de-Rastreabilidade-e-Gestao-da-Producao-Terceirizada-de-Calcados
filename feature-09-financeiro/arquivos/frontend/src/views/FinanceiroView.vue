<script setup>
import { ref, onMounted } from "vue";
import { api, parametros } from "../api";
import { useTask } from "../composables/useTask";
import { moeda, numero } from "../utils";
import Feedback from "../components/Feedback.vue";
import Filtros from "../components/Filtros.vue";
import Pagination from "../components/Pagination.vue";
const filtros = ref({ inicio: "", fim: "", bancaId: "" }),
  aplicados = ref({}),
  lista = ref(null),
  page = ref(0);
const { busy, erro, run } = useTask();
function carregar(p = 0) {
  page.value = p;
  run(async () => {
    lista.value = (
      await api.get("/relatorios/financeiro", {
        params: parametros({ ...aplicados.value, page: p }),
      })
    ).data;
  });
}
function filtrar() {
  aplicados.value = { ...filtros.value };
  carregar();
}
onMounted(() => carregar());
</script>
<template>
  <div class="page-head">
    <div>
      <span class="eyebrow">CONSOLIDAÇÃO FINANCEIRA</span>
      <h1>Valores por banca</h1>
      <p>
        Pares aprovados × preço contratado. Competência pela conclusão do lote,
        em Brasília.
      </p>
    </div>
  </div>
  <Filtros v-model="filtros" financeiro @apply="filtrar" /><Feedback
    :erro="erro"
    :busy="busy"
  />
  <section v-if="lista" class="panel">
    <div class="total-bar">
      Total devido no filtro <strong>{{ moeda(lista.totalDevido) }}</strong>
    </div>
    <div v-if="!lista.content.length" class="empty">
      Nenhum lote concluído para este filtro.
    </div>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Banca</th>
            <th>Competência</th>
            <th>Lotes</th>
            <th>Pares aprovados</th>
            <th>Valor devido</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in lista.content" :key="i">
            <td>{{ r.banca }}</td>
            <td>{{ String(r.mes).padStart(2, "0") }}/{{ r.ano }}</td>
            <td>{{ r.lotes }}</td>
            <td>{{ numero(r.aprovados) }}</td>
            <td>
              <strong>{{ moeda(r.valor) }}</strong>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <Pagination
      :page="page"
      :pages="lista.totalPages"
      :total="lista.totalElements"
      @change="carregar"
    />
  </section>
</template>
