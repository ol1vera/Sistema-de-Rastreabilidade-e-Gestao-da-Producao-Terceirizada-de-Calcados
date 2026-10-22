<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from "vue";
import Chart from "chart.js/auto";
import { statusNomes, tipos } from "../utils";
const props = defineProps({
  dados: Array,
  titulo: String,
  tipo: { type: String, default: "bar" },
});
const canvas = ref();
let chart;
function desenhar() {
  chart?.destroy();
  if (!canvas.value) return;
  chart = new Chart(canvas.value, {
    type: props.tipo,
    data: {
      labels: (props.dados || []).map(
        (x) => statusNomes[x.rotulo] || tipos[x.rotulo] || String(x.rotulo),
      ),
      datasets: [
        {
          label: props.titulo,
          data: (props.dados || []).map((x) => Number(x.valor)),
          backgroundColor: [
            "#176b5b",
            "#3da58c",
            "#91cbb5",
            "#b9d7cb",
            "#e5a558",
          ],
          borderColor: "#176b5b",
          borderWidth: 1,
          borderRadius: 5,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: props.tipo === "doughnut", position: "bottom" },
      },
    },
  });
}
onMounted(desenhar);
watch(() => props.dados, desenhar, { deep: true });
onBeforeUnmount(() => chart?.destroy());
</script>
<template>
  <section class="panel">
    <h3>{{ titulo }}</h3>
    <div v-if="!dados?.length" class="empty small">Sem dados no período.</div>
    <div class="chart">
      <canvas ref="canvas" :aria-label="titulo" role="img"></canvas>
    </div>
  </section>
</template>
