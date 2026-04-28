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
          点格：出勤态走 出差→请假→默认，休日先记成出勤。假种在右下角；请假时除角标外点格恢复默认。周末默认可休，调休/补班以日历为准。
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
              type="success"
              :disabled="isFutureMonth"
              :loading="submitLoading"
              :title="isFutureMonth ? '仅可提交至当前月' : undefined"
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
        仍可修改日历；修改后再次点击「提交本月」将更新提交时间。
      </n-alert>

      <n-spin
        :show="loading"
        :description="loading && !monthData ? '正在加载月历…' : undefined"
        class="attendance-spin"
      >
        <div class="cal-body">
        <div class="cal-body__row">
          <n-button
            quaternary
            class="cal-nav-btn"
            :disabled="loading"
            aria-label="上一月"
            title="上一月"
            @click="shiftMonth(-1)"
          >
            <i class="i-material-symbols:chevron-left cal-nav-ico" aria-hidden="true" />
          </n-button>
          <div class="cal-body__frame">
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

          <div class="grid">
            <div v-for="cell in cells" :key="cell.key" class="grid-cell">
              <div
                v-if="cell.type === 'day'"
                class="day-card"
                :class="[
                  statusClass(cell.day.status),
                  { 'is-compensatory-day': cell.day.compensatoryWorkday },
                ]"
                role="button"
                tabindex="0"
                :aria-label="dayAriaLabel(cell.day)"
                @click="onDayCardClick($event, cell.day)"
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

                <div
                  v-if="cell.day.status === 'LEAVE'"
                  class="day-leave-corner day-leave-corner--clickable"
                  title="点击编辑假种与备注"
                  @click.stop="onLeaveCornerClick(cell.day)"
                >
                  <span class="day-sub day-sub--pill day-leave-pill">
                    {{ leaveTypeText(cell.day.leaveType || 'ANNUAL') }}
                  </span>
                </div>
                <div
                  v-else-if="calendarSubline(cell.day)"
                  class="day-sub day-sub--muted"
                  :class="{ 'day-sub--tx': cell.day.compensatoryWorkday }"
                >
                  {{ calendarSubline(cell.day) }}
                </div>
              </div>

              <div v-else class="empty-cell" />
            </div>
          </div>
        </div>
          </div>
          <n-button
            quaternary
            class="cal-nav-btn"
            :disabled="loading"
            aria-label="下一月"
            title="下一月"
            @click="shiftMonth(1)"
          >
            <i class="i-material-symbols:chevron-right cal-nav-ico" aria-hidden="true" />
          </n-button>
        </div>
        </div>
      </n-spin>
    </n-card>

    <n-modal
      v-model:show="leaveModal.show"
      preset="card"
      title="编辑请假"
      style="width: 440px"
      @update:show="onLeaveModalShow"
    >
      <n-form label-placement="left" label-width="90">
        <n-form-item label="请假类型">
          <n-select
            v-model:value="leaveModal.leaveType"
            :options="leaveTypeOptions"
            placeholder="请选择"
            :consistent-menu-width="false"
          />
        </n-form-item>
        <n-form-item label="备注">
          <n-input
            v-model:value="leaveModal.remark"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            show-count
            :maxlength="200"
            placeholder="选填，如请假事由、流程单号等"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button :disabled="leaveModal.loading" @click="cancelLeaveModal">
            取消
          </n-button>
          <n-button type="primary" :loading="leaveModal.loading" @click="confirmLeaveModal">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getMonthView, submitMonth, toggleDay, updateAttendanceDay } from '@/api/dept-daily/attendance'

defineOptions({ name: 'DeptDailyAttendance' })

const loading = ref(true)
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
  /** 关弹窗未确定时刷新格内展示 */
  reloadOnClose: false,
})

