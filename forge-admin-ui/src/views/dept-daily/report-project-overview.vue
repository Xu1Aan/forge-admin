<template>
  <div class="p-4 dept-daily-overview-page">
    <n-card title="项目统览（项目进展表）" :bordered="false">
      <div class="toolbar">
        <n-space justify="space-between" align="center" wrap>
          <n-space align="center" wrap>
            <n-select v-model:value="year" style="width: 130px" :options="yearOptions" @update:value="reloadProjects" />
            <n-select v-model:value="month" style="width: 110px" :options="monthOptions" @update:value="reloadProjects" />
            <n-input
              v-model:value="projectKeyword"
              clearable
              style="width: 280px"
              placeholder="搜索项目名/负责人"
              @keyup.enter="reloadProjects"
              @clear="reloadProjects"
            />
          </n-space>
          <n-space>
            <n-button secondary :loading="loadingProject" @click="reloadProjects">
              刷新
            </n-button>
          </n-space>
        </n-space>
      </div>

      <n-data-table
        :columns="projectColumns"
        :data="projectRows"
        :loading="loadingProject"
        :pagination="projectPager"
        :row-key="r => r.projectId"
        remote
        @update:page="onProjectPage"
        @update:page-size="onProjectPageSize"
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
import { pageProjectProgress } from '@/api/dept-daily/overview'
import { pageProjectMonth } from '@/api/dept-daily/report'

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const reportYm = computed(() => `${year.value}-${String(month.value).padStart(2, '0')}`)

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return Array.from({ length: 7 }, (_, i) => ({ label: `${y - 3 + i}年`, value: y - 3 + i }))
})
const monthOptions = computed(() => Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })))

const loadingProject = ref(false)
const projectRows = ref([])
const projectKeyword = ref('')
const projectPager = reactive({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: true, pageSizes: [10, 20, 50] })

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

const detail = reactive({ show: false, title: '', loading: false, rows: [] })
const detailColumns = [
  { title: '项目ID', key: 'projectId', width: 90 },
  { title: '进展情况', key: 'progressText', minWidth: 520, ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 100, render: r => h(NTag, { size: 'small', type: tagTypeByStatus(r.status) }, { default: () => tagTextByStatus(r.status) }) },
]

const projectColumns = [
  { title: '项目名', key: 'projectName', minWidth: 240 },
  { title: '项目负责人', key: 'leaderName', width: 140 },
  { title: '进展情况', key: 'summaryText', minWidth: 320, ellipsis: { tooltip: true }, render: r => (r.summaryText || '') },
  { title: '状态', key: 'status', width: 100, render: r => h(NTag, { size: 'small', type: tagTypeByStatus(r.status) }, { default: () => tagTextByStatus(r.status) }) },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: r => h(NButton, { size: 'small', secondary: true, disabled: !r.reportId, onClick: () => openProjectReportDetail(r) }, { default: () => '详情' }),
  },
]

async function loadProjects() {
  loadingProject.value = true
  try {
    const res = await pageProjectProgress({
      pageNum: projectPager.page,
      pageSize: projectPager.pageSize,
      reportYm: reportYm.value,
      keyword: projectKeyword.value?.trim() || undefined,
    })
    const page = res.data
    projectRows.value = page?.records || []
    projectPager.itemCount = page?.total || 0
  } catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    loadingProject.value = false
  }
}

function reloadProjects() {
  projectPager.page = 1
  loadProjects()
}
function onProjectPage(p) {
  projectPager.page = p
  loadProjects()
}
function onProjectPageSize(ps) {
  projectPager.pageSize = ps
  projectPager.page = 1
  loadProjects()
}

async function openProjectReportDetail(row) {
  detail.show = true
  detail.title = `项目月报详情 - ${row.projectName}（${reportYm.value}）`
  detail.loading = true
  try {
    const res = await pageProjectMonth({ reportYm: reportYm.value, projectId: row.projectId, pageNum: 1, pageSize: 1 })
    const r = (res.data?.records || [])[0]
    detail.rows = r ? [{ id: r.id, projectId: row.projectId, progressText: r.summaryText, status: r.status }] : []
  } catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    detail.loading = false
  }
}

onMounted(() => {
  reloadProjects()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>

