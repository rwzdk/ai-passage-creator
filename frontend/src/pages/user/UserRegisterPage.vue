<template>
  <main id="userRegisterPage" class="auth-page register-page">
    <section class="auth-visual">
      <div class="visual-water" aria-hidden="true" />
      <div class="visual-bridge" aria-hidden="true"><span /><span /><span /></div>
      <div class="visual-copy">
        <div class="visual-mark"><img src="@/assets/logo.png" alt="AI 文章创作器" /></div>
        <div class="eyebrow">Begin your creative trace</div>
        <h1>给未来的自己，<br />留下一页文字。</h1>
        <p>创建一个专属空间，保存你的作品、数据与每一次正在发生的创作。</p>
        <div class="visual-note"><span /> 一页一页，写成自己的路径。</div>
      </div>
    </section>

    <section class="auth-form-wrap">
      <div class="auth-form-card">
        <div class="form-kicker">CREATE ACCOUNT</div>
        <h2>创建账号</h2>
        <p class="form-subtitle">注册后，开启你的 AI 创作旅程。</p>

        <a-form :model="formState" name="register" autocomplete="off" class="auth-form" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" size="large" placeholder="账号" class="form-input">
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>
          <div class="register-row">
            <a-form-item name="userName">
              <a-input v-model:value="formState.userName" size="large" placeholder="昵称（可选）" class="form-input"><template #prefix><UserOutlined /></template></a-input>
            </a-form-item>
            <a-form-item name="userEmail">
              <a-input v-model:value="formState.userEmail" size="large" placeholder="QQ 邮箱（可选）" class="form-input"><template #prefix><MailOutlined /></template></a-input>
            </a-form-item>
          </div>
          <a-form-item name="userPhone">
            <a-input v-model:value="formState.userPhone" size="large" placeholder="电话号码（可选）" class="form-input"><template #prefix><PhoneOutlined /></template></a-input>
          </a-form-item>
          <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }, { min: 8, message: '密码不能少于 8 位' }]">
            <a-input-password v-model:value="formState.userPassword" size="large" placeholder="密码（至少 8 位）" class="form-input"><template #prefix><LockOutlined /></template></a-input-password>
          </a-form-item>
          <a-form-item name="checkPassword" :rules="[{ required: true, message: '请确认密码' }, { min: 8, message: '密码不能少于 8 位' }, { validator: validateCheckPassword }]">
            <a-input-password v-model:value="formState.checkPassword" size="large" placeholder="确认密码" class="form-input"><template #prefix><SafetyOutlined /></template></a-input-password>
          </a-form-item>
          <a-button type="primary" html-type="submit" size="large" block class="submit-button" :loading="isSubmitting">
            创建我的空间 <ArrowRightOutlined />
          </a-button>
        </a-form>

        <div class="auth-footer"><span>已经有账号？</span><RouterLink to="/user/login">返回登录</RouterLink></div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { ArrowRightOutlined, LockOutlined, MailOutlined, PhoneOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons-vue'
import { userRegister } from '@/api/userController'

const router = useRouter()
const isSubmitting = ref(false)
const formState = reactive<API.UserRegisterRequest>({ userAccount: '', userPassword: '', checkPassword: '', userName: '', userEmail: '', userPhone: '' })

const validateCheckPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) callback(new Error('两次输入密码不一致'))
  else callback()
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
.auth-page { display: grid; grid-template-columns: 1.08fr 0.92fr; min-height: calc(100vh - 64px); background: var(--paper-warm); }
.auth-visual { position: relative; display: grid; place-items: center; overflow: hidden; padding: 72px 8vw; background: linear-gradient(150deg, #456f64, #203b38 72%); color: white; }
.visual-water { position: absolute; inset: 28% -12% -18%; border: 1px solid rgba(213,234,219,0.28); border-radius: 50% 50% 0 0; transform: rotate(-7deg); box-shadow: 0 -20px 120px rgba(143,184,164,0.16); }
.visual-water::before, .visual-water::after { position: absolute; left: 9%; right: 8%; height: 1px; background: rgba(213,234,219,0.22); content: ''; }
.visual-water::before { top: 23%; transform: rotate(5deg); }.visual-water::after { top: 54%; transform: rotate(-2deg); }
.visual-bridge { position: absolute; right: -2%; bottom: 18%; width: 48%; height: 32%; border-top: 2px solid rgba(247,245,238,0.24); transform: rotate(-10deg); }
.visual-bridge span { position: absolute; top: -2px; width: 2px; height: 100%; background: rgba(247,245,238,0.18); transform: rotate(8deg); }
.visual-bridge span:nth-child(1) { left: 32%; }.visual-bridge span:nth-child(2) { left: 58%; }.visual-bridge span:nth-child(3) { left: 82%; }
.visual-copy { position: relative; z-index: 1; max-width: 450px; }
.visual-mark { display: grid; place-items: center; width: 72px; height: 72px; margin-bottom: 32px; border: 1px solid rgba(255,255,255,0.32); border-radius: 22px; background: rgba(247,245,238,0.92); box-shadow: 0 14px 35px rgba(0,0,0,0.15); }
.visual-mark img { width: 56px; height: 56px; object-fit: contain; }
.eyebrow, .form-kicker { color: var(--river-green); font-size: 11px; font-weight: 700; letter-spacing: 0.16em; }
.visual-copy h1 { margin: 22px 0 20px; font-size: clamp(3rem, 5vw, 5.2rem); font-weight: 500; line-height: 1.02; }
.visual-copy p { max-width: 360px; margin: 0; color: rgba(243,247,243,0.74); font-size: 16px; line-height: 1.9; }
.visual-note { display: flex; align-items: center; gap: 10px; margin-top: 42px; color: rgba(243,247,243,0.82); font-size: 13px; }
.visual-note span { width: 8px; height: 8px; border-radius: 50%; background: var(--accent-gold); box-shadow: 0 0 0 5px rgba(199,168,120,0.18); }
.auth-form-wrap { display: grid; place-items: center; padding: 56px 7vw; background: rgba(247,245,238,0.92); }
.auth-form-card { width: min(100%, 390px); }
.register-page .visual-copy h1 { font-size: clamp(2.8rem, 4.2vw, 4.4rem); }
.form-kicker { color: var(--mountain-green); }.auth-form-card h2 { margin: 18px 0 8px; color: var(--ink-deep); font-size: 38px; font-weight: 500; }.form-subtitle { margin: 0 0 30px; color: var(--ink-muted); font-size: 14px; line-height: 1.7; }
.auth-form :deep(.ant-form-item) { margin-bottom: 16px; }.register-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }.register-row :deep(.ant-form-item) { min-width: 0; }
.form-input { border-radius: var(--radius-md); border-color: var(--line-soft); background: rgba(255,255,255,0.68); }.form-input :deep(.ant-input), .form-input :deep(.ant-input-password) { padding-top: 12px; padding-bottom: 12px; }.form-input :deep(.anticon) { color: var(--ink-muted); }
.submit-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; height: 50px; margin-top: 8px; border: 0; }.auth-footer { margin-top: 24px; color: var(--ink-muted); font-size: 13px; text-align: center; }.auth-footer a { margin-left: 6px; color: var(--mountain-green); font-weight: 700; }
@media (max-width: 800px) { .auth-page { grid-template-columns: 1fr; }.auth-visual { min-height: 420px; padding: 52px 32px; }.visual-copy { width: min(100%, 420px); }.visual-copy h1 { font-size: 3.4rem; }.visual-note { margin-top: 26px; }.auth-form-wrap { min-height: 580px; padding: 52px 24px 72px; } }
@media (max-width: 560px) { .register-row { grid-template-columns: 1fr; gap: 0; } }
</style>
