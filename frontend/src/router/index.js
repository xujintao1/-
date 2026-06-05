import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', meta: { title: '销售看板' }, component: () => import('../views/Dashboard.vue') },
      { path: 'factory-units', name: 'factory-units', meta: { title: '房源管理' }, component: () => import('../views/FactoryUnits.vue') },
      { path: 'customers', name: 'customers', meta: { title: '客户管理' }, component: () => import('../views/Customers.vue') },
      { path: 'subscriptions', name: 'subscriptions', meta: { title: '认购管理' }, component: () => import('../views/Subscriptions.vue') },
      { path: 'contracts', name: 'contracts', meta: { title: '合同管理' }, component: () => import('../views/Contracts.vue') },
      { path: 'approvals', name: 'approvals', meta: { title: '审批待办' }, component: () => import('../views/Approvals.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && auth.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
