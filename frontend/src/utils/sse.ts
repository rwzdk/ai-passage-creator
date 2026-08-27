/**
 * SSE 宸ュ叿鍑芥暟
 */

export interface SSEMessage {
  type: string
  data?: any
  [key: string]: any
}

export interface SSEOptions {
  onMessage: (message: SSEMessage) => void
  onError?: (error: Event) => void
  onComplete?: () => void
}

/**
 * 寤虹珛 SSE 杩炴帴
 */
export const connectSSE = (taskId: string, options: SSEOptions): EventSource => {
  const { onMessage, onError, onComplete } = options
  let hasReportedError = false

  const eventSource = new EventSource(`/api/article/progress/${taskId}`)

  eventSource.onmessage = (event) => {
    try {
      const message: SSEMessage = JSON.parse(event.data)
      hasReportedError = false
      onMessage(message)
      
      // 妫€鏌ユ槸鍚﹀畬鎴?      if (message.type === 'ALL_COMPLETE' || message.type === 'ERROR') {
        eventSource.close()
        onComplete?.()
      }
    } catch (error) {
      console.error('SSE 娑堟伅瑙ｆ瀽澶辫触:', error)
    }
  }

  eventSource.onerror = (error) => {
    console.error('SSE 杩炴帴閿欒:', error)
    // EventSource 浼氳嚜鍔ㄩ噸杩烇紱鍙姤鍛婄涓€娆℃柇绾匡紝鐢遍〉闈㈠惎鍔ㄧ姸鎬佽疆璇㈠厹搴曘€?    if (!hasReportedError) {
      hasReportedError = true
      onError?.(error)
    }
  }

  return eventSource
}

/**
 * 鍏抽棴 SSE 杩炴帴
 */
export const closeSSE = (eventSource: EventSource | null) => {
  if (eventSource) {
    eventSource.close()
  }
}
