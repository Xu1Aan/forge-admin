<template>
  <div class="p-4 dept-daily-overview-page">
    <n-card title="考勤一览表" :bordered="false">
      <div class="toolbar">
        <n-space justify="space-between" align="center" wrap>
          <n-space align="center" wrap>
            <n-select v-model:value="year" style="width: 130px" :options="yearOptions" @update:value="reload" />
            <n-select v-model:value="month" style="width: 110px" :options="monthOptions" @update:value="reload" />
            <n-select
              v-model:value="employeeType"
              style="width: 160px"
              clearable
              :options="employeeTypeOptions"
              placeholder="员工类型"
              @update:value="reload"
            />
            <n-input v-model:value="keyword" style="width: 220px" clearable placeholder="搜索姓名/用户名" @keyup.enter="reload" @clear="reload" />
            <n-button secondary :loading="loading" @click="reload">
              刷新
            </n-button>
          </n-space>
          <n-space>
            <n-button secondary :loading="exporting" @click="exportMonthExcel">
              导出当月
            </n-button>
            <n-button secondary :loading="exporting" @click="exportYearExcel">
              导出全年
            </n-button>
            <n-button tertiary @click="openOrderSetting">
              导出顺序设置
            </n-button>
            <n-button tertiary @click="goUnfilled">
              未填报人员
            </n-button>
          </n-space>
        </n-space>
      </div>

      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="r => r.userId"
        remote
        @update:page="onPage"
        @update:page-size="onPageSize"
      />
    </n-card>

    <n-modal v-model:show="orderModal.show" preset="card" style="width: min(720px, 92vw)" title="考勤导出人员顺序">
      <n-space vertical size="small">
        <div class="text-sm text-gray-500">
          每行一个姓名，导出时会按该顺序排序；未在列表中的人员会排在最后。
        </div>
        <n-input
          v-model:value="orderModal.text"
          type="textarea"
          :autosize="{ minRows: 10, maxRows: 22 }"
          placeholder="例如：&#10;姓名1&#10;姓名2&#10;姓名3"
        />
        <n-space justify="end">
          <n-button @click="orderModal.show = false">
            取消
          </n-button>
          <n-button type="primary" :loading="orderModal.saving" @click="saveOrderSetting">
            保存
          </n-button>
        </n-space>
      </n-space>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NTag } from 'naive-ui'
import { exportAttendance, getDeptDailyOverviewSetting, pageAttendanceMonthTable, saveDeptDailyOverviewSetting } from '@/api/dept-daily/overview'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const deptId = computed(() => userStore.userInfo?.mainOrgId || null)

const router = useRouter()
function goUnfilled() {
  router.push('/dept-daily/attendance-unfilled')
}

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const employeeType = ref(null)
const keyword = ref('')

const loading = ref(false)
const exporting = ref(false)
const rows = ref([])

const orderModal = reactive({
  show: false,
  text: '',
  saving: false,
})

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return Array.from({ length: 7 }, (_, i) => ({ label: `${y - 3 + i}年`, value: y - 3 + i }))
})
const monthOptions = computed(() => Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })))
const employeeTypeOptions = [
  { label: '正式员工', value: 1 },
  { label: '劳务派遣', value: 2 },
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

function empText(v) {
  if (v === 1) return '正式员工'
  if (v === 2) return '劳务派遣'
  return '-'
}
function statusTagType(s) {
  if (s === 'SUBMITTED') return 'success'
  if (s === 'DRAFT') return 'warning'
  return 'default'
}
function statusText(s) {
  if (s === 'SUBMITTED') return '已提交'
  if (s === 'DRAFT') return '草稿'
  return '未填报'
}
function dayText(s) {
  if (s === 'WORK') return '出'
  if (s === 'REST') return '休'
  if (s === 'TRAVEL') return '差'
  if (s === 'LEAVE') return '假'
  return ''
}
function dayTagType(s) {
  if (s === 'WORK') return 'success'
  if (s === 'TRAVEL') return 'info'
  if (s === 'LEAVE') return 'warning'
  return 'default'
}

const columns = computed(() => {
  const base = [
    { title: '员工姓名', key: 'realName', fixed: 'left', width: 150, render: r => r.realName || r.username || '-' },
    { title: '员工类型', key: 'employeeType', fixed: 'left', width: 110, render: r => empText(r.employeeType) },
    { title: '填报状态', key: 'sheetStatus', fixed: 'left', width: 110, render: r => h(NTag, { size: 'small', type: statusTagType(r.sheetStatus) }, { default: () => statusText(r.sheetStatus) }) },
    { title: '出勤', key: 'workDays', fixed: 'left', width: 70 },
    { title: '出差', key: 'travelDays', fixed: 'left', width: 70 },
    { title: '请假', key: 'leaveDays', fixed: 'left', width: 70 },
    { title: '休息', key: 'restDays', fixed: 'left', width: 70 },
  ]
  const days = []
  for (let d = 1; d <= 31; d++) {
    days.push({
      title: String(d),
      key: `d${d}`,
      width: 48,
      align: 'center',
      render: (r) => {
        const s = (r.days || [])[d - 1] || ''
        const t = dayText(s)
        if (!t) return ''
        return h(NTag, { size: 'small', type: dayTagType(s), bordered: false }, { default: () => t })
      },
    })
  }
  return [...base, ...days]
})

async function load() {
  loading.value = true
  try {
    const res = await pageAttendanceMonthTable({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      year: year.value,
      month: month.value,
      deptId: deptId.value || undefined,
      employeeType: employeeType.value ?? undefined,
      keyword: keyword.value?.trim() || undefined,
    })
    const page = res.data
    rows.value = page?.records || []
    pagination.itemCount = page?.total || 0
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '加载失败')
  }
  finally {
    loading.value = false
  }
}

async function exportYearExcel() {
  exporting.value = true
  try {
    const blob = await exportAttendance({
      year: year.value,
      deptId: deptId.value || undefined,
      employeeType: employeeType.value ?? undefined,
      keyword: keyword.value?.trim() || undefined,
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${year.value}年度考勤表.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.message || '导出失败')
  }
  finally {
    exporting.value = false
  }
}

async function exportMonthExcel() {
  exporting.value = true
  try {
    const blob = await exportAttendance({
      year: year.value,
      month: month.value,
      deptId: deptId.value || undefined,
      employeeType: employeeType.value ?? undefined,
      keyword: keyword.value?.trim() || undefined,
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${year.value}-${String(month.value).padStart(2, '0')}考勤表.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.message || '导出失败')
  }
  finally {
    exporting.value = false
  }
}

async function openOrderSetting() {
  orderModal.show = true
  try {
    const res = await getDeptDailyOverviewSetting({ deptId: deptId.value || undefined, employeeType: employeeType.value ?? undefined })
    orderModal.text = res?.data?.attendanceExportOrder || ''
  }
  catch (e) {
    console.warn(e)
    orderModal.text = ''
  }
}

async function saveOrderSetting() {
  orderModal.saving = true
  try {
    await saveDeptDailyOverviewSetting({
      deptId: deptId.value || null,
      officeId: null,
      employeeType: employeeType.value ?? null,
      attendanceExportOrder: orderModal.text || null,
    })
    window.$message?.success('保存成功')
    orderModal.show = false
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.message || '保存失败')
  }
  finally {
    orderModal.saving = false
  }
}

function reload() {
  pagination.page = 1
  load()
}
function onPage(p) {
  pagination.page = p
  load()
}
function onPageSize(ps) {
  pagination.pageSize = ps
  pagination.page = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>

