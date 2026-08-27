<template>
  <header class="header">
    <div class="header-container">
      <div class="header-left">
        <RouterLink to="/" class="logo-link">
          <div class="logo-wrapper">
            <img src="@/assets/logo-optimized.webp" alt="Logo" class="logo-img" width="64" height="64" />
            <h1 class="site-title">沅笺</h1>
          </div>
        </RouterLink>
      </div>

      <!-- 中间：导航菜单 -->
      <nav class="nav-center">
        <RouterLink
          v-for="item in menuItems"
          :key="item.key"
          :to="item.key"
          :class="['nav-item', { active: selectedKeys.includes(item.key) }]"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 右侧：用户操作区域 -->
      <div class="header-right">
        <div v-if="loginUserStore.loginUser.id" class="user-dropdown">
          <!-- VIP 标识 -->
          <RouterLink v-if="!isVip" to="/vip" class="upgrade-vip-btn">
            <CrownOutlined />
            <span>升级 VIP</span>
          </RouterLink>
          <RouterLink v-else to="/vip" class="vip-badge">
            <CrownOutlined />
            <span>VIP</span>
          </RouterLink>

          <a-dropdown>
            <a-space class="user-info">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="36" class="user-avatar" />
              <span class="user-name">
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </span>
            </a-space>
            <template #overlay>
              <a-menu class="dropdown-menu">
              <a-menu-item v-if="isVip" key="vip-info" class="vip-info-item" @click="router.push('/vip')">
                  <CrownOutlined />
                  <span>永久会员权益</span>
              </a-menu-item>
              <a-menu-divider v-if="isVip" />
                <a-menu-item key="profile" class="dropdown-item" @click="router.push('/profile')">
                  <UserOutlined />
                  <span>个人资料</span>
                </a-menu-item>
                <a-menu-item @click="doLogout" class="dropdown-item">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <RouterLink to="/user/login" class="login-btn">登录</RouterLink>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import message from 'ant-design-vue/es/message'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import {
  LogoutOutlined,
  HomeOutlined,
  EditOutlined,
  UnorderedListOutlined,
  SettingOutlined,
  CrownOutlined,
  BarChartOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { isVip as checkIsVip } from '@/utils/permission'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 判断是否为 VIP（管理员也视为 VIP）
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: HomeOutlined,
    label: '首页',
  },
  {
    key: '/create',
    icon: EditOutlined,
    label: '创作',
  },
  {
    key: '/article/list',
    icon: UnorderedListOutlined,
    label: '历史',
  },
  {
    key: '/admin/userManage',
    icon: SettingOutlined,
    label: '管理',
    admin: true,
  },
  {
    key: '/admin/statistics',
    icon: BarChartOutlined,
    label: '数据',
    admin: true,
  },
]

// 过滤菜单项
const menuItems = computed(() => {
  return originItems.filter((item) => {
    if (item.admin) {
      const loginUser = loginUserStore.loginUser
      return loginUser && loginUser.userRole === 'admin'
    }
    return true
  })
})

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 12px;
  z-index: 100;
  height: 64px;
  margin-bottom: -64px;
  padding: 0;
  line-height: normal;
  background: transparent;
  border: 0;
  overflow: visible;
  transition: transform var(--transition-normal);
}

.header-container {
  position: relative;
  max-width: 1160px;
  height: 64px;
  margin: 0 auto;
  padding: 0 14px 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 20px;
  background: linear-gradient(105deg, rgba(247, 250, 246, 0.88), rgba(224, 236, 229, 0.8));
  box-shadow: 0 14px 32px rgba(32, 59, 56, 0.11), inset 0 1px 0 rgba(255, 255, 255, 0.72), inset 0 -1px 0 rgba(69, 111, 100, 0.08);
}

.header-container::before {
  position: absolute;
  top: 0;
  left: 9%;
  right: 9%;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.96), transparent);
  pointer-events: none;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: block;
  transition: opacity var(--transition-fast);
}

.logo-link:hover {
  opacity: 1;
  filter: drop-shadow(0 0 12px rgba(143, 184, 164, 0.36));
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  filter: drop-shadow(0 0 8px rgba(143, 184, 164, 0.3));
}

