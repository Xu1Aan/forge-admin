<template>
  <div class="login-container">
    <!-- Animated background -->
    <div class="login-bg-animated">
      <div class="gradient-orb orb-1" />
      <div class="gradient-orb orb-2" />
      <div class="gradient-orb orb-3" />
      <div class="grid-pattern" />
    </div>

    <!-- Login card with glassmorphism -->
    <div class="login-card">
      <!-- Left side - Branding -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="logo-wrapper">
            <img src="@/assets/images/logo.png" class="logo-img" alt="Logo">
          </div>
          <h1 class="brand-title">
            {{ title }}
          </h1>
          <p class="brand-subtitle">
            企业级中后台基础框架
          </p>
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">
                <i class="ai-icon:shield" />
              </div>
              <div class="feature-text">
                <span class="feature-title">安全可靠</span>
                <span class="feature-desc">企业级安全架构</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <i class="ai-icon:zap" />
              </div>
              <div class="feature-text">
                <span class="feature-title">高效便捷</span>
                <span class="feature-desc">快速开发部署</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <i class="ai-icon:layers" />
              </div>
              <div class="feature-text">
                <span class="feature-title">功能强大</span>
                <span class="feature-desc">插件化架构设计</span>
              </div>
            </div>
          </div>
        </div>
        <!-- Decorative elements -->
        <div class="brand-decoration">
          <div class="deco-circle deco-1" />
          <div class="deco-circle deco-2" />
          <div class="deco-circle deco-3" />
        </div>
      </div>

      <!-- Right side - Login form -->
      <div class="login-form-wrapper">
        <div class="login-form">
          <div class="form-header">
            <h2 class="form-title">
              欢迎回来
            </h2>
            <p class="form-subtitle">
              请登录您的账户
            </p>
          </div>

          <div class="form-body">
            <!-- Username -->
            <div class="form-group">
              <label for="username" class="form-label">用户名</label>
              <div class="input-wrapper">
                <n-input
                  id="username"
                  v-model:value="loginInfo.username"
                  autofocus
                  class="modern-input"
                  placeholder="请输入用户名"
                  :maxlength="20"
                  size="large"
                >
                  <template #prefix>
                    <i class="input-icon ai-icon:user" />
                  </template>
                </n-input>
              </div>
            </div>

            <!-- Password -->
            <div class="form-group">
              <label for="password" class="form-label">密码</label>
              <div class="input-wrapper">
                <n-input
                  id="password"
                  v-model:value="loginInfo.password"
                  class="modern-input"
                  type="password"
                  show-password-on="click"
                  placeholder="请输入密码"
                  :maxlength="20"
                  size="large"
                  @keydown.enter="handleLogin()"
                >
                  <template #prefix>
                    <i class="input-icon ai-icon:lock" />
                  </template>
                </n-input>
              </div>
            </div>

            <!-- Remember me -->
            <div class="form-options">
              <n-checkbox
                :checked="isRemember"
                :on-update:checked="(val) => (isRemember = val)"
              >
                <span class="checkbox-label">记住我</span>
              </n-checkbox>
            </div>

            <!-- Submit button -->
            <n-button
              class="login-button"
              type="primary"
              size="large"
              :loading="loading"
              block
              @click="handleLogin()"
            >
              <span class="button-text">登录</span>
              <i v-if="!loading" class="button-icon ai-icon:arrow-right" />
            </n-button>

            <!-- Social/SSO login buttons -->
            <div v-if="showOtherLogin" class="social-login-section">
              <div class="social-divider">
                <span class="divider-text">其他登录方式</span>
              </div>
              <div class="social-buttons">
                <button
                  v-for="platform in socialPlatforms"
                  :key="platform.platform"
                  class="social-button"
                  :title="platform.platformName"
                  @click="handleSocialLogin(platform.platform)"
                >
                  <img
                    v-if="platform.platformLogoBase64"
                    :src="`data:image/png;base64,${platform.platformLogoBase64}`"
                    :alt="platform.platformName"
                    class="social-icon"
                  >
                  <i v-else class="social-icon-fallback ai-icon:link" />
                </button>

                <!-- Weaver SSO：整页跳转至后端 /sso/weaver/authorize，非弹窗 -->
                <button
                  type="button"
                  class="social-button"
                  title="泛微SSO登录"
                  @click="handleWeaverLogin()"
                >
                  <img
                    class="social-icon"
                    :src="fanweiLogo"
                    alt="泛微SSO登录"
                  >
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useStorage } from '@vueuse/core'
import mainApi from '@/api'
import { useAuthStore, usePermissionStore, useUserStore } from '@/store'
import { lStorage } from '@/utils'
import { encryptPassword, initKeyExchange } from '@/utils/crypto/key-exchange'
import { request } from '@/utils/http'
import api from './api'
import fanweiLogo from '@/assets/images/fanwei.png'

