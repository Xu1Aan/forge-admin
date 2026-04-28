<template>
  <div class="dept-daily-page p-4">
    <n-card :bordered="false" class="report-card">
      <template #header>
        <div class="card-hd">
          <div class="card-hd__title">项目月报</div>
          <div class="card-hd__sub">
            {{ reportYmLabel }}
          </div>
        </div>
      </template>

      <template #header-extra>
        <n-space align="center" wrap>
          <n-select
            v-model:value="year"
            style="width: 130px"
            :options="yearOptions"
            :consistent-menu-width="false"
            @update:value="reload"
          />
          <n-select
            v-model:value="month"
            style="width: 110px"
            :options="monthOptions"
            :consistent-menu-width="false"
            @update:value="reload"
          />
          <n-button secondary :loading="loading" @click="reload">
            刷新
          </n-button>
        </n-space>
      </template>

      <div class="toolbar">
        <n-space justify="space-between" align="center" wrap>
          <n-space align="center" wrap>
            <n-tag size="small" :type="filledCount === projects.length && projects.length ? 'success' : 'default'">
              已填写 {{ filledCount }}/{{ projects.length }}
            </n-tag>
            <n-tag v-if="dirtyCount > 0" size="small" type="warning">
              待保存 {{ dirtyCount }}
            </n-tag>
            <n-tag v-else size="small" type="info">
              自动保存（输入后约 1 秒）
            </n-tag>
          </n-space>
          <div class="toolbar-tip">
            输入后将自动保存
          </div>
        </n-space>
      </div>

      <n-spin :show="loading">
        <div v-if="projects.length === 0" class="empty">
          <n-empty description="暂无可填报项目月报（仅负责人可填）" />
        </div>

        <n-collapse v-else class="report-collapse" :default-expanded-names="defaultExpanded">
          <n-collapse-item
            v-for="(p, idx) in projects"
            :key="p.id"
            :name="p.id"
          >
            <template #header>
              <div class="row-hd">
                <div class="row-hd__no">{{ String(idx + 1).padStart(2, '0') }}</div>
                <div class="row-hd__main">
                  <div class="row-hd__title">{{ p.projectName }}</div>
                  <div class="row-hd__meta">
                    <span v-if="p.planEndDate">预计截止：{{ p.planEndDate }}</span>
                    <span v-else>预计截止：—</span>
                    <span class="sep">·</span>
                    <span>{{ getSaveText(p.id) }}</span>
                  </div>
                </div>
                <div class="row-hd__right">
                  <n-tag v-if="(form[p.id] || '').trim().length" size="small" type="success">
                    已填写
                  </n-tag>
                  <n-tag v-else size="small" type="default">
                    未填写
                  </n-tag>
                </div>
              </div>
            </template>

            <div class="panel">
              <n-input
                :value="form[p.id]"
                type="textarea"
                :autosize="false"
                :rows="6"
                placeholder="填写本月项目总体进展/总结（建议：里程碑、交付物、风险、下月计划）"
                @update:value="v => onChange(p.id, v)"
                @blur="() => flushDraft(p.id)"
              />

              <div class="panel-actions">
                <n-space align="center" justify="space-between" wrap>
                  <div class="panel-actions__hint">
                    {{ (form[p.id] || '').trim().length }}/2000
                  </div>
                  <n-space>
                    <n-button
                      tertiary
                      :loading="savingMap[p.id]"
                      :disabled="!(form[p.id] || '').trim().length"
                      @click="() => saveDraftNow(p.id)"
                    >
                      立即保存
                    </n-button>
                  </n-space>
                </n-space>
              </div>
            </div>
          </n-collapse-item>
        </n-collapse>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listFillableProjects } from '@/api/dept-daily/project'
import { pageProjectMonth, upsertProjectMonth } from '@/api/dept-daily/report'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const loading = ref(false)

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return Array.from({ length: 5 }, (_, i) => {
    const v = y - 2 + i
    return { label: `${v}年`, value: v }
  })
})
const monthOptions = computed(() => Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })))

const reportYm = computed(() => `${year.value}-${String(month.value).padStart(2, '0')}`)
const reportYmLabel = computed(() => `${year.value}年${month.value}月`)

