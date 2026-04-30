<template>
  <div class="home-page">
    <section class="hero" :style="heroBgStyle">
      <div class="hero__shade" aria-hidden="true" />
      <div class="hero__inner">
        <div class="hero__text">
          <h1 class="hero__title">
            欢迎使用数信院综合管理系统
          </h1>
          <p class="hero__subtitle">
            高效工作，规范管理，团队协作，项目推进
          </p>
          <div class="hero__date-row">
            <n-text depth="3" class="hero__date">
              {{ todayCnLine }}
            </n-text>
          </div>
        </div>
      </div>
    </section>

    <div class="home-scroll">
      <n-grid cols="2 s:2 m:4" responsive="screen" :x-gap="16" :y-gap="16" class="metric-grid">
        <n-gi>
          <n-card
            hoverable
            class="metric-card metric-card--primary"
            :bordered="false"
            embedded
            @click="router.push('/dept-daily/work-report')"
          >
            <template #header>
              <div class="metric-hd">
                <span class="metric-hd__icon-wrap metric-hd__icon-wrap--blue">
                  <i class="i-material-symbols:edit-document-outline-rounded" />
                </span>
                <span class="metric-hd__ttl">本月工作月报填报</span>
              </div>
            </template>
            <n-spin :show="summaryLoading">
              <div class="metric-body metric-body--accent">
                <span class="metric-main">{{ reportFilled }}<span class="metric-slash">/</span>{{ reportTotal }}</span>
                <div class="metric-sub">
                  已填报 / 需填报项目
                </div>
                <n-progress
                  type="line"
                  :percentage="reportProgressPct"
                  :height="8"
                  :border-radius="4"
                  :show-indicator="false"
                  :color="progressColor(reportProgressPct)"
                />
              </div>
            </n-spin>
          </n-card>
        </n-gi>

        <n-gi>
          <n-card
            hoverable
            class="metric-card"
            :bordered="false"
            embedded
            @click="router.push('/dept-daily/attendance')"
          >
            <template #header>
              <div class="metric-hd">
                <span class="metric-hd__icon-wrap metric-hd__icon-wrap--cyan">
                  <i class="i-material-symbols:calendar-month-rounded" />
                </span>
                <span class="metric-hd__ttl">本月考勤统计</span>
              </div>
            </template>
            <n-spin :show="summaryLoading">
              <div class="metric-body">
                <n-space wrap :size="[12, 8]">
                  <n-tag round size="small" type="success" :bordered="false">
                    出勤 {{ attCounts.work }}
                  </n-tag>
                  <n-tag round size="small" type="info" :bordered="false">
                    出差 {{ attCounts.travel }}
                  </n-tag>
                  <n-tag round size="small" type="warning" :bordered="false">
                    请假 {{ attCounts.leave }}
                  </n-tag>
                  <n-tag round size="small" type="default" :bordered="false">
                    休息 {{ attCounts.rest }}
                  </n-tag>
                </n-space>
                <div class="metric-caption">
                  {{ attStatusLabel }}
                </div>
              </div>
            </n-spin>
          </n-card>
        </n-gi>

        <n-gi>
          <n-card
            hoverable
            class="metric-card"
            :bordered="false"
            embedded
            @click="router.push('/dept-daily/work-report')"
          >
            <template #header>
              <div class="metric-hd">
                <span class="metric-hd__icon-wrap metric-hd__icon-wrap--violet">
                  <i class="i-material-symbols:groups-rounded" />
                </span>
                <span class="metric-hd__ttl">参与项目</span>
              </div>
            </template>
            <n-spin :show="summaryLoading">
              <div class="metric-body metric-body--accent">
                <span class="metric-main">{{ reportTotal }}</span>
                <div class="metric-sub">
                  当前需填报范围内的项目数
                </div>
              </div>
            </n-spin>
          </n-card>
        </n-gi>

        <n-gi>
          <n-card class="metric-card metric-card--pending" :bordered="false" embedded>
            <template #header>
              <div class="metric-hd">
                <span class="metric-hd__icon-wrap metric-hd__icon-wrap--amber">
                  <i class="i-material-symbols:playlist-add-check-rounded" />
                </span>
                <span class="metric-hd__ttl">待提交项</span>
              </div>
            </template>
            <n-spin :show="summaryLoading">
              <div class="pending-list">
                <div
                  v-for="item in pendingItemsDisplay"
                  :key="item.key"
                  class="pending-row"
                  role="button"
                  tabindex="0"
                  @click="router.push(item.path)"
                  @keydown.enter.prevent="router.push(item.path)"
                >
                  <n-tag v-if="item.urgent" size="tiny" type="warning" round :bordered="false">
                    待办
                  </n-tag>
                  <span class="pending-row__txt">{{ item.label }}</span>
                  <i class="i-material-symbols:chevron-forward-rounded pending-row__go" />
                </div>
                <n-empty v-if="pendingItemsDisplay.length === 0 && !summaryLoading" size="small" description="暂无待提交，保持很好" />
              </div>
            </n-spin>
          </n-card>
        </n-gi>
      </n-grid>

      <n-card class="notice-panel" title="系统通知" :bordered="false" :segmented="{ content: true }">
        <template #header-extra>
          <n-space align="center" wrap :size="8">
            <n-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" type="info" />
            <n-button quaternary type="primary" size="small" @click="goToNoticePage">
              查看更多 <i class="i-material-symbols:arrow-forward-rounded i-align" />
            </n-button>
          </n-space>
        </template>
        <n-scrollbar style="max-height: 360px">
          <div v-if="noticeList.length === 0 && !noticeLoading" class="notice-empty">
            <n-empty description="暂无系统通知" size="medium" />
          </div>
          <n-spin v-else :show="noticeLoading">
            <n-list clickable hoverable>
              <n-list-item v-for="notice in noticeList" :key="notice.noticeId" @click="handleViewNotice(notice)">
                <template #prefix>
                  <n-avatar
                    round
                    size="medium"
                    :style="{ background: 'linear-gradient(135deg, var(--primary-color), rgba(96,165,250,0.95))', color: '#fff' }"
                  >
                    <i class="i-material-symbols:campaign-rounded" />
                  </n-avatar>
                </template>
                <n-thing content-style="margin-top: 4px;">
                  <template #header>
                    <n-ellipsis :line-clamp="1" style="font-weight: 600" :class="{ 'notice-title-unread': notice.isRead === 0 }">
                      {{ notice.noticeTitle }}
                    </n-ellipsis>
                  </template>
                  <template #description>
                    <n-text depth="3" style="font-size: 13px;">
                      {{ formatTime(notice.publishTime) }}
                    </n-text>
                  </template>
                </n-thing>
                <template #suffix>
                  <n-tag v-if="notice.isRead === 0" size="small" round type="info" :bordered="false">
                    未读
                  </n-tag>
                </template>
              </n-list-item>
            </n-list>
          </n-spin>
        </n-scrollbar>
      </n-card>
    </div>

    <n-modal
      v-model:show="showNoticeModal"
      preset="card"
      title="公告详情"
      style="width: min(880px, 92vw)"
      :segmented="{ content: 'soft' }"
    >
      <div v-if="currentNotice" class="notice-detail">
        <div class="detail-header">
          <h3>{{ currentNotice.noticeTitle }}</h3>
          <n-space class="mt-8">
            <n-tag :type="getNoticeTypeColor(currentNotice.noticeType)" size="small">
              {{ getNoticeTypeText(currentNotice.noticeType) }}
            </n-tag>
            <span class="text-gray-400">发布人：{{ currentNotice.publisherName }}</span>
            <span class="text-gray-400">发布时间：{{ currentNotice.publishTime }}</span>
          </n-space>
        </div>
        <n-divider />
        <div class="detail-content" v-html="currentNotice.noticeContent" />

        <div v-if="currentNotice.attachments && currentNotice.attachments.length > 0" class="detail-attachments">
          <n-divider />
          <div class="attachment-title">
            附件
          </div>
          <n-space vertical>
            <div
              v-for="file in currentNotice.attachments"
              :key="file.fileId"
              class="attachment-item"
              @click="handleDownloadAttachment(file)"
            >
              <i class="i-material-symbols:attach-file" />
              <span>{{ file.fileName }}</span>
              <span class="text-gray-400">{{ formatFileSize(file.fileSize) }}</span>
              <i class="i-material-symbols:download" style="margin-left: auto" />
            </div>
          </n-space>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import heroBgUrl from '@/assets/images/background.png'
