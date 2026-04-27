<template>
  <div class="dept-attendance-page p-4">
    <n-card :bordered="false" class="attendance-card">
      <div class="card-head">
        <div class="card-head__titles">
          <h2 class="card-title">
            考勤填报
          </h2>
          <p v-if="monthLabel" class="card-sub">
            {{ monthLabel }}
            <n-tag
              v-if="sheetStatusLabel"
              size="small"
              :type="isSubmitted ? 'success' : 'default'"
              :bordered="false"
              class="status-tag"
            >
              {{ sheetStatusLabel }}
            </n-tag>
          </p>
        </div>
        <p class="card-hint">
          <span class="card-hint__dot" aria-hidden="true" />
          点击日期在「默认考勤 → 出差 → 请假」间循环；从出差进入请假时需选择类型。
        </p>
      </div>

      <div class="toolbar">
        <n-space align="center" justify="space-between" wrap>
          <n-space align="center" wrap>
            <n-select
              v-model:value="year"
              style="width: 130px"
              :options="yearOptions"
              placeholder="年份"
              :disabled="loading"
              :consistent-menu-width="false"
              @update:value="handleReload"
            />
            <n-select
              v-model:value="month"
              style="width: 110px"
              :options="monthOptions"
              placeholder="月份"
              :disabled="loading"
              :consistent-menu-width="false"
              @update:value="handleReload"
            />
            <n-button
              secondary
              :loading="loading"
              :disabled="loading"
              @click="handleReload"
            >
              刷新
            </n-button>
          </n-space>

          <n-space wrap>
            <n-button
              type="primary"
              :disabled="isSubmitted"
              :loading="oneClickLoading"
              @click="handleOneClickFill"
            >
              一键填报
            </n-button>
            <n-button
              type="success"
              :disabled="isSubmitted"
              :loading="submitLoading"
              @click="handleSubmit"
            >
              提交本月
            </n-button>
          </n-space>
        </n-space>
      </div>

      <n-alert
        v-if="isSubmitted"
        type="info"
        :bordered="false"
        class="submitted-alert"
        title="本月已提交"
      >
        已提交的月份不可再修改；如需调整请联系管理员或走补交流程（若已配置）。
      </n-alert>

      <n-spin
        :show="loading"
        :description="loading && !monthData ? '正在加载月历…' : undefined"
        class="attendance-spin"
      >
        <div class="cal-body">
        <div v-if="!monthData && !loading" class="cal-placeholder">
          <n-empty description="选择年月或点击刷新" size="small" />
        </div>
        <div
          v-else-if="loading && !monthData"
          class="cal-skeleton"
          aria-hidden="true"
        >
          <div class="skeleton-hd" />
          <div class="skeleton-g">
            <div v-for="n in 28" :key="n" class="skeleton-cell" />
          </div>
        </div>
        <div v-else class="calendar">
          <div class="week-header">
            <div
              v-for="(w, wi) in weekDays"
              :key="w"
              class="week-cell"
              :class="{ 'week-cell--end': wi >= 5 }"
            >
              {{ w }}
            </div>
          </div>

          <div class="grid" :class="{ 'grid--dim': isSubmitted }">
            <div v-for="cell in cells" :key="cell.key" class="grid-cell">
              <div
                v-if="cell.type === 'day'"
                class="day-card"
                :class="[statusClass(cell.day.status), { 'is-locked': isSubmitted }]"
                role="button"
                :tabindex="isSubmitted ? -1 : 0"
                :aria-label="dayAriaLabel(cell.day)"
                @click="handleDayClick(cell.day)"
                @keydown="onCardKeydown($event, cell.day)"
              >
                <div class="day-top">
                  <div class="day-icon" aria-hidden="true">
                    <i :class="statusIcon(cell.day.status)" />
                  </div>
                  <div class="day-body">
                    <div class="day-label">
                      {{ statusText(cell.day.status) }}
                    </div>
                    <div class="day-num" aria-hidden="true">
                      {{ dayNumber(cell.day.date) }}
                    </div>
                  </div>
                </div>

                <div v-if="cell.day.status === 'LEAVE'" class="day-sub day-sub--pill">
                  {{ leaveTypeText(cell.day.leaveType) }}
                </div>
                <div v-else-if="cell.day.name" class="day-sub day-sub--muted">
                  {{ cell.day.name }}
                </div>
              </div>

              <div v-else class="empty-cell" />
            </div>
          </div>
        </div>
        </div>
      </n-spin>
    </n-card>

    <n-modal v-model:show="leaveModal.show" preset="card" title="请选择请假类型" style="width: 420px">
      <n-form label-placement="left" label-width="90">
        <n-form-item label="请假类型">
          <n-select v-model:value="leaveModal.leaveType" :options="leaveTypeOptions" placeholder="请选择" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="leaveModal.remark" placeholder="可选" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="leaveModal.show = false">
            取消
          </n-button>
          <n-button type="primary" :loading="leaveModal.loading" @click="confirmLeave">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getMonthView, oneClickFillMonth, submitMonth, toggleDay } from '@/api/dept-daily/attendance'

