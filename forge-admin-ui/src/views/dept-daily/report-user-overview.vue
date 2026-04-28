<template>
  <div class="p-4 dept-daily-overview-page">
    <n-card title="月报统览（员工月报统计）" :bordered="false">
      <div class="toolbar">
        <n-space justify="space-between" align="center" wrap>
          <n-space align="center" wrap>
            <n-select v-model:value="year" style="width: 130px" :options="yearOptions" @update:value="reloadUsers" />
            <n-select v-model:value="month" style="width: 110px" :options="monthOptions" @update:value="reloadUsers" />
            <n-select
              v-model:value="employeeType"
              style="width: 160px"
              clearable
              :options="employeeTypeOptions"
              placeholder="员工类型"
              @update:value="reloadUsers"
            />
            <n-input
              v-model:value="userKeyword"
              clearable
              style="width: 260px"
              placeholder="搜索姓名/用户名"
              @keyup.enter="reloadUsers"
              @clear="reloadUsers"
            />
            <n-select
              v-model:value="userStatus"
              clearable
              style="width: 150px"
              :options="statusOptions"
              placeholder="状态"
              @update:value="reloadUsers"
            />
          </n-space>
          <n-space>
            <n-button tertiary :loading="refreshing" @click="refreshWorkReportFill">
              刷新未填报状态
            </n-button>
            <n-button secondary :loading="loadingUser" @click="reloadUsers">
              刷新
            </n-button>
          </n-space>
        </n-space>
      </div>

      <n-data-table
        :columns="userColumns"
        :data="userRows"
        :loading="loadingUser"
        :pagination="userPager"
        :row-key="r => r.userId"
        remote
        @update:page="onUserPage"
        @update:page-size="onUserPageSize"
      />
    </n-card>

    <n-modal v-model:show="detail.show" preset="card" :title="detail.title" style="width: 860px">
      <n-spin :show="detail.loading">
        <n-data-table :columns="detailColumns" :data="detail.rows" :row-key="r => r.id" />
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="detail.show = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NButton, NTag } from 'naive-ui'
import { pageUserReportStat, refreshFillState } from '@/api/dept-daily/overview'
import { pageUserMonthItemByUser } from '@/api/dept-daily/report'

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const reportYm = computed(() => `${year.value}-${String(month.value).padStart(2, '0')}`)

const employeeType = ref(null)
const employeeTypeOptions = [
  { label: '正式员工', value: 1 },
  { label: '劳务派遣', value: 2 },
]

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return Array.from({ length: 7 }, (_, i) => ({ label: `${y - 3 + i}年`, value: y - 3 + i }))
})
const monthOptions = computed(() => Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })))

const loadingUser = ref(false)
const userRows = ref([])
const userKeyword = ref('')
const userStatus = ref(null)
const statusOptions = [
  { label: '未填报', value: 'NONE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
]
const userPager = reactive({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: true, pageSizes: [10, 20, 50] })

const refreshing = ref(false)

function tagTypeByStatus(s) {
  if (s === 'SUBMITTED') return 'success'
  if (s === 'DRAFT') return 'warning'
  return 'default'
}
function tagTextByStatus(s) {
  if (s === 'SUBMITTED') return '已提交'
  if (s === 'DRAFT') return '草稿'
  return '未填报'
}
function empText(v) {
  if (v === 1) return '正式员工'
  if (v === 2) return '劳务派遣'
  return '-'
}

const userColumns = [
  { title: '员工姓名', key: 'realName', width: 160, render: r => r.realName || r.username || '-' },
  { title: '员工类型', key: 'employeeType', width: 120, render: r => empText(r.employeeType) },
  { title: '本月项目数', key: 'projectCount', width: 110 },
  { title: '状态', key: 'status', width: 100, render: r => h(NTag, { size: 'small', type: tagTypeByStatus(r.status) }, { default: () => tagTextByStatus(r.status) }) },
  { title: '操作', key: 'actions', width: 140, render: r => h(NButton, { size: 'small', secondary: true, onClick: () => openUserDetail(r) }, { default: () => '展开项目详情' }) },
]

const detail = reactive({ show: false, title: '', loading: false, rows: [] })
const detailColumns = [
  { title: '项目ID', key: 'projectId', width: 90 },
  { title: '进展情况', key: 'progressText', minWidth: 520, ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 100, render: r => h(NTag, { size: 'small', type: tagTypeByStatus(r.status) }, { default: () => tagTextByStatus(r.status) }) },
]

async function loadUsers() {
  loadingUser.value = true
  try {
    const res = await pageUserReportStat({
      pageNum: userPager.page,
      pageSize: userPager.pageSize,
      reportYm: reportYm.value,
      employeeType: employeeType.value ?? undefined,
      status: userStatus.value ?? undefined,
      keyword: userKeyword.value?.trim() || undefined,
    })
    const page = res.data
    userRows.value = page?.records || []
    userPager.itemCount = page?.total || 0
  } catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    loadingUser.value = false
  }
}

function reloadUsers() {
  userPager.page = 1
  loadUsers()
}
function onUserPage(p) {
  userPager.page = p
  loadUsers()
}
function onUserPageSize(ps) {
  userPager.pageSize = ps
  userPager.page = 1
  loadUsers()
}

async function refreshWorkReportFill() {
  refreshing.value = true
  try {
    await refreshFillState({ module: 'WORK_REPORT', startYm: reportYm.value, endYm: reportYm.value, employeeType: employeeType.value ?? null })
    window.$message?.success('刷新成功')
    await loadUsers()
  } catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

async function openUserDetail(row) {
  detail.show = true
  detail.title = `项目详情 - ${row.realName || row.username || ''}（${reportYm.value}）`
  detail.loading = true
  try {
    const res = await pageUserMonthItemByUser({ reportYm: reportYm.value, userId: row.userId, pageNum: 1, pageSize: 200 })
    detail.rows = res.data?.records || []
  } catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    detail.loading = false
  }
}

onMounted(() => {
  reloadUsers()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>

