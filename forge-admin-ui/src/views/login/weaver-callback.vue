<template>
  <div class="callback-container">
    <div class="callback-card">
      <div class="callback-icon">
        <i v-if="loading" class="ai-icon:loader animate-spin" />
        <i v-else-if="success" class="ai-icon:check-circle" style="color: #22C55E" />
        <i v-else class="ai-icon:x-circle" style="color: #EF4444" />
      </div>
      <h2 class="callback-title">
        {{ message }}
      </h2>
      <p class="callback-desc">
        {{ detailMessage }}
      </p>
    </div>
  </div>
</template>

<script setup>
import mainApi from '@/api'
import { useAuthStore, usePermissionStore, useUserStore } from '@/store'
import { lStorage } from '@/utils'
import { initKeyExchange } from '@/utils/crypto/key-exchange'
import { request } from '@/utils/http'
import api from './api'

const authStore = useAuthStore()
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const loading = ref(true)
const success = ref(false)
const message = ref('正在处理授权...')
const detailMessage = ref('请稍候...')

async function handleCallback() {
  const { ticket, code, state } = route.query
  const realTicket = ticket || code

  if (!realTicket || !state) {
    loading.value = false
    success.value = false
    message.value = '授权参数缺失'
    detailMessage.value = '缺少必要的授权参数，请重新尝试登录'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
    return
  }

  try {
    // 由后端完成 accessToken/profile 并签发系统token
    const res = await api.weaverCallback({ ticket: realTicket, state })
    if (res.code !== 200 || !res.data) {
      loading.value = false
      success.value = false
      message.value = '登录失败'
      detailMessage.value = res.msg || '泛微登录失败'
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    await onLoginSuccess(res.data)

    loading.value = false
    success.value = true
    message.value = '登录成功'
    detailMessage.value = '正在跳转到首页...'
  }
  catch (error) {
    console.error('泛微登录回调处理失败:', error)
    loading.value = false
    success.value = false
    message.value = '登录异常'
    detailMessage.value = '处理登录时发生错误，请重新尝试'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
}

async function onLoginSuccess(data = {}) {
  if (data.accessToken) {
    authStore.setToken({
      accessToken: data.accessToken,
      tokenType: data.tokenType || 'Bearer',
      expiresIn: data.expiresIn,
    })

    try {
      await initKeyExchange(request, data.accessToken)
    }
    catch (error) {
      console.warn('密钥交换失败，将使用降级方案:', error)
    }
  }

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
    lStorage.set('userInfo', loginUser)
  }

  try {
    await loadAndSetMenuData()

    if (window.opener) {
      window.opener.postMessage({
        type: 'SOCIAL_LOGIN_SUCCESS',
        data: {
          accessToken: data.accessToken,
          tokenType: data.tokenType,
          expiresIn: data.expiresIn,
        },
      }, '*')
      window.close()
    }
    else {
      const defaultRedirectPath = import.meta.env.VITE_HOME_PATH || '/'
      router.push(defaultRedirectPath)
    }
  }
  catch (error) {
    console.error(error)
    if (window.opener) {
      window.opener.postMessage({
        type: 'SOCIAL_LOGIN_FAILED',
        error: error.message,
      }, '*')
      window.close()
    }
    else {
      router.push('/login')
    }
  }
}

async function loadAndSetMenuData() {
  try {
    const permissionStore = usePermissionStore()
    const res = await mainApi.getMenu(1)
    if (res.code === 200 && res.data) {
      permissionStore.setMenuData(res.data)
    }

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

onMounted(() => {
  handleCallback()
})
</script>

<style scoped>
.callback-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: radial-gradient(1200px 600px at 30% 20%, rgba(59, 130, 246, 0.15), transparent),
    radial-gradient(900px 500px at 70% 80%, rgba(34, 197, 94, 0.12), transparent),
    #0b1220;
}

.callback-card {
  width: 520px;
  max-width: 92vw;
  padding: 28px 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  text-align: center;
}

.callback-icon {
  font-size: 44px;
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.9);
}

.callback-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
}

.callback-desc {
  margin-top: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
}
</style>