const yearOptions = computed(() => {
  const y = now.getFullYear()
  const sel = year.value ?? y
  const from = Math.min(y - 2, sel)
  const to = Math.max(y + 2, sel)
  return Array.from({ length: to - from + 1 }, (_, i) => {
    const v = from + i
    return { label: `${v}年`, value: v }
  })
})

const monthOptions = computed(() => Array.from({ length: 12 }).map((_, i) => ({ label: `${i + 1}月`, value: i + 1 })))

const isSubmitted = computed(() => monthData.value?.status === 'SUBMITTED')

/** 所选年月晚于当前年月时不可提交 */
const isFutureMonth = computed(() => {
  if (!year.value || !month.value)
    return false
  const d = new Date()
  const cy = d.getFullYear()
  const cm = d.getMonth() + 1
  if (year.value > cy)
    return true
  if (year.value < cy)
    return false
  return month.value > cm
})

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

/**
 * 节假日/调休副标题。出勤(WORK)与工作日表现一致：不展示仅「周几/噪声」等名称，避免周末记出勤时格子里多一行字。
 * 调休补班日仍显示名称或「调休·须出勤」。
 */
function calendarSubline(day) {
  if (!day || day.status === 'LEAVE')
    return ''
  if (day.status === 'WORK') {
    if (day.compensatoryWorkday) {
      if (day.name)
        return day.name
      return '调休·须出勤'
    }
    return ''
  }
  if (day.name)
    return day.name
  if (day.compensatoryWorkday)
    return '调休·须出勤'
  return ''
}

function dayAriaLabel(day) {
  if (!day?.date)
    return '日期'
  const d = dayNumber(day.date)
  const t = statusText(day.status)
  const extra = day.status === 'LEAVE'
    ? leaveTypeText(day.leaveType || 'ANNUAL')
    : (calendarSubline(day) || '')
  const cal = day.compensatoryWorkday ? '调休补班日，' : ''
  const state = isSubmitted.value
    ? '本月已提交，仍可修改。'
    : (day.status === 'LEAVE'
        ? '按回车或空格恢复为默认，右下角可编辑假种与备注。'
        : (day.status === 'REST'
            ? '按回车或空格可记为出勤。'
            : '按回车或空格切换状态。'))
  return `${year.value}年${month.value}月${d}日，${cal}${t}${extra ? '，' + extra : '。'}${state}`
}