import { getMonthView } from '@/api/dept-daily/attendance'
import { listFillableProjects } from '@/api/dept-daily/project'
import { pageUserMonthItem } from '@/api/dept-daily/report'
import { request } from '@/utils'

const router = useRouter()

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function pad2(n) {
  return String(n).padStart(2, '0')
}

const todayCnLine = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  const day = d.getDate()
  return `今天是${y}年${m}月${day}日 ${WEEKDAYS[d.getDay()]}`
})

const heroBgStyle = computed(() => ({
  backgroundImage: `linear-gradient(
    105deg,
    rgba(248, 250, 252, 0.92) 0%,
    rgba(248, 250, 252, 0.55) 38%,
    rgba(226, 245, 255, 0.35) 100%
  ),
  url(${heroBgUrl})`,
}))

/** 工作台摘要 */
const summaryLoading = ref(true)
const reportFilled = ref(0)
const reportTotal = ref(0)
const attCounts = ref({ work: 0, travel: 0, leave: 0, rest: 0 })
const attSheetStatus = ref(null)

const reportProgressPct = computed(() => {
  if (!reportTotal.value)
    return 0
  return Math.round((reportFilled.value / reportTotal.value) * 100)
})

function progressColor(pct) {
  if (pct >= 100)
    return 'linear-gradient(90deg, #22c55e, #86efac)'
  if (pct >= 40)
    return 'linear-gradient(90deg, #3b82f6, #60a5fa)'
  return 'linear-gradient(90deg, #f59e0b, #fbbf24)'
}

