<template>
  <section class="auth-page">
    <form class="panel auth-card" @submit.prevent="login">
      <h1>Login</h1>
      <p>Demo account: alice / 123456</p>
      <label>Username <input v-model="form.username" /></label>
      <label>Password <input v-model="form.password" type="password" /></label>
      <button>Login</button>
      <RouterLink to="/register">Create an account</RouterLink>
      <p v-if="error" class="error">{{ error }}</p>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { request } from '../api/http';
import { store } from '../main';

const router = useRouter();
const error = ref('');
const form = reactive({ username: 'alice', password: '123456' });

async function login() {
  error.value = '';
  try {
    const data = await request('/auth/login', { method: 'POST', body: JSON.stringify(form) });
    store.setSession(data.token, data.user);
    router.push('/play');
  } catch (err) {
    error.value = err.message;
  }
}
</script>
