import { useLoginUserStore } from '@/stores/loginUser'
import message from 'ant-design-vue/es/message'
import router from '@/router'
import { USER_ROLE_ADMIN } from '@/constants/user'

let loginUserFetchPromise: Promise<void> | undefined
let loginUserFetchTimer: number | undefined

const ensureLoginUser = (loginUserStore: ReturnType<typeof useLoginUserStore>) => {
  if (!loginUserFetchPromise) {
    loginUserFetchPromise = loginUserStore.fetchLoginUser().catch(() => undefined)
  }
  return loginUserFetchPromise
}

const scheduleLoginUserFetch = (loginUserStore: ReturnType<typeof useLoginUserStore>) => {
  if (loginUserFetchPromise || loginUserFetchTimer) return
  loginUserFetchTimer = window.setTimeout(() => {
    loginUserFetchTimer = undefined
    void ensureLoginUser(loginUserStore)
  }, 900)
}

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  const toUrl = to.fullPath
  if (to.meta.requiresAuth && !loginUserStore.loginUser.id) {
    next({
      path: '/user/login',
      query: { redirect: to.fullPath },
    })
    return
  }
  if (toUrl.startsWith('/admin')) {
    await ensureLoginUser(loginUserStore)
    const loginUser = loginUserStore.loginUser
    if (!loginUser || loginUser.userRole !== USER_ROLE_ADMIN) {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  } else {
    scheduleLoginUserFetch(loginUserStore)
  }
  next()
})
