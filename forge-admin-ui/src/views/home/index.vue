<template>
  <div class="sx-home">
    <!-- 欢迎横幅 -->
    <section class="hero" aria-labelledby="sx-home-title">
      <div class="hero__inner">
        <h1 id="sx-home-title" class="hero__title">
          欢迎使用数信院综合管理系统
        </h1>
        <p class="hero__sub">
          高效协同填报考勤与月报，数据集中呈现，便于研发室管理与自查。
        </p>
        <p class="hero__date">
          今天是 {{ todayLabel }}
        </p>
      </div>
    </section>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="stat-card"
        :class="`stat-card--${card.tone}`"
        role="button"
        tabindex="0"
        @click="go(card.path)"
        @keydown.enter="go(card.path)"
      >
        <div class="stat-card__text">
          <div class="stat-card__label">
            {{ card.label }}
          </div>
          <div class="stat-card__value">
            <n-spin v-if="statsLoading" size="small" />
            <template v-else>
              {{ card.value }}
            </template>
          </div>
          <div class="stat-card__hint">
            {{ card.hint }}
          </div>
        </div>
        <div class="stat-card__icon" aria-hidden="true">
          <i :class="card.icon" />
        </div>
      </div>
    </div>

    <!-- 快捷入口 + 通知 -->
    <div class="lower-grid">
      <n-card class="panel panel--quick" :bordered="false" size="small">
        <template #header>
          <div class="panel-head">
            <span class="panel-head__title">我的快捷入口</span>
          </div>
        </template>
        <div class="quick-grid">
          <button
            v-for="q in quickEntries"
            :key="q.path"
            type="button"
            class="quick-cell"
            @click="go(q.path)"
          >
            <div class="quick-cell__icon" :class="`quick-cell__icon--${q.tone}`">
              <i :class="q.icon" />
            </div>
            <span class="quick-cell__label">{{ q.label }}</span>
          </button>
        </div>
      </n-card>

      <n-card class="panel panel--notice" :bordered="false" size="small">
        <template #header>
          <div class="panel-head panel-head--split">
            <span class="panel-head__title">系统通知</span>
            <n-button text type="primary" size="small" @click="goToNoticePage">
              查看更多
              <i class="notice-more-ico ai-icon:chevron-right" />
            </n-button>
          </div>
        </template>
        <n-scrollbar style="max-height: 320px">
          <div v-if="!noticeList.length && !noticeLoading" class="notice-empty">
            暂无通知
          </div>
          <div v-else class="notice-feed">
            <div
              v-for="(notice, idx) in noticeList"
              :key="notice.noticeId"
              class="notice-row"
              @click="handleViewNotice(notice)"
            >
              <div
                class="notice-row__dot"
                :class="`notice-row__dot--${dotTone(idx)}`"
              />
              <div class="notice-row__body">
                <div class="notice-row__title">
                  {{ notice.noticeTitle }}
                </div>
                <div class="notice-row__date">
                  {{ formatNoticeDate(notice.publishTime) }}
                </div>
                <n-ellipsis :line-clamp="2" class="notice-row__snippet">
                  {{ noticeSnippet(notice) }}
                </n-ellipsis>
              </div>
            </div>
          </div>
        </n-scrollbar>
      </n-card>
    </div>

    <n-modal
      v-model:show="showNoticeModal"
      preset="card"
      title="公告详情"
      style="width: 800px"
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
import { getMonthView } from '@/api/dept-daily/attendance'
import { listFillableProjects } from '@/api/dept-daily/project'
import { pageUserMonthItem } from '@/api/dept-daily/report'
import { request } from '@/utils'

const router = useRouter()

const statsLoading = ref(false)
const noticeLoading = ref(false)

const attendanceSubmitted = ref(false)
const attendanceWorkTotal = ref(0)
const attendanceWorkPassed = ref(0)

const projectCount = ref(0)
const reportFilled = ref(0)
const reportTotal = ref(0)

const noticeList = ref([])
const showNoticeModal = ref(false)
const currentNotice = ref(null)

const now = new Date()
const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