const authStore = useAuthStore()
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const title = import.meta.env.VITE_TITLE

const loginInfo = ref({
  username: '',
  password: '',
})

const localLoginInfo = lStorage.get('loginInfo')
if (localLoginInfo) {
  loginInfo.value.username = localLoginInfo.username || ''
  loginInfo.value.password = localLoginInfo.password || ''
}

const isRemember = useStorage('isRemember', true)
const loading = ref(false)

// 三方登录平台列表
const socialPlatforms = ref([])
const socialLoading = ref(false)
const weaverSsoEnabled = ref(import.meta.env.VITE_WEAVER_SSO_ENABLED !== 'false')
const showOtherLogin = computed(() => socialPlatforms.value.length > 0 || weaverSsoEnabled.value)

// 获取已启用的三方登录平台
async function loadSocialPlatforms() {
  try {
    socialLoading.value = true
    const res = await api.getSocialPlatforms()
    if (res.code === 200 && res.data) {
      socialPlatforms.value = res.data.filter(p => p.enabled)
    }
  }
  catch (error) {
    console.error('获取三方登录平台失败:', error)
  }
  finally {
    socialLoading.value = false
  }
}

// 处理三方登录
async function handleSocialLogin(platform) {
  try {
    const res = await api.getSocialAuthUrl(platform)
    if (res.code === 200 && res.data) {
      // 打开授权窗口
      const width = 600
      const height = 500
      const left = (window.innerWidth - width) / 2
      const top = (window.innerHeight - height) / 2

      const authWindow = window.open(
        res.data.authUrl,
        'social_auth',
        `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no,resizable=yes`,
      )

      // 监听授权窗口关闭
      const checkClosed = setInterval(() => {
        if (authWindow.closed) {
          clearInterval(checkClosed)
        }
      }, 500)
    }
    else {
      $message.error(res.msg || '获取授权链接失败')
    }
  }
  catch (error) {
    console.error('获取三方授权链接失败:', error)
    $message.error('获取授权链接失败')
  }
}

