<template>
  <main id="userLoginPage" class="auth-page">
    <section class="auth-visual">
      <div class="visual-water" aria-hidden="true" />
      <div class="visual-bridge" aria-hidden="true"><span /><span /><span /></div>
      <div class="visual-copy">
        <div class="visual-mark"><img src="@/assets/logo.png" alt="AI 文章创作器" /></div>
        <div class="eyebrow">A quiet place to write</div>
        <h1>把灵感，<br />安静地写下来。</h1>
        <p>从一个题目开始，沿着自己的节奏，完成一篇真正属于你的作品。</p>
        <div class="visual-note"><span /> 今日也可以只写一句。</div>
      </div>
    </section>

    <section class="auth-form-wrap">
      <div class="auth-form-card">
        <div class="form-kicker">WELCOME BACK</div>
        <h2>欢迎回来</h2>
        <p class="form-subtitle">登录你的创作空间，继续未完成的表达。</p>

        <a-form :model="formState" name="login" autocomplete="off" class="auth-form" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" size="large" placeholder="账号" class="form-input">
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>
          <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }, { min: 8, message: '密码不能少于 8 位' }]">
            <a-input-password v-model:value="formState.userPassword" size="large" placeholder="密码" class="form-input">
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>
          <a-button type="primary" html-type="submit" size="large" block class="submit-button" :loading="isSubmitting">
            进入创作空间 <ArrowRightOutlined />
          </a-button>
        </a-form>

        <div class="auth-footer">
          <span>还没有账号？</span>
          <RouterLink to="/user/register">创建一个账号</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { ArrowRightOutlined, LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { userLogin } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const formState = reactive<API.UserLoginRequest>({ userAccount: '', userPassword: '' })
const router = useRouter()
const loginUserStore = useLoginUserStore()
const isSubmitting = ref(false)

const handleSubmit = async (values: API.UserLoginRequest) => {
  isSubmitting.value = true
  try {
    const res = await userLogin(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      await router.replace('/')
    } else {
      message.error(`登录失败，${res.data.message}`)
    }
  } catch (error) {
    console.error('登录请求失败:', error)
    message.error('登录服务暂时不可用，请稍后重试')
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
.visual-water::before { top: 23%; transform: rotate(5deg); }
.visual-water::after { top: 54%; transform: rotate(-2deg); }
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
.form-kicker { color: var(--mountain-green); }
.auth-form-card h2 { margin: 18px 0 8px; color: var(--ink-deep); font-size: 38px; font-weight: 500; }
.form-subtitle { margin: 0 0 36px; color: var(--ink-muted); font-size: 14px; line-height: 1.7; }
.auth-form :deep(.ant-form-item) { margin-bottom: 18px; }
.form-input { border-radius: var(--radius-md); border-color: var(--line-soft); background: rgba(255,255,255,0.68); }
.form-input :deep(.ant-input), .form-input :deep(.ant-input-password) { padding-top: 12px; padding-bottom: 12px; }
.form-input :deep(.anticon) { color: var(--ink-muted); }
.submit-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; height: 50px; margin-top: 8px; border: 0; }
.auth-footer { margin-top: 28px; color: var(--ink-muted); font-size: 13px; text-align: center; }
.auth-footer a { margin-left: 6px; color: var(--mountain-green); font-weight: 700; }

@media (max-width: 800px) {
  .auth-page { grid-template-columns: 1fr; }
  .auth-visual { min-height: 420px; padding: 52px 32px; }
  .visual-copy { width: min(100%, 420px); }
  .visual-copy h1 { font-size: 3.4rem; }
  .visual-note { margin-top: 26px; }
  .auth-form-wrap { min-height: 520px; padding: 52px 24px 72px; }
}
</style>