defineOptions({ name: 'DeptDailyAttendance' })

const loading = ref(true)
const oneClickLoading = ref(false)
const submitLoading = ref(false)

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const weekDays = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日']

const monthData = ref(null)

const leaveTypeOptions = [
  { label: '病假', value: 'SICK' },
  { label: '事假', value: 'PERSONAL' },
  { label: '公休', value: 'PUBLIC' },
  { label: '婚假', value: 'MARRIAGE' },
  { label: '丧假', value: 'BEREAVEMENT' },
  { label: '年假', value: 'ANNUAL' },
  { label: '产假', value: 'MATERNITY' },
  { label: '计划生育假', value: 'FAMILY_PLANNING' },
  { label: '探亲假', value: 'HOME_VISIT' },
  { label: '工伤假', value: 'WORK_INJURY' },
]

const leaveModal = reactive({
  show: false,
  date: '',
  leaveType: null,
  remark: '',
  loading: false,
})

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return Array.from({ length: 5 }).map((_, i) => {
    const v = y - 2 + i
    return { label: `${v}年`, value: v }
  })
})

const monthOptions = computed(() => Array.from({ length: 12 }).map((_, i) => ({ label: `${i + 1}月`, value: i + 1 })))

const isSubmitted = computed(() => monthData.value?.status === 'SUBMITTED')

const monthLabel = computed(() => {
  if (!year.value || !month.value)
    return ''
  return `${year.value}年${month.value}月`
})

const sheetStatusLabel = computed(() => {
  const s = monthData.value?.status
  if (!s || s === 'NONE')
    return null
  if (s === 'DRAFT')
    return '草稿'
  if (s === 'SUBMITTED')
    return '已提交'
  return s
})

const cells = computed(() => {
  const days = monthData.value?.days || []
  if (days.length === 0)
    return []

  const first = new Date(`${year.value}-${String(month.value).padStart(2, '0')}-01T00:00:00`)
  // JS: 0=Sun..6=Sat；我们要 Monday=0..Sunday=6
  const jsDay = first.getDay()
  const offset = (jsDay + 6) % 7

  const list = []
  for (let i = 0; i < offset; i++) list.push({ key: `e-${i}`, type: 'empty' })
  for (const d of days) list.push({ key: d.date, type: 'day', day: d })

  // 补齐到整行
  const tail = (7 - (list.length % 7)) % 7
  for (let i = 0; i < tail; i++) list.push({ key: `t-${i}`, type: 'empty' })
  return list
})

function statusText(status) {
  const map = { WORK: '出勤', REST: '休', TRAVEL: '出差', LEAVE: '请假' }
  return map[status] || status
}

function leaveTypeText(type) {
  const map = {
    SICK: '病假',
    PERSONAL: '事假',
    PUBLIC: '公休',
    MARRIAGE: '婚假',
    BEREAVEMENT: '丧假',
    ANNUAL: '年假',
    MATERNITY: '产假',
    FAMILY_PLANNING: '计划生育假',
    HOME_VISIT: '探亲假',
    WORK_INJURY: '工伤假',
  }
  return map[type] || (type || '')
}

function statusClass(status) {
  const map = {
    REST: 'is-rest',
    WORK: 'is-work',
    TRAVEL: 'is-travel',
    LEAVE: 'is-leave',
  }
  return map[status] || ''
}

function statusIcon(status) {
  const map = {
    REST: 'i-material-symbols:free-breakfast-outline-rounded',
    WORK: 'i-material-symbols:work-outline-rounded',
    TRAVEL: 'i-material-symbols:business-center-outline-rounded',
    LEAVE: 'i-material-symbols:person-outline-rounded',
  }
  return map[status] || 'i-material-symbols:calendar-month-outline-rounded'
}

function dayNumber(yyyyMMdd) {
  if (!yyyyMMdd) return ''
  return String(yyyyMMdd).slice(-2)
}

