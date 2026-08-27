<template>
  <main id="userRegisterPage" class="auth-page register-page">
    <section class="auth-visual">
      <div class="visual-copy">
        <div class="visual-mark"><img src="@/assets/logo.webp" alt="AI 文章创作器" width="64" height="64" /></div>
        <div class="eyebrow">A PLACE FOR YOUR WORDS</div>
        <h1>让每个灵感，<br />都有落笔之处</h1>
        <p>注册一个专属空间，用 AI 把零散的想法写成文章，保存每一次创作，也留住一路走来的文字</p>
        <div class="visual-note"><span /> 从一行字开始，慢慢写成自己的作品</div>
      </div>
    </section>

    <section class="auth-form-wrap">
      <div class="auth-form-card">
        <div class="form-kicker">A SPACE TO CREATE</div>
        <h2>为灵感留一席之地</h2>
        <p class="form-subtitle">注册账号，开启 AI 创作，保存作品与创作轨迹</p>

        <a-form :model="formState" name="register" autocomplete="off" class="auth-form" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" size="large" placeholder="账号" class="form-input">
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="userName">
            <a-input v-model:value="formState.userName" size="large" placeholder="昵称（可选）" class="form-input"><template #prefix><UserOutlined /></template></a-input>
          </a-form-item>
          <a-form-item name="userEmail" :rules="[{ required: true, message: '请输入 QQ 邮箱' }, { pattern: /^[A-Za-z0-9._%+-]+@qq\.com$/i, message: '请输入有效的 QQ 邮箱' }]">
            <a-input v-model:value="formState.userEmail" size="large" placeholder="QQ 邮箱" class="form-input"><template #prefix><MailOutlined /></template></a-input>
          </a-form-item>
          <a-form-item name="verificationCode" :rules="[{ required: true, message: '请输入邮箱验证码' }, { len: 6, message: '验证码为 6 位数字' }]">
            <a-input v-model:value="formState.verificationCode" size="large" placeholder="邮箱验证码" class="form-input">
              <template #prefix><SafetyOutlined /></template>
              <template #suffix><a-button type="link" size="small" :disabled="countdown > 0 || isSendingCode" @click="sendCode">{{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}</a-button></template>
            </a-input>
          </a-form-item>
          <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }, { min: 8, message: '密码不能少于 8 位' }]">
            <a-input-password v-model:value="formState.userPassword" size="large" placeholder="密码（至少 8 位）" class="form-input"><template #prefix><LockOutlined /></template></a-input-password>
          </a-form-item>
          <a-form-item name="checkPassword" :rules="[{ required: true, message: '请确认密码' }, { min: 8, message: '密码不能少于 8 位' }, { validator: validateCheckPassword }]">
            <a-input-password v-model:value="formState.checkPassword" size="large" placeholder="确认密码" class="form-input"><template #prefix><SafetyOutlined /></template></a-input-password>
          </a-form-item>
          <a-button type="primary" html-type="submit" size="large" block class="submit-button" :loading="isSubmitting">
            进入我的创作空间 <ArrowRightOutlined />
          </a-button>
        </a-form>

        <div class="auth-footer"><span>已有账号？</span><RouterLink to="/user/login">立即登录</RouterLink></div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { ArrowRightOutlined, LockOutlined, MailOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons-vue'
import { sendRegistrationEmailCode, userRegister } from '@/api/userController'

const router = useRouter()
const isSubmitting = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)
const formState = reactive<API.UserRegisterRequest>({ userAccount: '', userPassword: '', checkPassword: '', userName: '', userEmail: '', verificationCode: '' })

const validateCheckPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) callback(new Error('两次输入密码不一致'))
  else callback()
}

