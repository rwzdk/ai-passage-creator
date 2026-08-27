import axios from 'axios'
import message from 'ant-design-vue/es/message'
import { API_BASE_URL } from '@/config/env'
import { REQUEST_TIMEOUT, UNAUTHORIZED_CODE } from '@/constants'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === UNAUTHORIZED_CODE) {
      // 不是获取用户信息的请求，并且用户目前不是已经在用户登录页面，则跳转到登录页面
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }

    // 业务接口使用 HTTP 200 返回错误码；统一转成 rejected Promise，
    // 让调用方的 catch 能恢复页面状态并展示后端真实错误。
    if (data?.code !== undefined && data.code !== 0) {
      const error = new Error(data.message || '请求失败') as Error & {
        response?: typeof response
      }
      error.response = response
      return Promise.reject(error)
    }

    return response
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error)
  },
)

export default myAxios