const todayLabel = computed(() => {
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const d = now.getDate()
  return `${y}年${m}月${d}日 ${weekdays[now.getDay()]}`
})

const pendingCount = computed(() => {
  let n = 0
  if (!attendanceSubmitted.value)
    n += 1
  n += Math.max(0, reportTotal.value - reportFilled.value)
  return n
})

const statCards = computed(() => {
  const tw = attendanceWorkTotal.value
  const tp = attendanceWorkPassed.value
  const attVal = statsLoading.value
    ? ''
    : (!tw ? '— / —' : (attendanceSubmitted.value ? `${tw} / ${tw}` : `${tp} / ${tw}`))
  const attHint = attendanceSubmitted.value ? '本月考勤已提交' : '请在「月度考勤」核对后提交'

  const repVal = statsLoading.value
    ? ''
    : (reportTotal.value ? `${reportFilled.value} / ${reportTotal.value}` : '— / —')
  const repHint = '已填写项目条数 / 可填报项目'

  const projVal = statsLoading.value ? '' : `${projectCount.value} 个`
  const pendVal = statsLoading.value ? '' : `${pendingCount.value} 项`

  return [
    {
      key: 'att',
      label: '本月考勤状态',
      value: attVal,
      hint: attHint,
      tone: 'blue',
      icon: 'i-material-symbols:calendar-month-outline-rounded',
      path: '/dept-daily/attendance',
    },
    {
      key: 'rep',
      label: '本月已填报工作月报',
      value: repVal,
      hint: repHint,
      tone: 'green',
      icon: 'i-material-symbols:description-outline-rounded',
      path: '/dept-daily/work-report',
    },
    {
      key: 'proj',
      label: '参与项目数量',
      value: projVal,
      hint: '当前可填报项目（本月）',
      tone: 'purple',
      icon: 'i-material-symbols:groups-outline-rounded',
      path: '/dept-daily/project-config',
    },
    {
      key: 'pend',
      label: '待提交事项',
      value: pendVal,
      hint: '含未提交考勤与未填写月报条目',
      tone: 'orange',
      icon: 'i-material-symbols:assignment-outline-rounded',
      path: '/dept-daily/work-report',
    },
  ]
})

const quickEntries = [
  { label: '考勤与工作填报', path: '/dept-daily/attendance', icon: 'i-material-symbols:event-available-outline-rounded', tone: 'blue' },
  { label: '个人工作月报', path: '/dept-daily/work-report', icon: 'i-material-symbols:person-outline-rounded', tone: 'green' },
  { label: '项目月报', path: '/dept-daily/project-report', icon: 'i-material-symbols:folder-open-outline-rounded', tone: 'teal' },
  { label: '研发室考勤统览', path: '/dept-daily/attendance-table', icon: 'i-material-symbols:table-chart-outline-rounded', tone: 'indigo' },
  { label: '研发室年月报统览', path: '/dept-daily/report-user-overview', icon: 'i-material-symbols:analytics-outline-rounded', tone: 'purple' },
  { label: '项目与人员配置', path: '/dept-daily/project-config', icon: 'i-material-symbols:tune-rounded', tone: 'orange' },
]

function go(path) {
  if (path)
    router.push(path)
}

function parseYmd(dateStr) {
  if (!dateStr)
    return null
  const parts = String(dateStr).split('-').map(Number)
  if (parts.length < 3)
    return null
  return new Date(parts[0], parts[1] - 1, parts[2])
}

function countWorkdaysProgress(days, year, month) {
  const total = days.filter(d => d.defaultStatus === 'WORK').length
  const today = new Date()
  const y0 = today.getFullYear()
  const m0 = today.getMonth() + 1
  const d0 = today.getDate()

  const isFuture = year > y0 || (year === y0 && month > m0)
  const isPast = year < y0 || (year === y0 && month < m0)

  let passed = 0
  if (isFuture) {
    passed = 0
  }
  else if (isPast) {
    passed = total
  }
  else {
    for (const d of days) {
      if (d.defaultStatus !== 'WORK')
        continue
      const dt = parseYmd(d.date)
      if (dt && dt.getDate() <= d0)
        passed++
    }
  }
  return { total, passed }
}

