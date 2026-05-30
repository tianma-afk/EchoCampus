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
        },
        {
          path: ':id',
          component: () => import('../views/LandmarkDetail.vue')
        },
        {
          path: ':id/edit',
          component: () => import('../views/EditLandmark.vue')
        },
        {
          path: ':id/images',
          component: () => import('../views/ManageImages.vue')
        }
      ]
    },
    {
      path: '/universities',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/university/UniversityManagement.vue')
        },
        {
          path: ':id',
          component: () => import('../views/university/UniversityDetail.vue')
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
      path: '/tasks',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/TaskManagement.vue')
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
