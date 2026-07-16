<template>
  <section>
    <header class="page-header"><h1>Records</h1></header>
    <div class="panel">
      <table>
        <thead><tr><th>Player</th><th>Opponent</th><th>Result</th><th>Time</th></tr></thead>
        <tbody>
          <tr v-for="record in records" :key="record.id">
            <td>{{ record.playerName }}</td>
            <td>{{ record.opponentName }}</td>
            <td>{{ record.result }}</td>
            <td>{{ new Date(record.createdAt).toLocaleString() }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { request } from '../api/http';

const records = ref([]);
onMounted(async () => {
  records.value = await request('/records');
});
</script>