.site-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink-deep);
  white-space: nowrap;
  letter-spacing: -0.3px;
}

/* 导航菜单 */
.nav-center {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px;
  border: 1px solid rgba(69, 111, 100, 0.12);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.34);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  border: 1px solid transparent;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.nav-item:hover {
  color: var(--ink-deep);
  border-color: rgba(69, 111, 100, 0.13);
  background: rgba(143, 184, 164, 0.18);
  box-shadow: 0 0 16px rgba(143, 184, 164, 0.16);
}

.nav-item.active {
  color: var(--ink-deep);
  border-color: rgba(69, 111, 100, 0.18);
  background: linear-gradient(135deg, rgba(143, 184, 164, 0.42), rgba(247, 250, 246, 0.56));
  box-shadow: 0 0 16px rgba(143, 184, 164, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.56);
}

.nav-item.active::after {
  position: absolute;
  right: 18px;
  bottom: 3px;
  left: 18px;
  height: 2px;
  content: '';
  border-radius: var(--radius-full);
  background: var(--river-green);
  box-shadow: 0 0 10px rgba(143, 184, 164, 0.8);
}

.nav-icon {
  font-size: 16px;
  color: var(--river-green);
}

/* 用户区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-dropdown {
  cursor: pointer;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.upgrade-vip-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  background: transparent;
  color: var(--accent-gold);
  text-decoration: none;
  transition: all var(--transition-fast);

  &:hover {
    background: rgba(199, 168, 120, 0.16);
    color: #e0c79d;
  }

  .anticon {
    font-size: 13px;
  }
}

.vip-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--accent-gold);
  text-decoration: none;
  transition: all var(--transition-fast);

  &:hover {
    color: #e0c79d;
  }

  .anticon {
    font-size: 13px;
  }
}

.user-info {
  padding: 6px 12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
}

.user-info:hover {
  background: rgba(143, 184, 164, 0.18);
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.86);
  box-shadow: 0 0 0 3px rgba(143, 184, 164, 0.16), 0 0 14px rgba(143, 184, 164, 0.16);
}

.user-name {
  font-weight: 500;
  color: var(--ink-deep);
  font-size: 14px;
}

.login-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 24px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: white;
  background: rgba(69, 111, 100, 0.9);
  border: 1px solid rgba(69, 111, 100, 0.2);
  box-shadow: 0 6px 16px rgba(69, 111, 100, 0.18);
  transition: all var(--transition-normal);
  text-decoration: none;
}

.login-btn:hover {
  color: white;
  background: var(--mountain-green);
  box-shadow: 0 8px 18px rgba(69, 111, 100, 0.24);
}

.dropdown-menu {
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(69, 111, 100, 0.18);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  transition: all var(--transition-fast);
}

.dropdown-item:hover {
  background: var(--color-background-secondary);
}

.vip-info-item {
  color: var(--color-primary-dark);
  background: rgba(34, 197, 94, 0.1);
  font-weight: 600;
  cursor: default;

  &:hover {
    background: rgba(34, 197, 94, 0.15);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .header {
    top: 8px;
    height: 56px;
    margin-bottom: -56px;
  }

  .header-container {
    height: 56px;
    margin: 0 12px;
    padding: 0 10px;
    border-radius: 17px;
  }

  .site-title {
    display: none;
  }

  .nav-item span {
    display: none;
  }

  .nav-item {
    padding: 8px 10px;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 520px) {
  .header-container {
    margin: 0 8px;
  }

  .nav-center {
    gap: 1px;
    padding: 3px;
  }

  .nav-item {
    padding: 8px;
  }

  .header-right {
    gap: 4px;
  }

  .upgrade-vip-btn,
  .vip-badge {
    padding: 6px 8px;
  }

  .upgrade-vip-btn span,
  .vip-badge span {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .header,
  .logo-link,
  .nav-item,
  .upgrade-vip-btn,
  .vip-badge,
  .user-info,
  .login-btn {
    transition: none;
  }
}
</style>