async function loadDashboardStats() {
  statsLoading.value = true
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const ym = `${y}-${String(m).padStart(2, '0')}`

  try {
    const [mvRes, fillRes, itemRes] = await Promise.all([
      getMonthView(y, m),
      listFillableProjects({}),
      pageUserMonthItem({ reportYm: ym, pageNum: 1, pageSize: 200 }),
    ])

    const mv = mvRes?.data
    const days = mv?.days || []
    attendanceSubmitted.value = mv?.status === 'SUBMITTED'
    const { total, passed } = countWorkdaysProgress(days, y, m)
    attendanceWorkTotal.value = total
    attendanceWorkPassed.value = attendanceSubmitted.value ? total : passed

    const projects = fillRes?.data || []
    projectCount.value = projects.length
    reportTotal.value = projects.length

    const records = itemRes?.data?.records || []
    const byProject = new Map(records.map(r => [r.projectId, r]))
    let filled = 0
    for (const p of projects) {
      const r = byProject.get(p.id)
      const t = (r?.progressText || '').trim()
      if (t.length)
        filled++
    }
    reportFilled.value = filled
  }
  catch (e) {
    console.error(e)
    attendanceWorkTotal.value = 0
    attendanceWorkPassed.value = 0
    projectCount.value = 0
    reportFilled.value = 0
    reportTotal.value = 0
  }
  finally {
    statsLoading.value = false
  }
}

async function loadNoticeList() {
  noticeLoading.value = true
  try {
    const res = await request.get('/system/notice/user/page', {
      params: { pageNum: 1, pageSize: 6 },
    })
    if (res.code === 200)
      noticeList.value = res.data.records || []
  }
  catch (e) {
    console.error(e)
  }
  finally {
    noticeLoading.value = false
  }
}

function dotTone(i) {
  return ['blue', 'green', 'orange'][i % 3]
}

function formatNoticeDate(time) {
  if (!time)
    return ''
  return String(time).split(' ')[0]
}

function noticeSnippet(notice) {
  const raw = notice?.noticeContent || ''
  return String(raw).replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 80)
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
  catch {
    window.$message?.error('获取详情失败')
  }
}

