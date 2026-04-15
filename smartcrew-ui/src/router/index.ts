import { createRouter, createWebHistory } from 'vue-router'
import { portalConfig } from '../config/portal'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('../layouts/PublicLayout.vue'),
      children: [
        {
          path: '',
          name: 'public-chat',
          component: () => import('../views/public/PublicChatView.vue')
        }
      ]
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('../views/admin/AdminLoginView.vue')
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('../views/admin/AdminDashboardView.vue')
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/AdminUsersView.vue')
        },
        {
          path: 'identities',
          name: 'admin-identities',
          component: () => import('../views/admin/AdminIdentitiesView.vue')
        },
        {
          path: 'agents',
          name: 'admin-agents',
          component: () => import('../views/admin/AdminAgentsView.vue')
        },
        {
          path: 'knowledge-bases',
          name: 'admin-knowledge-bases',
          component: () => import('../views/admin/AdminKnowledgeBasesView.vue')
        },
        {
          path: 'prompts',
          name: 'admin-prompts',
          component: () => import('../views/admin/AdminPromptsView.vue')
        },
        {
          path: 'preferences',
          name: 'admin-preferences',
          component: () => import('../views/admin/AdminPreferencesView.vue')
        },
        {
          path: 'conversations',
          name: 'admin-conversations',
          component: () => import('../views/admin/AdminConversationsView.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.path.startsWith('/admin')) {
    if (!portalConfig.enableAdmin) {
      return '/'
    }
    if (to.path !== '/admin/login' && !authStore.isAdminLoggedIn) {
      return '/admin/login'
    }
    if (to.path === '/admin/login' && authStore.isAdminLoggedIn) {
      return '/admin/dashboard'
    }
  }
  if (to.path === '/' && !portalConfig.enableWeb) {
    return portalConfig.enableAdmin ? '/admin/login' : false
  }
  return true
})

export default router
