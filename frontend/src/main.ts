import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import zhCN from 'ant-design-vue/es/locale/zh_CN'
import 'ant-design-vue/dist/reset.css'
import 'dayjs/locale/zh-cn'

import '@/access'
import '@/styles/variables.css'
import '@/styles/common.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 全局配置 Ant Design 中文语言
app.provide('locale', zhCN)

app.mount('#app')
