<script setup>
import { ref, onMounted } from "vue";
import { api, parametros } from "../api";
import { useAuth } from "../stores/auth";
import { useTask } from "../composables/useTask";
import { dataBR, numero } from "../utils";
import Feedback from "../components/Feedback.vue";
import Filtros from "../components/Filtros.vue";
import Pagination from "../components/Pagination.vue";
import StatusBadge from "../components/StatusBadge.vue";
const auth = useAuth(),
  lista = ref(null),
  page = ref(0),
  filtros = ref({ busca: "", status: "", bancaId: "", inicio: "", fim: "" }),
  aplicados = ref({});
const { busy, erro, run } = useTask();
function carregar(p = 0) {
  page.value = p;
  return run(async () => {
    lista.value = (
      await api.get("/lotes", {
        params: parametros({ ...aplicados.value, page: p, size: 20 }),
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
      <span class="eyebrow">RASTREABILIDADE</span>
      <h1>Lotes de produção</h1>
      <p>Consulte a programação e acompanhe cada etapa.</p>
    </div>
    <RouterLink v-if="auth.perfil === 'GESTOR'" class="button" to="/lotes/novo"
      >+ Criar lote</RouterLink
    >
  </div>
  <Filtros v-model="filtros" busca @apply="filtrar" /><Feedback
    :erro="erro"
    :busy="busy"
  />
  <section class="panel" v-if="lista">
    <div v-if="!lista.content.length" class="empty">
      <h3>Nenhum lote encontrado</h3>
      <p>Crie um lote ou ajuste os filtros de busca.</p>
    </div>
    <div class="table-wrap" v-else>
      <table>
        <thead>
          <tr>
            <th>Lote / programação</th>
            <th>Modelo / cor</th>
            <th>Banca</th>
            <th>Pares</th>
            <th>Status</th>
            <th>Criação</th>
            <th>Prazo</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in lista.content" :key="l.codigo">
            <td>
              <RouterLink class="code" :to="'/lotes/' + l.codigo">{{
                l.codigo
              }}</RouterLink
              ><small>{{ l.programacao }}</small>
            </td>
            <td>
              {{ l.modelo }}<small>{{ l.cor }}</small>
            </td>
            <td>{{ l.banca.nome }}</td>
            <td>{{ numero(l.totalPares) }}</td>
            <td><StatusBadge :status="l.status" /></td>
            <td>{{ dataBR(l.criadoEm) }}</td>
            <td>{{ dataBR(l.prazo) }}</td>
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
