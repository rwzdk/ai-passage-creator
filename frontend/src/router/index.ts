import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(_to, _from, savedPosition) {
    if (!_from.name) return false
    return savedPosition ? { ...savedPosition, behavior: 'auto' } : { top: 0, behavior: 'auto' }
  },
  routes: [
    {
      path: '/',
      name: '主页',
      component: () => import('@/pages/HomePage.vue'),
    },
    {
      path: '/create',
      name: '创作文章',
      component: () => import('@/pages/article/ArticleCreatePage.vue'),
    },
    {
      path: '/article/list',
      meta: { requiresAuth: true },
      name: '文章列表',
      component: () => import('@/pages/article/ArticleListPage.vue'),
    },
    {
      path: '/article/:taskId',
      name: '文章详情',
      component: () => import('@/pages/article/ArticleDetailPage.vue'),
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('@/pages/user/UserLoginPage.vue'),
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('@/pages/user/UserRegisterPage.vue'),
    },
    {
      path: '/profile',
      name: '个人资料',
      component: () => import('@/pages/user/UserProfilePage.vue'),
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: () => import('@/pages/admin/UserManagePage.vue'),
    },
    {
      path: '/admin/statistics',
      name: '数据分析',
      component: () => import('@/pages/admin/StatisticsPage.vue'),
    },
    {
      path: '/vip',
      name: '会员购买',
      component: () => import('@/pages/VipPage.vue'),
    },
  ],
})

export default router