const projects = ref([])
const form = reactive({})
const lastSaved = reactive({})
const dirty = reactive({})
const savingMap = reactive({})
const timers = new Map()

const defaultExpanded = computed(() => {
  const ids = projects.value.slice(0, 3).map(p => p.id)
  for (const p of projects.value) {
    if ((form[p.id] || '').trim().length) ids.push(p.id)
  }
  return Array.from(new Set(ids))
})

const filledCount = computed(() => projects.value.filter(p => (form[p.id] || '').trim().length > 0).length)
const dirtyCount = computed(() => projects.value.filter(p => dirty[p.id]).length)

function getSaveText(projectId) {
  if (savingMap[projectId]) return '保存中…'
  if (dirty[projectId]) return '未保存'
  if (lastSaved[projectId] != null) return '已保存'
  return '—'
}

function canWriteForProject(p) {
  const perms = userStore.permissions || []
  return (userStore.userId && userStore.userId === p.leaderUserId)
    || userStore.isAdmin
    || userStore.isTenantAdmin
    || perms.includes('dept-daily:project:monthly-report')
}

async function load() {
  loading.value = true
  try {
    const res = await listFillableProjects({})
    const all = res.data || []
    projects.value = all.filter(canWriteForProject)

    // 并行拉取回显，避免逐条串行等待
    const results = await Promise.all(projects.value.map(async (p) => {
      const pageRes = await pageProjectMonth({ reportYm: reportYm.value, projectId: p.id, pageNum: 1, pageSize: 1 })
      const r = (pageRes.data?.records || [])[0]
      return { projectId: p.id, summaryText: r?.summaryText || '' }
    }))
    for (const it of results) {
      form[it.projectId] = it.summaryText
      lastSaved[it.projectId] = it.summaryText
      dirty[it.projectId] = false
      savingMap[it.projectId] = false
    }
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
  load()
}

function onChange(projectId, v) {
  form[projectId] = v
  dirty[projectId] = (String(v || '') !== String(lastSaved[projectId] || ''))
  scheduleDraft(projectId)
}

function scheduleDraft(projectId) {
  if (timers.has(projectId)) clearTimeout(timers.get(projectId))
  timers.set(projectId, setTimeout(() => {
    void saveDraftNow(projectId)
  }, 1000))
}

async function saveDraftNow(projectId) {
  const text = (form[projectId] || '').trim()
  if (!text) {
    lastSaved[projectId] = ''
    dirty[projectId] = false
    return
  }
  if (savingMap[projectId]) return
  savingMap[projectId] = true
  try {
    await upsertProjectMonth({
      reportYm: reportYm.value,
      projectId,
      summaryText: text,
      submit: false,
    })
    lastSaved[projectId] = form[projectId]
    dirty[projectId] = false
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '保存失败')
  }
  finally {
    savingMap[projectId] = false
  }
}

async function flushDraft(projectId) {
  if (timers.has(projectId)) {
    clearTimeout(timers.get(projectId))
    timers.delete(projectId)
  }
  if (dirty[projectId])
    await saveDraftNow(projectId)
}

// 已按需求移除“提交”能力：保留自动保存与手动“立即保存”

onMounted(() => {
  load()
})
</script>

<style scoped>
.dept-daily-page {
  min-height: 100%;
}

.report-card :deep(.n-card-header) {
  align-items: flex-start;
}

.card-hd__title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}
.card-hd__sub {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.toolbar {
  margin: 4px 0 14px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
}
.toolbar-tip {
  font-size: 12px;
  color: #64748b;
}

.report-collapse :deep(.n-collapse-item__header) {
  padding: 12px 8px;
}
.row-hd {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  min-width: 0;
}
.row-hd__no {
  width: 28px;
  flex-shrink: 0;
  font-weight: 800;
  color: #0f172a;
}
.row-hd__main {
  min-width: 0;
  flex: 1;
}
.row-hd__title {
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-hd__meta {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.sep {
  opacity: 0.6;
}
.row-hd__right {
  flex-shrink: 0;
}
.panel {
  padding: 6px 4px 10px;
}
.panel :deep(textarea) {
  resize: vertical;
}
.panel-actions {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e5e7eb;
}
.panel-actions__hint {
  font-size: 12px;
  color: #94a3b8;
}
.empty {
  padding: 24px 0;
}
</style>

