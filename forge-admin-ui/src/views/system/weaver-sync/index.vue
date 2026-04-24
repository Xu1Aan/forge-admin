<template>
  <div class="weaver-sync-page">
    <n-card
      :bordered="false"
      class="weaver-sync-card"
      :segmented="{ content: true, footer: 'soft' }"
    >
      <template #header>
        <div class="card-header">
          <i class="i-material-symbols:sync-rounded header-icon" />
          <span>泛微用户组织同步</span>
        </div>
      </template>

      <n-alert type="info" :show-icon="true" class="mb-4">
        全量拉取并差分同步到本系统（组织、用户、主部门）。请在后端
        <code>external.weaver</code> 中开启同步、配置
        <code>getUsersInfo</code> 地址与鉴权。点击后<strong>仅提交任务</strong>，接口立即返回；具体耗时取决于泛微与数据量，请在下表查看进度，页面可每 3 秒自动刷新。
      </n-alert>

      <n-space vertical size="large">
        <n-space align="center" wrap>
          <n-button
            type="primary"
            :loading="submitting"
            :disabled="submitting"
            @click="onSyncClick"
          >
            <template #icon>
              <i class="i-material-symbols:cloud-sync" />
            </template>
            开始全量同步
          </n-button>
          <n-text v-if="pendingBatchId" type="info" class="tip-text">
            已提交后台同步（批次 #{{ pendingBatchId }}）{{ pollHint }}
          </n-text>
          <n-text v-else depth="3" class="tip-text">
            仅请求本服务，由服务转发至泛微接口；不在浏览器保存泛微密钥。
          </n-text>
        </n-space>

        <n-card v-if="lastResult" size="small" :title="lastResult.status === 'running' ? '最近同步（进行中）' : '最近同步结果'" :bordered="true">
          <n-descriptions :column="2" label-placement="left" size="small" bordered>
            <n-descriptions-item label="批次ID">
              {{ lastResult.batchId }}
            </n-descriptions-item>
            <n-descriptions-item label="状态">
              <NTag v-bind="statusTagProps(lastResult.status)" size="small">
                {{ lastResult.status }}
              </NTag>
            </n-descriptions-item>
            <n-descriptions-item v-if="lastResult.status !== 'running'" label="拉取组织">
              {{ lastResult.fetchedOrgCount }}
            </n-descriptions-item>
            <n-descriptions-item v-if="lastResult.status !== 'running'" label="拉取人员">
              {{ lastResult.fetchedUserCount }}
            </n-descriptions-item>
            <n-descriptions-item v-if="lastResult.status !== 'running'" label="新增">
              {{ lastResult.insertedCount }}
            </n-descriptions-item>
            <n-descriptions-item v-if="lastResult.status !== 'running'" label="更新">
              {{ lastResult.updatedCount }}
            </n-descriptions-item>
            <n-descriptions-item v-if="lastResult.status !== 'running'" label="禁用">
              {{ lastResult.disabledCount }}
            </n-descriptions-item>
            <n-descriptions-item v-if="lastResult.status !== 'running'" label="跳过">
              {{ lastResult.skippedCount }}
            </n-descriptions-item>
          </n-descriptions>
          <n-text
            v-if="lastResult.status === 'running'"
            depth="3"
            class="mt-2 block"
            style="font-size: 13px"
          >
            任务在服务端后台执行，下方「同步批次」将自动更新；完成或失败后会提示。
          </n-text>
          <n-alert
            v-if="lastResult.status === 'partial'"
            class="mt-3"
            type="warning"
            :show-icon="true"
            title="部分成功或含跳过/告警"
          >
            请查看下方批次表中的本批次 <code>errorMessage</code> 字段，或根据「跳过」条数处理无工号/重复工号等数据问题。
          </n-alert>
        </n-card>
      </n-space>
    </n-card>

    <n-card
      :bordered="false"
      class="weaver-sync-card weaver-batches-card"
      title="同步批次"
      :segmented="{ content: true, footer: 'soft' }"
    >
      <template #header-extra>
        <n-button quaternary size="small" :loading="tableLoading" @click="loadBatches">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新
        </n-button>
      </template>
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="tableLoading"
        :bordered="false"
        :scroll-x="1200"
        :pagination="pagination"
        :row-key="(row) => row.id"
        remote
        size="small"
        striped
      />
    </n-card>
  </div>
</template>

<script setup>
import { NButton, NTag } from 'naive-ui'
import { h, onMounted, onUnmounted, ref } from 'vue'
import { runWeaverFullSync, pageWeaverSyncBatches } from '@/api/system/weaver-sync'
import { formatDateTime } from '@/utils'

defineOptions({ name: 'WeaverSync' })

const submitting = ref(false)
const lastResult = ref(null)
/** 当前关注中的批次（提交后轮询直到终态） */
const pendingBatchId = ref(null)
const pollHint = ref('，正在自动刷新进度…')
let pollTimer = null
const POLL_MS = 3000

const tableLoading = ref(false)
const tableData = ref([])

const pagination = ref({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: (info) => `共 ${info.itemCount} 条`,
  onChange: (page) => {
    pagination.value.page = page
    loadBatches()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.value.pageSize = pageSize
    pagination.value.page = 1
    loadBatches()
  },
})

function statusTagProps(status) {
  const s = String(status || '')
  if (s === 'success')
    return { type: 'success' }
  if (s === 'partial')
    return { type: 'warning' }
  if (s === 'failed' || s === 'running')
    return { type: s === 'failed' ? 'error' : 'default' }
  return { type: 'default' }
}

