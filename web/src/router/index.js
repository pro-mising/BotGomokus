import { createRouter, createWebHistory } from 'vue-router'
import IndexView from '../views/pk/IndexView.vue'
import NotFound from '../views/error/NotFound.vue'

const routes = [
  {
    path: '/',
    redirect: '/pk'
  },
  {
    path: '/pk',
    name: 'PK',
    component: IndexView
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
