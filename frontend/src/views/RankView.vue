<template>
  <section>
    <header class="page-header"><h1>Rank</h1></header>
    <div class="panel">
      <table>
        <thead><tr><th>Player</th><th>Rating</th></tr></thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="user-cell"><img :src="user.photo" alt="" />{{ user.username }}</td>
            <td>{{ user.rating }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { request } from '../api/http';

const users = ref([]);
onMounted(async () => {
  users.value = await request('/ranklist');
});
</script>
