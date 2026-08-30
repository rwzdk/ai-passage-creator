import { useLoginUserStore } from '@/stores/loginUser'
import message from 'ant-design-vue/es/message'
import router from '@/router'
import { USER_ROLE_ADMIN } from '@/constants/user'

let loginUserFetchPromise: Promise<void> | undefined

const ensureLoginUser = (loginUserStore: ReturnType<typeof useLoginUserStore>) => {
  if (loginUserStore.loginUser.id) return Promise.resolve()
  if (!loginUserFetchPromise) {
    loginUserFetchPromise = loginUserStore.fetchLoginUser().catch(() => undefined)
  }
  return loginUserFetchPromise
}

const scheduleLoginUserFetch = (loginUserStore: ReturnType<typeof useLoginUserStore>) => {
  void ensureLoginUser(loginUserStore)
}

/**
 * 全局权限校验
 */
router.beforeEach(async (to, _from, next) => {
  const loginUserStore = useLoginUserStore()
  const toUrl = to.fullPath
  const isAdminRoute = toUrl.startsWith('/admin')

  if (to.meta.requiresAuth || isAdminRoute) {
    if (!loginUserStore.loginUser.id) {
      await ensureLoginUser(loginUserStore)
    }

    const loginUser = loginUserStore.loginUser
    if (!loginUser.id) {
      next({
        path: '/user/login',
        query: { redirect: to.fullPath },
      })
      return
    }

    if (isAdminRoute && loginUser.userRole !== USER_ROLE_ADMIN) {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  } else {
    if (!to.path.startsWith('/user/login') && !to.path.startsWith('/user/register')) {
      scheduleLoginUserFetch(loginUserStore)
    }
  }
  next()
})