function onSyncClick() {
  window.$dialog.warning({
    title: '确认全量同步',
    content: '将拉取泛微全量人员与组织并写库，是否继续？',
    positiveText: '开始同步',
    negativeText: '取消',
    onPositiveClick: () => doSync(),
  })
}

function stopPoll() {
  if (pollTimer != null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPoll() {
  stopPoll()
  pollTimer = setInterval(() => {
    loadBatches()
  }, POLL_MS)
}

function applyBatchRowToLastResult(row) {
  if (!row || !lastResult.value)
    return
  lastResult.value = {
    batchId: row.id,
    status: row.status,
    fetchedOrgCount: row.fetchedOrgCount ?? 0,
    fetchedUserCount: row.fetchedUserCount ?? 0,
    insertedCount: row.insertedCount ?? 0,
    updatedCount: row.updatedCount ?? 0,
    disabledCount: row.disabledCount ?? 0,
    skippedCount: row.skippedCount ?? 0,
  }
}

function onPendingBatchDone(row) {
  stopPoll()
  pollHint.value = ''
  applyBatchRowToLastResult(row)
  if (row.status === 'success')
    window.$message.success('全量同步已完成')
  else if (row.status === 'partial')
    window.$message.warning('同步结束（部分成功或含跳过项），请查看结果与批次表')
  else if (row.status === 'failed')
    window.$message.error('同步失败，请查看批次表错误信息')
  else
    window.$message.info(`同步结束，状态：${row.status || '-'}`)
}

async function doSync() {
  submitting.value = true
  try {
    const res = await runWeaverFullSync()
    const data = res?.data
    if (data) {
      lastResult.value = data
      if (data.status === 'running' && data.batchId != null) {
        pendingBatchId.value = data.batchId
        if (pagination.value.page !== 1) {
          pagination.value.page = 1
        }
        window.$message.success('已提交全量同步，后台执行中。下方将自动刷新进度。')
        startPoll()
        await loadBatches()
      }
    }
  } catch (e) {
    window.$message.error(e?.message || '提交同步失败')
  } finally {
    submitting.value = false
  }
}

async function loadBatches() {
  tableLoading.value = true
  try {
    const res = await pageWeaverSyncBatches({
      pageNum: pagination.value.page,
      pageSize: pagination.value.pageSize,
    })
    const page = res?.data
    if (page) {
      tableData.value = page.records || []
      pagination.value.itemCount = page.total != null ? Number(page.total) : 0
      if (pendingBatchId.value != null) {
        const id = Number(pendingBatchId.value)
        const row = (page.records || []).find((r) => r.id === id)
        if (row && row.status && row.status !== 'running') {
          onPendingBatchDone(row)
          pendingBatchId.value = null
        }
      }
    } else {
      tableData.value = []
      pagination.value.itemCount = 0
    }
  } catch {
    tableData.value = []
  } finally {
    tableLoading.value = false
  }
}

function fmtTime(v) {
  if (v == null)
    return '-'
  // 后端 LocalDateTime 可能序列化为字符串/时间戳/数组（[yyyy,MM,dd,HH,mm,ss]）/对象（{year,...}）
  if (Array.isArray(v) && v.length >= 3) {
    const [y, M, d, H = 0, m = 0, s = 0] = v
    return formatDateTime(new Date(Number(y), Number(M) - 1, Number(d), Number(H), Number(m), Number(s)))
  }
  if (typeof v === 'object' && (v.year != null || v.month != null || v.day != null)) {
    const y = Number(v.year ?? 0)
    const M = Number(v.month ?? 1)
    const d = Number(v.day ?? v.dayOfMonth ?? 1)
    const H = Number(v.hour ?? 0)
    const m = Number(v.minute ?? 0)
    const s = Number(v.second ?? 0)
    return formatDateTime(new Date(y, M - 1, d, H, m, s))
  }
  return formatDateTime(v)
}

const columns = [
  { title: 'ID', key: 'id', width: 80, fixed: 'left' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(NTag, { size: 'small', ...statusTagProps(row.status) }, () => row.status || '-')
    },
  },
  { title: '触发', key: 'triggerType', width: 90 },
  { title: '开始时间', key: 'startedAt', width: 170, render: (row) => fmtTime(row.startedAt) },
  { title: '结束时间', key: 'endedAt', width: 170, render: (row) => fmtTime(row.endedAt) },
  { title: '拉取组织', key: 'fetchedOrgCount', width: 90 },
  { title: '拉取人员', key: 'fetchedUserCount', width: 90 },
  { title: '新增', key: 'insertedCount', width: 70 },
  { title: '更新', key: 'updatedCount', width: 70 },
  { title: '禁用', key: 'disabledCount', width: 70 },
  { title: '跳过', key: 'skippedCount', width: 70 },
  {
    title: '错误/说明',
    key: 'errorMessage',
    minWidth: 200,
    ellipsis: { tooltip: true },
  },
  { title: '快照Hash', key: 'rawSnapshotHash', width: 120, ellipsis: { tooltip: true } },
]

onMounted(async () => {
  await loadBatches()
  const runningRow = tableData.value.find((r) => r.status === 'running')
  if (runningRow) {
    pendingBatchId.value = runningRow.id
    lastResult.value = {
      batchId: runningRow.id,
      status: 'running',
    }
    startPoll()
  }
})

onUnmounted(() => {
  stopPoll()
})
</script>

<style scoped>
.weaver-sync-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 2px 16px;
}

.weaver-sync-card,
.weaver-batches-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}

.header-icon {
  font-size: 22px;
  opacity: 0.85;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-3 {
  margin-top: 12px;
}

.tip-text {
  font-size: 13px;
  max-width: 520px;
  line-height: 1.5;
}

code {
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--n-code-color, rgba(0, 0, 0, 0.06));
}
</style>
