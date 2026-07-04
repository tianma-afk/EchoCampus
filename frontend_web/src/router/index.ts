import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      component: () => import('../views/Login.vue'),
    },
    {
      path: '/',
      component: Layout,
      redirect: '/home',
    },
    {
      path: '/landmark',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/LandmarkManagement.vue'),
        },
        {
          path: 'create',
          component: () => import('../views/CreateLandmark.vue'),
        },
        {
          path: ':id',
          component: () => import('../views/LandmarkDetail.vue'),
        },
        {
          path: ':id/edit',
          component: () => import('../views/EditLandmark.vue'),
        },
        {
          path: ':id/images',
          component: () => import('../views/ManageImages.vue'),
        },
      ],
    },
    {
      path: '/universities',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/university/UniversityManagement.vue'),
        },
        {
          path: ':id',
          component: () => import('../views/university/UniversityDetail.vue'),
        },
      ],
    },
    {
      path: '/home',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/Home.vue'),
        },
      ],
    },
    {
      path: '/feedback',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/feedback/FeedbackList.vue'),
        },
        {
          path: ':id',
          component: () => import('../views/feedback/FeedbackDetail.vue'),
        },
      ],
    },
    {
      path: '/tasks',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/TaskManagement.vue'),
        },
      ],
    },
    {
      path: '/admins',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/AdminManagement.vue'),
        },
      ],
    },
    {
      path: '/cleanup',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/ImageCleanup.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      component: Layout,
      children: [
        {
          path: '',
          component: () => import('../views/NotFound.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('echocampus_token')
  if (!token && to.path !== '/login') {
    return '/login'
  }
  if (token && to.path === '/login') {
    return '/home'
  }
})

export default router