const attStatusLabel = computed(() => {
  const s = attSheetStatus.value
  if (s === 'SUBMITTED')
    return '考勤表状态：本月已提交'
  if (s === 'DRAFT')
    return '考勤表状态：草稿（可继续编辑并提交）'
  if (!s || s === 'NONE')
    return '考勤表状态：待完善'
  return `考勤表状态：${s}`
})

const pendingItemsDisplay = computed(() => {
  const list = []
  const unfilled = Math.max(0, reportTotal.value - reportFilled.value)
  if (unfilled > 0) {
    list.push({
      key: 'report',
      label: `个人工作月报 ${unfilled} 项未完成`,
      path: '/dept-daily/work-report',
      urgent: true,
    })
  }
  const s = attSheetStatus.value
  if (s !== 'SUBMITTED') {
    list.push({
      key: 'att',
      label: s === 'DRAFT' ? '考勤填报待提交' : '考勤填报尚待提交',
      path: '/dept-daily/attendance',
      urgent: s !== 'DRAFT',
    })
  }
  return list
})

async function loadWorkbenchSummary() {
  summaryLoading.value = true
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const reportYm = `${y}-${pad2(m)}`

  try {
    const [projRes, pageRes, viewRes] = await Promise.all([
      listFillableProjects({}),
      pageUserMonthItem({ reportYm, pageNum: 1, pageSize: 200 }),
      getMonthView(y, m),
    ])

    const projects = projRes.data || []
    reportTotal.value = projects.length
    const records = pageRes.data?.records || []
    const byProject = new Map(records.map(r => [r.projectId, r]))
    let filled = 0
    for (const p of projects) {
      const text = String(byProject.get(p.id)?.progressText ?? '').trim()
      if (text.length)
        filled++
    }
    reportFilled.value = filled

    const days = viewRes?.data?.days || []
    const c = { work: 0, travel: 0, leave: 0, rest: 0 }
    for (const d of days) {
      const st = d.status
      if (st === 'WORK') c.work++
      else if (st === 'TRAVEL') c.travel++
      else if (st === 'LEAVE') c.leave++
      else if (st === 'REST') c.rest++
    }
    attCounts.value = c
    attSheetStatus.value = viewRes?.data?.status ?? null
  }
  catch (e) {
    console.warn('首页工作台摘要加载失败（可能没有对应菜单权限或未登录插件）:', e)
    reportTotal.value = reportTotal.value || 0
  }
  finally {
    summaryLoading.value = false
  }
}

