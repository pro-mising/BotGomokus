<template>
  <section>
    <header class="page-header">
      <div>
        <h1>Play</h1>
        <p>A REST-based mini battle. Later you can replace it with WebSocket.</p>
      </div>
      <button @click="play">Start Battle</button>
    </header>
    <div class="layout">
      <div class="panel board-panel">
        <div v-if="record" class="result">
          <strong>{{ record.result }}</strong>
          <span>{{ record.ratingDelta > 0 ? '+' : '' }}{{ record.ratingDelta }} pts</span>
        </div>
        <div class="board">
          <div v-for="(row, r) in map" :key="r" class="board-row">
            <span
              v-for="(cell, c) in row"
              :key="`${r}-${c}`"
              class="cell"
              :class="{ wall: cell === 1, player: r === 7 && c === 1, enemy: r === 1 && c === 7 }"
            />
          </div>
        </div>
      </div>
      <div class="panel">
        <h2>What to trace</h2>
        <ul class="notes">
          <li>Button calls POST /api/games/play.</li>
          <li>Backend returns map, result, and rating.</li>
          <li>Vue updates the board and user score.</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue';
import { request } from '../api/http';
import { store } from '../main';

const record = ref(null);
const fallbackMap = Array.from({ length: 9 }, (_, r) =>
  Array.from({ length: 9 }, (_, c) => (r === 0 || c === 0 || r === 8 || c === 8 ? 1 : 0))
);
const map = computed(() => record.value?.map || fallbackMap);

async function play() {
  const data = await request('/games/play', { method: 'POST', body: JSON.stringify({ botId: null }) });
  record.value = data.record;
  store.user = { ...store.user, rating: data.rating };
  localStorage.setItem('kob-demo-user', JSON.stringify(store.user));
}
</script>