const sendCode = async () => {
  const email = formState.userEmail?.trim() || ''
  if (!/^[A-Za-z0-9._%+-]+@qq\.com$/i.test(email)) {
    message.error('请输入有效的 QQ 邮箱')
    return
  }
  isSendingCode.value = true
  try {
    const res = await sendRegistrationEmailCode({ userEmail: email })
    if (res.data.code !== 0) {
      message.error(`验证码发送失败，${res.data.message}`)
      return
    }
    message.success('验证码已发送，请查收邮箱')
    countdown.value = 60
    const timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) window.clearInterval(timer)
    }, 1000)
  } catch (error) {
    console.error('验证码发送失败:', error)
    message.error('验证码发送失败，请稍后重试')
  } finally {
    isSendingCode.value = false
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  isSubmitting.value = true
  try {
    const res = await userRegister(values)
    if (res.data.code === 0) {
      message.success('注册成功')
      await router.replace('/user/login')
    } else {
      message.error(`注册失败，${res.data.message}`)
    }
  } catch (error) {
    console.error('注册请求失败:', error)
    message.error('注册服务暂时不可用，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.auth-page { display: grid; grid-template-columns: 1.08fr 0.92fr; min-height: 100vh; background: var(--paper-warm); }
.auth-visual { position: relative; display: grid; place-items: center; overflow: hidden; padding: 72px 8vw; background: linear-gradient(150deg, #456f64, #203b38 72%); color: white; }
.visual-copy { position: relative; z-index: 1; max-width: 450px; }
.visual-mark { display: grid; place-items: center; width: 72px; height: 72px; margin-bottom: 32px; border: 1px solid rgba(255,255,255,0.32); border-radius: 22px; background: rgba(247,245,238,0.92); box-shadow: 0 14px 35px rgba(0,0,0,0.15); }
.visual-mark img { width: 56px; height: 56px; object-fit: contain; }
.eyebrow, .form-kicker { color: var(--river-green); font-size: 11px; font-weight: 700; letter-spacing: 0.16em; }
.visual-copy h1 { max-width: 100%; margin: 22px 0 20px; font-family: var(--font-display); font-size: clamp(2.8rem, 4.4vw, 4.6rem); font-weight: 500; line-height: 1.08; letter-spacing: 0; }
.visual-copy p { max-width: 390px; margin: 0; color: rgba(243,247,243,0.74); font-size: 16px; line-height: 1.85; }
.visual-note { display: flex; align-items: center; gap: 10px; margin-top: 42px; color: rgba(243,247,243,0.82); font-size: 13px; }
.visual-note span { width: 8px; height: 8px; border-radius: 50%; background: var(--accent-gold); box-shadow: 0 0 0 5px rgba(199,168,120,0.18); }
.auth-form-wrap { display: grid; place-items: center; padding: 56px 7vw; background: rgba(247,245,238,0.92); }
.auth-form-card { width: min(100%, 420px); }
.register-page .visual-copy h1 { font-size: clamp(2.8rem, 4.2vw, 4.4rem); }
.form-kicker { color: var(--mountain-green); }.auth-form-card h2 { margin: 14px 0 10px; color: var(--ink-deep); font-family: var(--font-display); font-size: clamp(2.25rem, 3vw, 2.75rem); font-weight: 500; line-height: 1.15; }.form-subtitle { max-width: 30em; margin: 0 0 28px; color: var(--ink-muted); font-size: 14px; line-height: 1.7; }
.auth-form :deep(.ant-form-item) { margin-bottom: 16px; }.register-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }.register-row :deep(.ant-form-item) { min-width: 0; }
.form-input { border-radius: var(--radius-md); border-color: var(--line-soft); background: rgba(255,255,255,0.68); }.form-input :deep(.ant-input), .form-input :deep(.ant-input-password) { padding-top: 12px; padding-bottom: 12px; }.form-input :deep(.anticon) { color: var(--ink-muted); }
.submit-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; height: 50px; margin-top: 8px; border: 0; }.auth-footer { margin-top: 24px; color: var(--ink-muted); font-size: 13px; text-align: center; }.auth-footer a { margin-left: 6px; color: var(--mountain-green); font-weight: 700; }
@media (max-width: 800px) { .auth-page { grid-template-columns: 1fr; }.auth-visual { min-height: 420px; padding: 52px 32px; }.visual-copy { width: min(100%, 420px); }.visual-copy h1 { max-width: 100%; font-size: 3.4rem; }.visual-note { margin-top: 26px; }.auth-form-wrap { min-height: 580px; padding: 52px 24px 72px; } }
@media (max-width: 560px) { .register-row { grid-template-columns: 1fr; gap: 0; } }
/* 渡口黄昏背景 */
.auth-page .auth-visual {
  background-image:
    linear-gradient(145deg, rgba(32, 59, 56, .58), rgba(32, 59, 56, .78)),
    url('@/assets/scenes/auth-dusk-bridge.webp');
  background-position: center;
  background-size: cover;
}
</style>
