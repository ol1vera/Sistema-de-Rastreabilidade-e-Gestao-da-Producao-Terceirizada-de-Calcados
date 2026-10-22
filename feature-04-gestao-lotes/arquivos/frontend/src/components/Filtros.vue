<script setup>
import { ref, onMounted } from "vue";
import { catalogo } from "../api";
import { statusNomes } from "../utils";
import { useAuth } from "../stores/auth";
const model = defineModel({ required: true });
defineProps({ busca: Boolean, financeiro: Boolean });
defineEmits(["apply"]);
const bancas = ref([]),
  erro = ref(""),
  auth = useAuth();
onMounted(async () => {
  try {
    bancas.value = await catalogo("bancas");
  } catch {
    erro.value = "Não foi possível carregar as bancas.";
  }
});
</script>
<template>
  <form class="filters" @submit.prevent="$emit('apply')">
    <label v-if="busca"
      >Código do lote<input
        v-model="model.busca"
        placeholder="Buscar código…" /></label
    ><label v-if="auth.perfil !== 'OPERADOR_BANCA'"
      >Banca<select v-model="model.bancaId">
        <option value="">Todas as bancas</option>
        <option v-for="b in bancas" :value="b.id" :key="b.id">
          {{ b.nome }}
        </option>
      </select></label
    ><label v-if="!financeiro"
      >Status<select v-model="model.status">
        <option value="">Todos os status</option>
        <option v-for="(n, s) in statusNomes" :value="s" :key="s">
          {{ n }}
        </option>
      </select></label
    ><label
      >{{ financeiro ? "Conclusão de" : "Criação de"
      }}<input type="date" v-model="model.inicio" /></label
    ><label
      >Até<input type="date" v-model="model.fim" :min="model.inicio" /></label
    ><button class="secondary" type="submit">Filtrar</button
    ><small v-if="erro" role="alert">{{ erro }}</small>
  </form>
</template>
