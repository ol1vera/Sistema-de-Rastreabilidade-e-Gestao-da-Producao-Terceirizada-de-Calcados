<script setup>
defineProps({ page: Number, pages: Number, total: Number });
defineEmits(["change"]);
</script>
<template>
  <div class="pagination">
    <span
      >{{ total || 0 }} registros · Página {{ page + 1 }} de
      {{ Math.max(1, pages || 0) }}</span
    >
    <div>
      <button
        class="secondary"
        :disabled="page === 0"
        @click="$emit('change', page - 1)"
      >
        Anterior</button
      ><button
        class="secondary"
        :disabled="page + 1 >= pages"
        @click="$emit('change', page + 1)"
      >
        Próxima
      </button>
    </div>
  </div>
</template>