async function markAsRead(noticeId) {
  try {
    await request.post('/system/notice/markAsRead', null, { params: { noticeId } })
    loadNoticeList()
  }
  catch (e) {
    console.error(e)
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
    window.$message?.success('开始下载')
  }
  catch {
    window.$message?.error('下载失败')
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

function getNoticeTypeText(type) {
  const typeMap = { NOTICE: '通知公告', ANNOUNCEMENT: '系统公告', NEWS: '新闻动态' }
  return typeMap[type] || type
}

function getNoticeTypeColor(type) {
  const colorMap = { NOTICE: 'info', ANNOUNCEMENT: 'warning', NEWS: 'success' }
  return colorMap[type] || 'default'
}

onMounted(() => {
  loadDashboardStats()
  loadNoticeList()
})
</script>

<style scoped>
.sx-home {
  min-height: 100%;
  padding: 20px 24px 32px;
  background: #f5f7fa;
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
}

/* ----- Hero ----- */
.hero {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
  min-height: 168px;
  background:
    linear-gradient(
      118deg,
      rgba(230, 244, 255, 0.97) 0%,
      rgba(245, 250, 255, 0.92) 42%,
      rgba(224, 242, 254, 0.55) 100%
    ),
    radial-gradient(900px 200px at 100% 0%, rgba(24, 144, 255, 0.12), transparent 60%),
    radial-gradient(600px 180px at 0% 100%, rgba(56, 189, 248, 0.1), transparent 55%);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(24, 144, 255, 0.12);
}

.hero__inner {
  position: relative;
  z-index: 1;
  padding: 28px 32px;
  max-width: 720px;
}

.hero__title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 600;
  color: #1d39c4;
  letter-spacing: 0.02em;
}

.hero__sub {
  margin: 0 0 8px;
  font-size: 14px;
  color: #434343;
  line-height: 1.6;
}

.hero__date {
  margin: 0;
  font-size: 13px;
  color: #595959;
}

/* ----- Stat cards ----- */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid transparent;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.stat-card:focus-visible {
  outline: 2px solid #1890ff;
  outline-offset: 2px;
}

.stat-card--blue {
  background: #e6f4ff;
  border-color: rgba(24, 144, 255, 0.2);
}
.stat-card--green {
  background: #f6ffed;
  border-color: rgba(82, 196, 26, 0.25);
}
.stat-card--purple {
  background: #f9f0ff;
  border-color: rgba(114, 46, 209, 0.2);
}
.stat-card--orange {
  background: #fff7e6;
  border-color: rgba(250, 140, 22, 0.25);
}

.stat-card__label {
  font-size: 13px;
  color: #595959;
  margin-bottom: 6px;
}

.stat-card__value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-card--blue .stat-card__value {
  color: #1890ff;
}
.stat-card--green .stat-card__value {
  color: #52c41a;
}
.stat-card--purple .stat-card__value {
  color: #722ed1;
}
.stat-card--orange .stat-card__value {
  color: #fa8c16;
}

.stat-card__hint {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

.stat-card__icon {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  opacity: 0.88;
}

.stat-card--blue .stat-card__icon {
  background: rgba(24, 144, 255, 0.12);
  color: #1890ff;
}
.stat-card--green .stat-card__icon {
  background: rgba(82, 196, 26, 0.12);
  color: #52c41a;
}
.stat-card--purple .stat-card__icon {
  background: rgba(114, 46, 209, 0.12);
  color: #722ed1;
}
.stat-card--orange .stat-card__icon {
  background: rgba(250, 140, 22, 0.15);
  color: #fa8c16;
}

/* ----- Lower grid ----- */
.lower-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 16px;
  align-items: start;
}

.panel {
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.panel-head {
  display: flex;
  align-items: center;
}

.panel-head--split {
  justify-content: space-between;
  width: 100%;
}

.panel-head__title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.notice-more-ico {
  font-size: 14px;
  margin-left: 2px;
  vertical-align: middle;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.quick-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
  cursor: pointer;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.quick-cell:hover {
  background: #fff;
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.quick-cell__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.quick-cell__icon--blue {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
}
.quick-cell__icon--green {
  background: linear-gradient(135deg, #52c41a, #73d13d);
}
.quick-cell__icon--teal {
  background: linear-gradient(135deg, #13c2c2, #36cfc9);
}
.quick-cell__icon--indigo {
  background: linear-gradient(135deg, #2f54eb, #597ef7);
}
.quick-cell__icon--purple {
  background: linear-gradient(135deg, #722ed1, #9254de);
}
.quick-cell__icon--orange {
  background: linear-gradient(135deg, #fa8c16, #ffc069);
}

.quick-cell__label {
  font-size: 13px;
  color: #434343;
  text-align: center;
  line-height: 1.35;
}

.notice-empty {
  padding: 40px 16px;
  text-align: center;
  color: #8c8c8c;
  font-size: 13px;
}

.notice-feed {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 4px;
}

.notice-row {
  display: flex;
  gap: 12px;
  padding: 12px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.notice-row:hover {
  background: #f5f5f5;
}

.notice-row__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 5px;
}

.notice-row__dot--blue {
  background: #1890ff;
}
.notice-row__dot--green {
  background: #52c41a;
}
.notice-row__dot--orange {
  background: #fa8c16;
}

.notice-row__title {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 4px;
}

.notice-row__date {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.notice-row__snippet {
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
}

.notice-detail {
  padding: 4px 0;
}

.detail-header h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.detail-content {
  line-height: 1.75;
  color: #434343;
  font-size: 14px;
}

.detail-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.detail-attachments {
  margin-top: 16px;
}

.attachment-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 14px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #595959;
}

.attachment-item:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.mt-8 {
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .lower-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .sx-home {
    padding: 12px;
  }
  .stat-row {
    grid-template-columns: 1fr;
  }
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
