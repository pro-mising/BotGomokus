<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">KOB Demo</div>
      <nav v-if="store.token">
        <RouterLink to="/play">Play</RouterLink>
        <RouterLink to="/bots">My Bots</RouterLink>
        <RouterLink to="/rank">Rank</RouterLink>
        <RouterLink to="/records">Records</RouterLink>
      </nav>
      <div v-if="store.user" class="profile">
        <img :src="store.user.photo" alt="" />
        <div>
          <strong>{{ store.user.username }}</strong>
          <span>{{ store.user.rating }} pts</span>
        </div>
      </div>
      <button v-if="store.token" class="ghost" @click="logout">Logout</button>
    </aside>
    <main><RouterView /></main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { store } from './main';

const router = useRouter();
function logout() {
  store.logout();
  router.push('/login');
}
</script>
