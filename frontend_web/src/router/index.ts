import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: Layout,
      redirect: '/landmark'
    },
    {
      path: '/landmark',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/LandmarkManagement.vue')
        },
        {
          path: 'create',
          component: () => import('../views/CreateLandmark.vue')
        }
      ]
    },
    {
      path: '/home',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/LandmarkManagement.vue')
        }
      ]
    },
    {
      path: '/feedback',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/LandmarkManagement.vue')
        }
      ]
    },
    {
      path: '/settings',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/LandmarkManagement.vue')
        }
      ]
    }
  ],
})

export default router
