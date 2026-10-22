<script setup>
defineProps({ erro: String, sucesso: String, busy: Boolean, campos: Object });
</script>
<template>
  <div v-if="erro" class="notice error" role="alert">
    {{ erro }}
    <ul v-if="campos && Object.keys(campos).length">
      <li v-for="(texto, campo) in campos" :key="campo">
        {{ campo }}: {{ texto }}
      </li>
    </ul>
  </div>
  <div v-if="sucesso" class="notice success" role="status">{{ sucesso }}</div>
  <div v-if="busy" class="loading" role="status">Carregando…</div>
</template>
