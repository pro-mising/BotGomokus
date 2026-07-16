<template>
  <section>
    <header class="page-header">
      <div>
        <h1>My Bots</h1>
        <p>Practice create, read, update, and delete.</p>
      </div>
      <button @click="startCreate">New Bot</button>
    </header>
    <div class="layout">
      <div class="panel">
        <table>
          <thead><tr><th>Name</th><th>Description</th><th></th></tr></thead>
          <tbody>
            <tr v-for="bot in bots" :key="bot.id">
              <td>{{ bot.title }}</td>
              <td>{{ bot.description }}</td>
              <td class="actions">
                <button class="ghost" @click="edit(bot)">Edit</button>
                <button class="danger" @click="remove(bot.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <form class="panel editor" @submit.prevent="save">
        <h2>{{ editingId ? 'Edit Bot' : 'New Bot' }}</h2>
        <label>Title <input v-model="form.title" /></label>
        <label>Description <input v-model="form.description" /></label>
        <label>Code <textarea v-model="form.code" rows="10" /></label>
        <button>{{ editingId ? 'Save' : 'Create' }}</button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { request } from '../api/http';

const bots = ref([]);
const editingId = ref(null);
const error = ref('');
const form = reactive({ title: '', description: '', code: 'return 1;' });

onMounted(loadBots);

async function loadBots() {
  bots.value = await request('/bots');
}

function startCreate() {
  editingId.value = null;
  form.title = '';
  form.description = '';
  form.code = 'return 1;';
}

function edit(bot) {
  editingId.value = bot.id;
  form.title = bot.title;
  form.description = bot.description;
  form.code = bot.code;
}

async function save() {
  error.value = '';
  try {
    await request(editingId.value ? `/bots/${editingId.value}` : '/bots', {
      method: editingId.value ? 'PUT' : 'POST',
      body: JSON.stringify(form)
    });
    startCreate();
    await loadBots();
  } catch (err) {
    error.value = err.message;
  }
}

async function remove(id) {
  await request(`/bots/${id}`, { method: 'DELETE' });
  await loadBots();
}
</script>