function buildAuthorizeUrl(path, params = {}) {
  const prefix = import.meta.env.VITE_REQUEST_PREFIX || ''
  const p = prefix.endsWith('/') ? prefix.slice(0, -1) : prefix
  const fullPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${window.location.origin}${p}${fullPath}`)
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '')
      url.searchParams.set(k, String(v))
  })
  return url.toString()
}

// Weaver SSO 登录（后端负责跳转到 OA）
function handleWeaverLogin() {
  if (!weaverSsoEnabled.value) {
    $message.warning('泛微SSO未启用')
    return
  }

  // redirect_uri 需在后端配置为前端回调页：/#/login/weaver-callback
  const authUrl = buildAuthorizeUrl('/sso/weaver/authorize', {
    tenantId: route.query.tenantId,
    userClient: 'pc',
  })

  // 泛微登录应为整页跳转（避免浏览器拦截弹窗，也更符合 SSO 体验）
  window.location.href = authUrl
}

async function handleLogin() {
  const { username, password } = loginInfo.value

  if (!username || !password)
    return $message.warning('请输入用户名和密码')

  try {
    loading.value = true
    $message.loading('正在验证，请稍后...', { key: 'login' })

    const encryptedPassword = await encryptPassword(password, request)

    const params = {
      username,
      password: encryptedPassword,
      authType: 'password',
      encrypted: true,
      userClient: 'pc',
      appId: import.meta.env.VITE_APP_ID || 'forge_pc_001',
      appSecret: import.meta.env.VITE_APP_SECRET || undefined,
    }

    const res = await api.login(params)

    if (res.code === 200) {
      if (isRemember.value) {
        lStorage.set('loginInfo', { username, password })
      }
      else {
        lStorage.remove('loginInfo')
      }
      onLoginSuccess(res.data)
    }
    else {
      $message.destroy('login')
    }
  }
  catch (error) {
    $message.destroy('login')
    console.error(error)
  }
  loading.value = false
}

async function onLoginSuccess(data = {}) {
  // 设置认证信息 - LoginResult 结构
  if (data.accessToken) {
    authStore.setToken({
      accessToken: data.accessToken,
      tokenType: data.tokenType || 'Bearer',
      expiresIn: data.expiresIn,
    })

    // 密钥交换 - 使用 token 作为会话标识
    try {
      await initKeyExchange(request, data.accessToken)
    }
    catch (error) {
      console.warn('密钥交换失败，将使用降级方案:', error)
    }
  }

  // 如果返回了用户信息，设置到用户存储中
  if (data.userInfo) {
    const loginUser = data.userInfo
    userStore.setUser({
      id: loginUser.userId,
      username: loginUser.username,
      nickName: loginUser.realName || loginUser.username,
      email: loginUser.email,
      phone: loginUser.phone,
      avatar: loginUser.avatar,
      userType: loginUser.userType,
      userStatus: loginUser.userStatus,
      tenantId: loginUser.tenantId,
      roleIds: loginUser.roleIds || [],
      roleKeys: loginUser.roleKeys || [],
      permissions: loginUser.permissions || [],
      apiPermissions: loginUser.apiPermissions || [],
      orgIds: loginUser.orgIds || [],
      mainOrgId: loginUser.mainOrgId,
      roles: loginUser.roleKeys ? Array.from(loginUser.roleKeys) : [],
      userInfo: loginUser,
    })

    // 同时存储到localStorage用于持久化
    lStorage.set('userInfo', loginUser)
  }

  $message.loading('登录中...', { key: 'login' })
  try {
    // 先获取菜单数据，再跳转
    await loadAndSetMenuData()

    $message.success('登录成功', { key: 'login' })
    // 使用环境变量中的默认跳转路径
    const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'

    // 处理重定向
    const redirectPath = route.query.redirect
    if (redirectPath && redirectPath !== '/login') {
      // 如果 redirect 不是登录页，则跳转到 redirect
      delete route.query.redirect
      router.push({ path: redirectPath, query: route.query })
    }
    else {
      // 否则跳转到首页
      router.push(defaultRedirectPath)
    }
  }
  catch (error) {
    console.error(error)
    $message.destroy('login')
  }
}

// 监听三方登录子窗口的消息
async function handleSocialLoginMessage(event) {
  if (event.data?.type === 'SOCIAL_LOGIN_SUCCESS') {
    const { data } = event.data

    // 设置 token
    if (data?.accessToken) {
      authStore.setToken({
        accessToken: data.accessToken,
        tokenType: data.tokenType || 'Bearer',
        expiresIn: data.expiresIn,
      })
    }

    $message.success('登录成功')

    // 使用 window.location.href 强制刷新页面跳转
    const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'
    window.location.href = defaultRedirectPath
  }
  else if (event.data?.type === 'SOCIAL_LOGIN_FAILED') {
    $message.error('三方登录失败，请重试')
  }
}

// 页面加载：三方登录等
onMounted(() => {
  loadSocialPlatforms()
  // 监听三方登录消息
  window.addEventListener('message', handleSocialLoginMessage)

  // 兼容泛微回跳两种形式：
  // 1) /#/login?ticket=xxx
  // 2) /?ticket=xxx#/login?redirect=/home
  const callbackQuery = resolveWeaverCallbackQuery()
  if (callbackQuery.ticket || callbackQuery.code) {
    handleWeaverTicketLogin(callbackQuery)
  }
})

onUnmounted(() => {
  window.removeEventListener('message', handleSocialLoginMessage)
})

function resolveWeaverCallbackQuery() {
  const hashQuery = { ...route.query }
  const searchQuery = Object.fromEntries(new URLSearchParams(window.location.search).entries())
  const merged = {
    ...searchQuery,
    ...hashQuery,
  }
  // 票据以 search 为准（当前实际回跳为 /?ticket=...#/login）
  if (searchQuery.ticket)
    merged.ticket = searchQuery.ticket
  if (searchQuery.code)
    merged.code = searchQuery.code
  return merged
}

function resolveAppTargetPath(rawPath) {
  if (!rawPath || rawPath === '/login')
    return import.meta.env.VITE_HOME_PATH || '/'
  return String(rawPath).startsWith('/') ? String(rawPath) : `/${String(rawPath)}`
}

function buildAppUrl(path) {
  return router.resolve({ path }).href
}

async function handleWeaverTicketLogin(callbackQuery = null) {
  if (!weaverSsoEnabled.value)
    return
  try {
    loading.value = true
    $message.loading('正在使用泛微票据登录...', { key: 'login' })

    const query = callbackQuery || resolveWeaverCallbackQuery()
    const ticket = query.ticket || query.code
    const redirect = query.redirect
    const res = await api.weaverCallback({
      ticket,
      state: query.state,
      tenantId: query.tenantId || route.query.tenantId,
      userClient: 'pc',
    })

    if (res.code !== 200 || !res.data) {
      $message.error(res.msg || '泛微登录失败', { key: 'login' })
      return
    }

    await onLoginSuccess(res.data)

    // 优先跳转回 redirect，并保持当前应用 base/hash 规则
    const targetPath = resolveAppTargetPath(redirect)
    window.location.href = buildAppUrl(targetPath)
  }
  catch (e) {
    console.error(e)
    $message.error('泛微登录异常', { key: 'login' })
  }
  finally {
    loading.value = false
  }
}

// 获取并设置菜单数据
async function loadAndSetMenuData() {
  try {
    const permissionStore = usePermissionStore()

    // 获取菜单数据
    const res = await mainApi.getMenu(1)
    if (res.code === 200 && res.data) {
      // 设置菜单数据到store
      permissionStore.setMenuData(res.data)
    }
    else {
      console.error('菜单数据格式不正确:', res)
    }

    // 等待菜单数据加载完成（最多等待5秒）
    let waitCount = 0
    while (!permissionStore.menuDataLoaded && waitCount < 50) {
      await new Promise(resolve => setTimeout(resolve, 100))
      waitCount++
    }
  }
  catch (error) {
    console.error('获取菜单数据失败:', error)
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap');

/* Container */
.login-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  font-family: 'Inter', sans-serif;
}

/* Animated background */
.login-bg-animated {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 50%, #bae6fd 100%);
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: float 25s ease-in-out infinite;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  top: -20%;
  left: -10%;
  animation-delay: 0s;
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
  bottom: -15%;
  right: -10%;
  animation-delay: 8s;
}

.orb-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #06b6d4, #22d3ee);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: 16s;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  z-index: 1;
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(40px, -60px) scale(1.05);
  }
  50% {
    transform: translate(-30px, 30px) scale(0.95);
  }
  75% {
    transform: translate(20px, 40px) scale(1.02);
  }
}

/* Login card */
.login-card {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1fr;
  max-width: 1100px;
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.02),
    0 4px 6px -1px rgba(0, 0, 0, 0.02),
    0 12px 24px -4px rgba(0, 0, 0, 0.04),
    0 24px 48px -8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  animation: slideUp 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (min-width: 768px) {
  .login-card {
    grid-template-columns: 42% 58%;
  }
}

/* Left side - Branding */
.login-brand {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);
  padding: 48px 40px;
  display: none;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

@media (min-width: 768px) {
  .login-brand {
    display: flex;
  }
}

.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.deco-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -80px;
  animation: pulse 8s ease-in-out infinite;
}

.deco-2 {
  width: 200px;
  height: 200px;
  bottom: -60px;
  left: -40px;
  animation: pulse 6s ease-in-out infinite 2s;
}

.deco-3 {
  width: 150px;
  height: 150px;
  top: 40%;
  right: 20%;
  background: rgba(255, 255, 255, 0.05);
  animation: pulse 7s ease-in-out infinite 4s;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.logo-wrapper {
  margin-bottom: 28px;
  animation: fadeIn 0.8s ease-out 0.2s both;
  display: flex;
  justify-content: center;
}

.logo-img {
  width: 72px;
  height: 72px;
  object-fit: contain;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.25));
  transition: transform 0.3s ease;
}

.logo-img:hover {
  transform: scale(1.05);
}

.brand-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 1.75rem;
  font-weight: 700;
  color: white;
  margin-bottom: 8px;
  animation: fadeIn 0.8s ease-out 0.3s both;
  letter-spacing: -0.02em;
}

.brand-subtitle {
  font-size: 0.9375rem;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 40px;
  animation: fadeIn 0.8s ease-out 0.4s both;
  font-weight: 400;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: fadeIn 0.8s ease-out 0.5s both;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  text-align: left;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  cursor: default;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(4px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: white;
}

.feature-desc {
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.7);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Right side - Form */
.login-form-wrapper {
  padding: 48px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.5);
}

@media (max-width: 767px) {
  .login-form-wrapper {
    padding: 32px 24px;
  }
}

.login-form {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;
  animation: fadeIn 0.6s ease-out 0.2s both;
  text-align: center;
}

.form-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 1.625rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.form-subtitle {
  font-size: 0.9375rem;
  color: #64748b;
  font-weight: 400;
}

.form-body {
  animation: fadeIn 0.6s ease-out 0.4s both;
}

/* Form groups */
.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
  letter-spacing: 0.01em;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.modern-input {
  width: 100%;
}

/* Override Naive UI input styles */
:deep(.modern-input.n-input) {
  border-radius: 10px;
}

:deep(.modern-input .n-input__border),
:deep(.modern-input .n-input__state-border) {
  border-radius: 10px;
}

:deep(.modern-input.n-input:not(.n-input--disabled):hover) {
  border-color: #3b82f6;
}

:deep(.modern-input.n-input:not(.n-input--disabled):focus-within) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-icon {
  color: #94a3b8;
  font-size: 16px;
}

/* Form options */
.form-options {
  margin-bottom: 24px;
}

.checkbox-label {
  font-size: 0.875rem;
  color: #64748b;
}

/* Login button */
.login-button {
  margin-top: 8px;
  border-radius: 10px;
  font-size: 0.9375rem;
  font-weight: 600;
  height: 46px;
  transition: all 0.25s ease;
  cursor: pointer;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.25);
}

.login-button:active {
  transform: translateY(0);
}

.login-button :deep(.n-button__content) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.button-text {
  font-family: 'Plus Jakarta Sans', sans-serif;
  letter-spacing: 0.01em;
}

.button-icon {
  font-size: 1.125rem;
  transition: transform 0.25s ease;
}

.login-button:hover .button-icon {
  transform: translateX(4px);
}

/* Social login section */
.social-login-section {
  margin-top: 28px;
}

.social-divider {
  position: relative;
  text-align: center;
  margin-bottom: 20px;
}

.social-divider::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e2e8f0 20%, #e2e8f0 80%, transparent);
}

.divider-text {
  position: relative;
  display: inline-block;
  padding: 0 16px;
  background: #fff;
  font-size: 0.8125rem;
  color: #94a3b8;
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.social-button {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  padding: 0;
}

.social-button:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.15);
}

.social-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.social-icon-fallback {
  font-size: 20px;
  color: #64748b;
}

.social-button:hover .social-icon-fallback {
  color: #3b82f6;
}

/* Dark mode */
.dark .login-bg-animated {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
}

.dark .login-card {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.1);
}

.dark .login-form-wrapper {
  background: transparent;
}

.dark .form-title {
  color: #f1f5f9;
}

.dark .form-subtitle {
  color: #94a3b8;
}

.dark .form-label {
  color: #cbd5e1;
}

.dark .social-divider::before {
  background: linear-gradient(90deg, transparent, #334155 20%, #334155 80%, transparent);
}

.dark .divider-text {
  background: transparent;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }

  .gradient-orb,
  .deco-circle {
    animation: none;
  }
}

/* Responsive */
@media (max-width: 767px) {
  .form-title {
    font-size: 1.375rem;
  }

  .login-card {
    border-radius: 20px;
  }
}
</style>