function dayAriaLabel(day) {
  if (!day?.date)
    return '日期'
  const d = dayNumber(day.date)
  const t = statusText(day.status)
  const extra = day.status === 'LEAVE'
    ? leaveTypeText(day.leaveType)
    : (day.name || '')
  const state = isSubmitted.value ? '本月已提交，不可修改。' : '按回车或空格可切换。'
  return `${year.value}年${month.value}月${d}日，${t}${extra ? '，' + extra : '。'}${state}`
}

function onCardKeydown(e, day) {
  if (e.key !== 'Enter' && e.key !== ' ')
    return
  e.preventDefault()
  if (isSubmitted.value)
    return
  handleDayClick(day)
}

async function loadView() {
  loading.value = true
  try {
    const res = await getMonthView(year.value, month.value)
    monthData.value = res.data
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '加载失败')
  }
  finally {
    loading.value = false
  }
}

function handleReload() {
  loadView()
}

async function handleOneClickFill() {
  oneClickLoading.value = true
  try {
    await oneClickFillMonth(year.value, month.value)
    window.$message?.success('已创建本月填报单')
    await loadView()
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '操作失败')
  }
  finally {
    oneClickLoading.value = false
  }
}

async function handleSubmit() {
  submitLoading.value = true
  try {
    await submitMonth(year.value, month.value)
    window.$message?.success('提交成功')
    await loadView()
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '提交失败')
  }
  finally {
    submitLoading.value = false
  }
}

async function handleDayClick(day) {
  if (!day?.date) return
  if (isSubmitted.value) {
    window.$message?.warning('本月已提交，无法修改')
    return
  }

  // 后端切换规则：默认 -> TRAVEL -> LEAVE -> 默认
  // 这里做一次前端预判：从 TRAVEL 切到 LEAVE 时弹出请假类型选择
  if (day.status === 'TRAVEL') {
    leaveModal.show = true
    leaveModal.date = day.date
    leaveModal.leaveType = null
    leaveModal.remark = day.remark || ''
    return
  }
  await doToggle(day.date, null, day.remark || '')
}

async function confirmLeave() {
  if (!leaveModal.date) return
  if (!leaveModal.leaveType) {
    window.$message?.warning('请选择请假类型')
    return
  }
  leaveModal.loading = true
  try {
    await doToggle(leaveModal.date, leaveModal.leaveType, leaveModal.remark)
    leaveModal.show = false
  }
  finally {
    leaveModal.loading = false
  }
}

async function doToggle(date, leaveType, remark) {
  try {
    const res = await toggleDay(year.value, month.value, { date, leaveType, remark })
    const updated = res.data
    // 就地更新
    if (monthData.value?.days?.length) {
      const idx = monthData.value.days.findIndex(d => d.date === updated.date)
      if (idx >= 0) monthData.value.days[idx] = { ...monthData.value.days[idx], ...updated }
    }
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '操作失败')
    throw e
  }
}

onMounted(() => {
  loadView()
})
</script>

<style scoped>
.dept-attendance-page {
  min-height: 100%;
}

.attendance-card :deep(.n-card__content) {
  padding-top: 4px;
}

.card-head {
  margin-bottom: 16px;
  padding-bottom: 4px;
  border-bottom: 1px solid #e2e8f0;
}

.card-title {
  margin: 0 0 4px;
  font-size: 1.25rem;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: 0.02em;
}