function onCardKeydown(e, day) {
  if (e.key !== 'Enter' && e.key !== ' ')
    return
  e.preventDefault()
  if (day.status === 'LEAVE') {
    void revertDayToDefault(day)
    return
  }
  void advanceDayState(day)
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

/** 与顶部年月联动，用于左右切换月 */
function shiftMonth(delta) {
  let y = year.value
  let m = month.value
  m += delta
  while (m < 1) {
    m += 12
    y -= 1
  }
  while (m > 12) {
    m -= 12
    y += 1
  }
  year.value = y
  month.value = m
  loadView()
}

async function handleSubmit() {
  if (isFutureMonth.value) {
    window.$message?.warning('不能提交未到来的月份')
    return
  }
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

function getDayByDate(date) {
  return monthData.value?.days?.find(d => d.date === date) || null
}

function onDayCardClick(e, day) {
  if (!day?.date) return
  if (e?.target?.closest?.('.day-leave-corner'))
    return
  if (day.status === 'LEAVE') {
    void revertDayToDefault(day)
    return
  }
  void advanceDayState(day)
}

/**
 * 非请假态步进：休息日先记为出勤，再点走 toggle；出勤/已覆盖出勤后按「出差 → 请假 → 默认」循环。
 * 出差→请假时带默认年假，不调弹窗。
 */
function advanceDayState(day) {
  if (day.status === 'TRAVEL')
    return doToggle(day.date, 'ANNUAL', null)
  if (day.status === 'REST')
    return putDayMerge(day, { status: 'WORK', leaveType: null, remark: null })
  return doToggle(day.date, null, day.remark || null)
}

/** 从请假回到日历默认(出勤/休) */
async function revertDayToDefault(day) {
  return doToggle(day.date, null, null)
}

function onLeaveCornerClick(day) {
  if (!day?.date) return
  openChangeLeaveTypeModal(day)
}

function openChangeLeaveTypeModal(day) {
  leaveModal.date = day.date
  leaveModal.leaveType = day.leaveType || 'ANNUAL'
  leaveModal.remark = String(day.remark ?? '')
  leaveModal.reloadOnClose = true
  leaveModal.show = true
}

function onLeaveModalShow(show) {
  if (show) return
  if (leaveModal.reloadOnClose)
    void loadView()
  leaveModal.reloadOnClose = false
}

function cancelLeaveModal() {
  leaveModal.show = false
}

async function confirmLeaveModal() {
  if (!leaveModal.date) return
  const d = getDayByDate(leaveModal.date)
  if (!d) {
    leaveModal.show = false
    return
  }
  if (!leaveModal.leaveType) {
    window.$message?.warning('请选择请假类型')
    return
  }
  const lt = leaveModal.leaveType
  leaveModal.loading = true
  try {
    const ok = await putDayMerge(d, { status: 'LEAVE', leaveType: lt, remark: leaveModal.remark })
    if (ok) {
      leaveModal.reloadOnClose = false
      leaveModal.show = false
    }
  }
  finally {
    leaveModal.loading = false
  }
}

async function putDayMerge(day, { status, leaveType, remark }) {
  if (!day?.date) return false
  const remarkTrim = remark == null || remark === '' ? '' : String(remark).trim()
  const body = {
    date: day.date,
    status,
    leaveType: status === 'LEAVE' ? leaveType : null,
    remark: remarkTrim.length ? remarkTrim : null,
  }
  try {
    await updateAttendanceDay(year.value, month.value, body)
    const idx = monthData.value?.days?.findIndex(x => x.date === day.date)
    if (idx >= 0) {
      const cur = monthData.value.days[idx]
      const next = {
        ...cur,
        status,
        leaveType: status === 'LEAVE' ? leaveType : null,
        remark: body.remark,
      }
      monthData.value.days[idx] = next
    }
    return true
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '操作失败')
    await loadView()
    return false
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

.cal-body__row {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 100%;
}

.cal-body__frame {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 1688px;
  width: 100%;
}

.cal-nav-btn {
  flex-shrink: 0;
  padding: 4px 2px;
  border-radius: 10px;
}

.cal-nav-ico {
  display: block;
  font-size: 28px;
  line-height: 1;
  opacity: 0.8;
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
  .day-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  }
}

@media (prefers-reduced-motion: reduce) {
  .day-card {
    transition: none;
  }
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

/* 调休补班日：在「出勤」态下用左侧色条与平日区分
.day-card.is-work.is-compensatory-day {
  border-color: rgba(217, 119, 6, 0.35);
  box-shadow: inset 3px 0 0 0 #d97706;
} */

.day-sub--tx {
  color: #b45309;
  font-weight: 600;
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

.day-leave-corner {
  margin-top: auto;
  width: 100%;
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  padding-top: 2px;
}

.day-leave-corner--clickable {
  cursor: pointer;
  border-radius: 8px;
  outline: none;
}

@media (hover: hover) and (pointer: fine) {
  .day-leave-corner--clickable:hover .day-leave-pill {
    background: rgba(255, 255, 255, 0.3);
  }
}

.day-leave-pill {
  text-align: right;
  max-width: 100%;
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
.dark .cal-nav-ico {
  opacity: 0.95;
  color: #cbd5e1;
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
  .dark .day-card:hover {
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
.dark .day-card.is-work.is-compensatory-day {
  border-color: rgba(251, 191, 36, 0.4);
  box-shadow: inset 3px 0 0 0 #fbbf24;
}
.dark .day-sub--tx {
  color: #fcd34d;
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

