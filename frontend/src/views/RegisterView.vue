<template>
  <section class="auth-page">
    <form class="panel auth-card" @submit.prevent="register">
      <h1>Register</h1>
      <label>Username <input v-model="form.username" /></label>
      <label>Password <input v-model="form.password" type="password" /></label>
      <button>Register</button>
      <RouterLink to="/login">Back to login</RouterLink>
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
const form = reactive({ username: '', password: '' });

async function register() {
  error.value = '';
  try {
    const data = await request('/auth/register', { method: 'POST', body: JSON.stringify(form) });
    store.setSession(data.token, data.user);
    router.push('/play');
  } catch (err) {
    error.value = err.message;
  }
}
</script>
