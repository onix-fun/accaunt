<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import Chart from 'chart.js/auto';

const props = defineProps<{
  data: number[];
  color?: string;
}>();

const canvasRef = ref<HTMLCanvasElement | null>(null);
let chartInstance: Chart | null = null;

onMounted(() => {
  if (canvasRef.value) {
    chartInstance = new Chart(canvasRef.value, {
      type: 'bar',
      data: {
        labels: props.data.map((_, i) => i.toString()),
        datasets: [{
          data: props.data,
          backgroundColor: props.color || '#10b981',
          borderRadius: 2,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { display: false },
          y: { display: false, min: 0 }
        },
        plugins: {
          legend: { display: false },
          tooltip: { enabled: false }
        },
        animation: false
      }
    });
  }
});

watch(() => props.data, (newData) => {
  if (chartInstance) {
    chartInstance.data.datasets[0].data = newData;
    chartInstance.update();
  }
}, { deep: true });

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.destroy();
  }
});
</script>

<template>
  <div class="w-full h-full">
    <canvas ref="canvasRef"></canvas>
  </div>
</template>