/** 通知 */
const noticeList = ref([])
const unreadCount = ref(0)
const noticeLoading = ref(false)
const showNoticeModal = ref(false)
const currentNotice = ref(null)

async function loadNoticeList() {
  noticeLoading.value = true
  try {
    const res = await request.get('/system/notice/user/page', {
      params: { pageNum: 1, pageSize: 8 },
    })
    if (res.code === 200)
      noticeList.value = res.data.records || []
  }
  catch (error) {
    console.error('加载通知失败:', error)
  }
  finally {
    noticeLoading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const res = await request.get('/system/notice/user/unread-count')
    if (res.code === 200)
      unreadCount.value = res.data
  }
  catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

async function handleViewNotice(notice) {
  try {
    const res = await request.post('/system/notice/getById', null, {
      params: { noticeId: notice.noticeId },
    })
    if (res.code === 200) {
      currentNotice.value = res.data
      showNoticeModal.value = true
      if (notice.isRead === 0)
        await markAsRead(notice.noticeId)
    }
  }
  catch (error) {
    window.$message.error('获取详情失败')
  }
}

async function markAsRead(noticeId) {
  try {
    await request.post('/system/notice/markAsRead', null, {
      params: { noticeId },
    })
    loadNoticeList()
    loadUnreadCount()
  }
  catch (error) {
    console.error('标记已读失败:', error)
  }
}

function goToNoticePage() {
  router.push('/system/notice-list')
}

function handleDownloadAttachment(file) {
  try {
    const downloadUrl = `/api/system/file/download?fileId=${file.fileId}`
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.$message.success('开始下载')
  }
  catch (error) {
    window.$message.error('下载失败')
  }
}

function formatFileSize(bytes) {
  if (!bytes)
    return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / k ** i).toFixed(2)} ${sizes[i]}`
}

function formatTime(time) {
  if (!time)
    return '-'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute)
    return '刚刚'
  if (diff < hour)
    return `${Math.floor(diff / minute)}分钟前`
  if (diff < day)
    return `${Math.floor(diff / hour)}小时前`
  if (diff < 7 * day)
    return `${Math.floor(diff / day)}天前`

  return time.split(' ')[0]
}

function getNoticeTypeText(type) {
  const typeMap = {
    NOTICE: '通知公告',
    ANNOUNCEMENT: '系统公告',
    NEWS: '新闻动态',
  }
  return typeMap[type] || type
}

function getNoticeTypeColor(type) {
  const colorMap = {
    NOTICE: 'info',
    ANNOUNCEMENT: 'warning',
    NEWS: 'success',
  }
  return colorMap[type] || 'default'
}

onMounted(() => {
  loadWorkbenchSummary()
  loadNoticeList()
  loadUnreadCount()
})
</script>

<style scoped>
.home-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--body-color, #f4f7fb);
}

.hero {
  position: relative;
  flex-shrink: 0;
  min-height: 280px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.hero__shade {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero__inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 24px 40px;
  display: flex;
  align-items: center;
  min-height: 280px;
}

.hero__text {
  max-width: 640px;
}

.hero__title {
  margin: 0 0 12px;
  font-size: clamp(1.45rem, 3.2vw, 1.85rem);
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #0f172a;
  line-height: 1.25;
}

.hero__subtitle {
  margin: 0 0 18px;
  font-size: 15px;
  line-height: 1.65;
  color: #334155;
  font-weight: 500;
}

.hero__date-row {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(8px);
}

.hero__date {
  font-size: 14px;
  font-weight: 500;
  color: #475569 !important;
}

.home-scroll {
  flex: 1;
  padding: 20px 24px 32px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.metric-grid {
  margin-bottom: 20px;
}

.metric-card {
  border-radius: 14px !important;
  border: 1px solid rgba(226, 232, 240, 0.85) !important;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: default;
  height: 100%;
}

.metric-card--primary {
  border-color: rgba(59, 130, 246, 0.22) !important;
}

.metric-card[hoverable]:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  cursor: pointer;
}

.metric-card.metric-card--pending:hover {
  transform: none;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.metric-card :deep(.n-card-header) {
  padding-bottom: 8px;
}

.metric-hd {
  display: flex;
  align-items: center;
  gap: 10px;
}

.metric-hd__icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.metric-hd__icon-wrap--blue {
  background: linear-gradient(135deg, #2563eb, #38bdf8);
}

.metric-hd__icon-wrap--cyan {
  background: linear-gradient(135deg, #0891b2, #22d3ee);
}

.metric-hd__icon-wrap--violet {
  background: linear-gradient(135deg, #6366f1, #a78bfa);
}

.metric-hd__icon-wrap--amber {
  background: linear-gradient(135deg, #d97706, #fbbf24);
}

.metric-hd__ttl {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.metric-body {
  min-height: 88px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.metric-body--accent {
  gap: 6px;
}

.metric-main {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.metric-slash {
  font-weight: 600;
  color: #94a3b8;
  margin: 0 2px;
  font-size: 22px;
}

.metric-sub {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.metric-caption {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.45;
}

.pending-list {
  min-height: 88px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pending-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: rgba(248, 250, 252, 0.9);
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
  font-size: 13px;
  color: #334155;
}

.pending-row:hover,
.pending-row:focus-visible {
  background: rgba(239, 246, 255, 0.95);
  border-color: rgba(59, 130, 246, 0.35);
  outline: none;
}

.pending-row__txt {
  flex: 1;
  min-width: 0;
  font-weight: 500;
}

.pending-row__go {
  flex-shrink: 0;
  font-size: 18px;
  color: #94a3b8;
}

.notice-panel {
  border-radius: 14px !important;
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.notice-panel :deep(.n-card-header__main) {
  font-weight: 700;
  font-size: 15px;
}

.notice-empty {
  padding: 32px;
}

.notice-title-unread {
  color: var(--primary-color);
}

.mt-8 {
  margin-top: 8px;
}

.text-gray-400 {
  color: #94a3b8;
  font-size: 13px;
}

.i-align {
  font-size: 16px;
  vertical-align: -3px;
  margin-left: 2px;
}

.notice-detail {
  padding: 4px 0;
}

.detail-header h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
}

.detail-content {
  line-height: 1.8;
  font-size: 14px;
  color: var(--text-color-2);
  min-height: 60px;
}

.detail-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 10px 0;
}

.detail-attachments {
  margin-top: 20px;
}

.attachment-title {
  font-weight: 600;
  margin-bottom: 10px;
  font-size: 14px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: background 0.15s ease;
}

.attachment-item:hover {
  background: rgba(239, 246, 255, 0.6);
}

@media (max-width: 639px) {
  .hero__inner {
    padding: 24px 16px 28px;
    min-height: 240px;
  }

  .home-scroll {
    padding: 16px;
  }

  .metric-main {
    font-size: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .metric-card[hoverable]:hover {
    transform: none;
  }
}

/* 暗色：标题区仍保持浅色可读（背景图左侧偏亮）；下方卡片随主题 */
:root.dark .home-page {
  background: var(--body-color);
}

:root.dark .hero__title {
  color: #f1f5f9;
}

:root.dark .hero__subtitle {
  color: #cbd5e1;
}

:root.dark .hero__date-row {
  background: rgba(30, 41, 59, 0.55);
  border-color: rgba(51, 65, 85, 0.85);
}

:root.dark .hero__date {
  color: #e2e8f0 !important;
}

:root.dark .metric-hd__ttl,
:root.dark .metric-main {
  color: var(--text-color-1);
}

:root.dark .metric-sub,
:root.dark .metric-caption {
  color: var(--text-color-3);
}
</style>
