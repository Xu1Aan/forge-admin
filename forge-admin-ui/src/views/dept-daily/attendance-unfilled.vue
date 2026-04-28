<template>
  <div class="p-4 dept-daily-overview-page">
    <n-card title="考勤统览（未填报人员）" :bordered="false">
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
            <n-button tertiary :loading="refreshing" @click="refreshFill">
              刷新未填报状态
            </n-button>
          </n-space>
        </n-space>
      </div>

      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="r => `${r.userId}-${r.ym}`"
        remote
        @update:page="onPage"
        @update:page-size="onPageSize"
      />
    </n-card>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NTag } from 'naive-ui'
import { getDeptDailyOverviewSetting, pageFillState, refreshFillState } from '@/api/dept-daily/overview'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const deptId = computed(() => userStore.userInfo?.mainOrgId || null)

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const employeeType = ref(null)
const keyword = ref('')

const loading = ref(false)
const refreshing = ref(false)
const rows = ref([])

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return Array.from({ length: 7 }, (_, i) => {
    const v = y - 3 + i
    return { label: `${v}年`, value: v }
  })
})
const monthOptions = computed(() => Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })))
const employeeTypeOptions = [
  { label: '正式员工', value: 1 },
  { label: '劳务派遣', value: 2 },
]

const ym = computed(() => `${year.value}-${String(month.value).padStart(2, '0')}`)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

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
function empText(v) {
  if (v === 1) return '正式员工'
  if (v === 2) return '劳务派遣'
  return '-'
}

const columns = [
  { title: '员工姓名', key: 'realName', width: 160, render: r => r.realName || r.username || '-' },
  { title: '员工类型', key: 'employeeType', width: 120, render: r => empText(r.employeeType) },
  { title: '未填报月份', key: 'ym', width: 110 },
  { title: '状态', key: 'status', width: 100, render: r => h(NTag, { size: 'small', type: statusTagType(r.status) }, { default: () => statusText(r.status) }) },
]

async function ensureFillStateReady() {
  try {
    const settingRes = await getDeptDailyOverviewSetting({ deptId: deptId.value || undefined })
    const startYm = settingRes?.data?.attendanceStartYm || null
    await refreshFillState({
      module: 'ATTENDANCE',
      deptId: deptId.value || null,
      startYm,
      endYm: ym.value,
      employeeType: employeeType.value ?? null,
    })
  }
  catch (e) {
    console.warn(e)
  }
}

async function load() {
  loading.value = true
  try {
    const res = await pageFillState({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      module: 'ATTENDANCE',
      ym: ym.value,
      deptId: deptId.value || undefined,
      employeeType: employeeType.value ?? undefined,
      status: 'NONE',
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

async function refreshFill() {
  refreshing.value = true
  try {
    await refreshFillState({
      module: 'ATTENDANCE',
      deptId: deptId.value || null,
      startYm: ym.value,
      endYm: ym.value,
      employeeType: employeeType.value ?? null,
    })
    window.$message?.success('刷新成功')
    await load()
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '刷新失败')
  }
  finally {
    refreshing.value = false
  }
}

onMounted(async () => {
  await ensureFillStateReady()
  await load()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>

