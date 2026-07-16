import { createApp, reactive } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import LoginView from './views/LoginView.vue';
import RegisterView from './views/RegisterView.vue';
import PlayView from './views/PlayView.vue';
import BotsView from './views/BotsView.vue';
import RankView from './views/RankView.vue';
import RecordsView from './views/RecordsView.vue';
import './styles.css';

export const store = reactive({
  token: localStorage.getItem('kob-demo-token') || '',
  user: JSON.parse(localStorage.getItem('kob-demo-user') || 'null'),
  setSession(token, user) {
    this.token = token;
    this.user = user;
    localStorage.setItem('kob-demo-token', token);
    localStorage.setItem('kob-demo-user', JSON.stringify(user));
  },
  logout() {
    this.token = '';
    this.user = null;
    localStorage.removeItem('kob-demo-token');
    localStorage.removeItem('kob-demo-user');
  }
});

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/play' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/register', component: RegisterView, meta: { public: true } },
    { path: '/play', component: PlayView },
    { path: '/bots', component: BotsView },
    { path: '/rank', component: RankView },
    { path: '/records', component: RecordsView }
  ]
});

router.beforeEach((to) => {
  if (!to.meta.public && !store.token) return '/login';
});

createApp(App).use(router).mount('#app');
