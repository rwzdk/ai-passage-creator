// @ts-ignore
/* eslint-disable */
import request from '@/request'

export async function getHotTopics(options?: { [key: string]: any }) {
  return request<API.BaseResponseHotTopicsVO>('/article/hot-topics', {
    method: 'GET',
    ...(options || {}),
  })
}