.card-sub {
  margin: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.status-tag {
  margin-left: 2px;
  vertical-align: middle;
}

.card-hint {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.card-hint__dot {
  width: 6px;
  height: 6px;
  margin-top: 5px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #3b82f6;
  opacity: 0.85;
}

.toolbar {
  margin-bottom: 14px;
}

.submitted-alert {
  margin-bottom: 14px;
  border-radius: 8px;
}

.attendance-spin {
  min-height: 320px;
}

.cal-body {
  width: 100%;
  min-height: 320px;
}

.cal-placeholder {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #e2e8f0;
  border-radius: 10px;
  background: #fafbfc;
}

.cal-skeleton {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.skeleton-hd {
  height: 40px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: sk 1.1s ease-in-out infinite;
}

.skeleton-g {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
}

.skeleton-cell {
  min-height: 88px;
  border-radius: 10px;
  background: #f1f5f9;
  animation: sk 1.1s ease-in-out infinite;
}

.skeleton-hd,
.skeleton-cell {
  background-image: linear-gradient(90deg, #f1f5f9 0%, #e8eef5 50%, #f1f5f9 100%);
  background-size: 200% 100%;
}

@keyframes sk {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-hd,
  .skeleton-cell {
    animation: none;
    background: #f1f5f9;
  }
}

.calendar {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.week-cell {
  padding: 10px 6px;
  text-align: center;
  font-weight: 600;
  color: #334155;
  font-size: 13px;
  letter-spacing: 0.02em;
}

.week-cell--end {
  color: #0f766e;
  background: rgba(15, 118, 110, 0.04);
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
  min-height: 360px;
  background: #fff;
  transition: opacity 0.2s ease;
}

.grid--dim {
  opacity: 0.9;
  pointer-events: none;
  user-select: none;
}

.grid-cell {
  min-height: 96px;
}

.empty-cell {
  min-height: 96px;
  border-radius: 8px;
  background: #fafafa;
  border: 1px dashed #e8e8e8;
}

.day-card {
  min-height: 96px;
  height: 100%;
  box-sizing: border-box;
  border-radius: 10px;
  border: 1px solid transparent;
  padding: 8px 10px 6px;
  cursor: pointer;
  transition:
    transform 0.12s ease,
    box-shadow 0.12s ease,
    border-color 0.12s ease;
  user-select: none;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

@media (hover: hover) and (pointer: fine) {
  .day-card:hover:not(.is-locked) {
    transform: translateY(-1px);
    box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  }
}

@media (prefers-reduced-motion: reduce) {
  .day-card {
    transition: none;
  }
}

.day-card.is-locked {
  cursor: not-allowed;
  filter: grayscale(0.12);
  opacity: 0.9;
}

.day-card:focus {
  outline: none;
}

.day-card:focus-visible {
  outline: 2px solid #3b82f6;
  outline-offset: 2px;
  z-index: 1;
}

.day-top {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.day-icon {
  flex-shrink: 0;
  line-height: 1;
}

.day-icon i {
  font-size: 20px;
  line-height: 1;
  display: block;
}

.day-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  text-align: right;
  gap: 2px;
}

.day-label {
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.day-num {
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  opacity: 0.92;
}

.day-sub {
  font-size: 11px;
  line-height: 1.3;
  font-weight: 600;
  text-align: right;
  margin-top: 2px;
  padding-left: 6px;
}

.day-sub--pill {
  display: inline-block;
  margin-left: auto;
  max-width: 100%;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  font-weight: 600;
  letter-spacing: 0.02em;
}

.day-sub--muted {
  color: inherit;
  opacity: 0.7;
  font-weight: 500;
}

/* 状态配色：尽量贴近你截图 */
.day-card.is-rest {
  background: #e8f7ea;
  color: #166534;
  border-color: rgba(22, 101, 52, 0.12);
}

.day-card.is-work {
  background: #eaf1ff;
  color: #1d4ed8;
  border-color: rgba(29, 78, 216, 0.12);
}

.day-card.is-travel {
  background: #ffecef;
  color: #be123c;
  border-color: rgba(190, 18, 60, 0.12);
}

.day-card.is-leave {
  background: #3f4d6a;
  color: #fff;
  border-color: rgba(255, 255, 255, 0.16);
}

.dark .card-head {
  border-bottom-color: #334155;
}
.dark .card-title {
  color: #f1f5f9;
}
.dark .card-sub {
  color: #94a3b8;
}
.dark .card-hint {
  color: #94a3b8;
}
.dark .cal-placeholder {
  border-color: #334155;
  background: #0f172a;
}
.dark .cal-skeleton {
  border-color: #334155;
  background: #0f172a;
}
.dark .calendar {
  border-color: #334155;
}
.dark .week-header {
  background: #0b1220;
  border-bottom-color: #334155;
}
.dark .week-cell {
  color: #cbd5e1;
}
.dark .week-cell--end {
  color: #5eead4;
  background: rgba(45, 212, 191, 0.06);
}
.dark .grid {
  background: #0f172a;
}
.dark .empty-cell {
  background: rgba(15, 23, 42, 0.6);
  border-color: #334155;
}
@media (hover: hover) and (pointer: fine) {
  .dark .day-card:hover:not(.is-locked) {
    box-shadow: 0 10px 22px rgba(0, 0, 0, 0.35);
  }
}
.dark .day-card.is-rest {
  background: rgba(34, 197, 94, 0.12);
  color: #86efac;
}
.dark .day-card.is-work {
  background: rgba(59, 130, 246, 0.12);
  color: #93c5fd;
}
.dark .day-card.is-travel {
  background: rgba(244, 63, 94, 0.14);
  color: #fecdd3;
}
.dark .day-card.is-leave {
  background: rgba(148, 163, 184, 0.22);
  color: #e2e8f0;
}
</style>

