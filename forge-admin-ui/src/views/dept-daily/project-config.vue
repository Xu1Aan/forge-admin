<template>
  <div class="dept-daily-page project-config-shell">
    <div v-if="!showForm" class="project-list-panel">
      <div class="project-list-header">
        <div class="header-title">
          <i class="i-material-symbols:account-tree-rounded" />
          <span>项目与人员配置</span>
        </div>
        <n-space :size="12" wrap justify="end">
          <n-button v-if="canImportExcel" secondary size="small" @click="openImport">
            Excel导入
          </n-button>
          <n-button type="primary" size="small" @click="openCreate">
            新项目立项
          </n-button>
        </n-space>
      </div>

      <div class="project-list-toolbar">
        <n-space justify="space-between" align="center" wrap :size="[12, 10]">
          <n-space align="center" wrap :size="[12, 10]">
            <n-select
              v-model:value="queryYear"
              style="width: 140px"
              :options="yearOptions"
              placeholder="年份"
              :consistent-menu-width="false"
              @update:value="reload"
            />
            <n-select
              v-model:value="queryCategory"
              style="width: 160px"
              clearable
              placeholder="项目类别"
              :options="categoryOptions"
              :consistent-menu-width="false"
              @update:value="reload"
            />
            <n-input
              v-model:value="keyword"
              style="width: 240px"
              clearable
              placeholder="项目名关键字"
              @keyup.enter="reload"
              @clear="reload"
            />
            <n-button secondary size="small" :loading="loading" @click="reload">
              <template #icon>
                <i class="i-material-symbols:search-rounded" />
              </template>
              搜索
            </n-button>
          </n-space>

          <n-space :size="12" wrap justify="end">
            <n-button quaternary size="small" :disabled="loading" @click="reload">
              <template #icon>
                <i class="i-material-symbols:refresh-rounded" />
              </template>
              刷新
            </n-button>
          </n-space>
        </n-space>
      </div>

      <div class="project-list-content">
        <n-data-table
          :columns="columns"
          :data="rows"
          :loading="loading"
          :pagination="pagination"
          :row-key="r => r.id"
          remote
          :scroll-x="1200"
          @update:page="onPage"
          @update:page-size="onPageSize"
        />
      </div>
    </div>

    <ProjectConfigFormPanel
      v-else
      ref="formPanelRef"
      :edit="edit"
      :project-detail-status="projectDetailStatus"
      @close="closeForm"
      @save="save"
    />

    <n-modal v-model:show="importer.show" preset="card" title="Excel导入项目" style="width: 640px">
      <n-space vertical :size="12">
        <n-alert type="warning" :bordered="false">
          默认先“仅校验不落库”。确认无误后再执行“正式导入”，避免人员重名/缺失导致误导入。
        </n-alert>
        <n-upload
          :default-upload="false"
          accept=".xlsx,.xls"
          :max="1"
          :file-list="importer.fileList"
          @update:file-list="onImportFileChange"
        >
          <n-button>选择Excel文件</n-button>
        </n-upload>
        <n-space justify="end" :size="12">
          <n-button :disabled="!importer.file || importer.loading" @click="() => doImport(true)">
            仅校验
          </n-button>
          <n-button type="error" :disabled="!importer.file" :loading="importer.loading" @click="() => doImport(false)">
            正式导入
          </n-button>
        </n-space>
        <div v-if="importer.result" class="import-result">
          <n-tag size="small" type="info">总行数 {{ importer.result.total }}</n-tag>
          <n-tag size="small" type="success">成功 {{ importer.result.success }}</n-tag>
          <n-tag size="small" :type="importer.result.failed ? 'warning' : 'default'">失败 {{ importer.result.failed }}</n-tag>
          <div v-if="(importer.result.errors || []).length" class="import-errors">
            <div class="import-errors__title">失败明细</div>
            <ul class="import-errors__list">
              <li v-for="e in importer.result.errors" :key="`${e.rowNum}-${e.projectName}`">
                第 {{ e.rowNum }} 行：{{ e.projectName || '（空项目名）' }} — {{ e.message }}
              </li>
            </ul>
          </div>
        </div>
      </n-space>
      <template #footer>
        <n-space justify="end" :size="12">
          <n-button @click="importer.show = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { h, nextTick, onMounted, reactive, ref, computed } from 'vue'
import { NButton, NTag, NSpace } from 'naive-ui'
import ProjectConfigFormPanel from './components/ProjectConfigFormPanel.vue'
import { projectCategoryLabel, PROJECT_CATEGORY_OPTIONS } from '@/constants/dept-daily-project-category'
import { pageProjects, getProjectDetail, createProject, updateProject, deleteProject, importProjectsExcel, finishProject } from '@/api/dept-daily/project'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const loading = ref(false)
const rows = ref([])

const now = new Date()
const queryYear = ref(now.getFullYear())
const keyword = ref('')
const queryCategory = ref(null)
const categoryOptions = PROJECT_CATEGORY_OPTIONS

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return Array.from({ length: 7 }, (_, i) => {
    const v = y - 3 + i
    return { label: `${v}年`, value: v }
  })
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

function isLeaderForRow(row) {
  if (!row)
    return false
  const uid = userStore.userId
  if (uid == null)
    return false
  const leaderId = row.leaderUserId
  if (leaderId == null)
    return false
  return Number(uid) === Number(leaderId)
}

const APPROVE_PERMISSION = 'dept:project:aprrove'

function canApproveProject() {
  const perms = userStore.permissions || []
  return userStore.isAdmin
    || userStore.isTenantAdmin
    || perms.includes(APPROVE_PERMISSION)
}

const canImportExcel = computed(() => canApproveProject())

function formatProjectStatus(status) {
  const s = status || ''
  if (s === 'DRAFT')
    return { type: 'warning', label: '待审批' }
  if (s === 'ACTIVE')
    return { type: 'default', label: '进行中' }
  if (s === 'DONE')
    return { type: 'success', label: '已完成' }
  if (s === 'CLOSED')
    return { type: 'default', label: '已关闭' }
  return { type: 'default', label: s || '—' }
}

const columns = [
  {
    title: '序号',
    key: 'no',
    width: 70,
    render: (_, idx) => (pagination.page - 1) * pagination.pageSize + idx + 1,
  },
  { title: '项目名', key: 'projectName', minWidth: 220 },
  {
    title: '项目类别',
    key: 'projectCategory',
    width: 120,
    render: row => projectCategoryLabel(row.projectCategory),
  },
  { title: '项目负责人', key: 'leaderName', width: 140 },
  { title: '参与项目人数', key: 'memberCount', width: 120 },
  { title: '项目立项时间', key: 'startDate', width: 140 },
  { title: '项目预计截止时间', key: 'planEndDate', width: 160 },
  {
    title: '项目状态',
    key: 'status',
    width: 110,
    render: (row) => {
      const { type, label } = formatProjectStatus(row.status)
      return h(NTag, { type, size: 'small' }, { default: () => label })
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right',
    render: (row) => {
      const actions = []
      const leader = isLeaderForRow(row)
      const canApprove = canApproveProject()

      // 所有人都可以查看
      actions.push(h(NButton, { size: 'small', secondary: true, onClick: () => openView(row) }, { default: () => '查看' }))

      // 待审批状态：只有具备审批权限的人才能审批
      if (canApprove && row?.status === 'DRAFT') {
        actions.push(h(NButton, { size: 'small', type: 'primary', onClick: () => onApproveProject(row) }, { default: () => '审批' }))
      }

      // 只有项目负责人可编辑/删除
      if (leader) {
        actions.push(h(NButton, { size: 'small', secondary: true, onClick: () => openEdit(row) }, { default: () => '编辑' }))
        actions.push(h(NButton, { size: 'small', tertiary: true, type: 'error', onClick: () => onDeleteProject(row) }, { default: () => '删除' }))
      }

      return h(NSpace, { size: 8 }, () => actions)
    },
  },
]

function onDeleteProject(row) {
  if (!isLeaderForRow(row)) {
    window.$message?.warning('无权限')
    return
  }
  if (!row?.id)
    return
  const name = row.projectName || '该项目'
  window.$dialog?.warning({
    title: '确认删除项目？',
    content: `将删除「${name}」及其成员关系。若项目已产生月报数据将禁止删除。此操作不可恢复，请谨慎。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteProject(row.id)
        window.$message?.success('删除成功')
        await load()
      }
      catch (e) {
        console.error(e)
        window.$message?.error(e?.response?.data?.message || e?.message || '删除失败')
      }
    },
  })
}

function onApproveProject(row) {
  if (!canApproveProject()) {
    window.$message?.warning('无权限')
    return
  }
  if (row?.status !== 'DRAFT') {
    window.$message?.warning('项目不在待审批状态')
    return
  }
  if (!row?.id)
    return
  const name = row.projectName || '该项目'
  window.$dialog?.warning({
    title: '确认审批？',
    content: `将「${name}」状态从“待审批”更新为“进行中”。审批后即可进入下一步流程（如月报填报）。`,
    positiveText: '审批通过',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await finishProject(row.id, { done: false })
        window.$message?.success('审批通过')
        await load()
      }
      catch (e) {
        console.error(e)
        window.$message?.error(e?.response?.data?.message || e?.message || '审批失败')
      }
    },
  })
}

async function load() {
  loading.value = true
  try {
    const res = await pageProjects({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      year: queryYear.value,
      keyword: keyword.value?.trim() || undefined,
      projectCategory: queryCategory.value || undefined,
    })
    const page = res.data
    // 后端已按：待审批优先 + updateTime 倒序（兜底 id 倒序）排序，这里保持服务端顺序
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

function formatDateToIso(ms) {
  if (!ms) return null
  const d = new Date(ms)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const formPanelRef = ref(null)
/** 是否在页面内展示配置表单（替代原弹窗） */
const showForm = ref(false)

/** 编辑/查看时同步后端项目状态，用于「项目状态」只读展示 */
const projectDetailStatus = ref(null)

const edit = reactive({
  saving: false,
  readOnly: false,
  form: {
    id: null,
    projectName: '',
    leaderUserId: null,
    memberUserIds: [],
    startDate: null,   // DatePicker: timestamp
    planEndDate: null, // DatePicker: timestamp
    remark: '',
    projectCategory: null,
  },
})

function normalizeUserId(v) {
  if (v == null || v === '')
    return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

function hasMember(uid) {
  return (edit.form.memberUserIds || []).some(x => Number(x) === Number(uid))
}

function ensureLeaderInMembers() {
  const leaderId = normalizeUserId(edit.form.leaderUserId)
  if (leaderId == null)
    return
  if (!hasMember(leaderId))
    edit.form.memberUserIds = [leaderId, ...(edit.form.memberUserIds || [])]
}

function resetForm() {
  edit.form.id = null
  edit.form.projectName = ''
  edit.form.leaderUserId = null
  edit.form.memberUserIds = []
  edit.readOnly = false
  edit.form.startDate = null
  edit.form.planEndDate = null
  edit.form.remark = ''
  edit.form.projectCategory = null
  projectDetailStatus.value = null
}

function openCreate() {
  resetForm()
  edit.readOnly = false
  showForm.value = true
}

const importer = reactive({
  show: false,
  file: null,
  fileList: [],
  loading: false,
  result: null,
})

function openImport() {
  if (!canImportExcel.value) {
    window.$message?.warning('无权限：不能导入项目')
    return
  }
  importer.show = true
  importer.file = null
  importer.fileList = []
  importer.loading = false
  importer.result = null
}

function onImportFileChange(list) {
  importer.fileList = list || []
  const first = importer.fileList[0]
  // Naive UI: UploadFileInfo.file 可能为 File 或为空；某些场景会包在 raw/file 字段里
  const f = first?.file || first?.raw || first?.file?.file
  importer.file = f || null
}

async function doImport(dryRun) {
  if (!importer.file) {
    window.$message?.warning('请先选择Excel文件')
    return
  }
  importer.loading = true
  try {
    const res = await importProjectsExcel(importer.file, { dryRun })
    importer.result = res.data
    if (dryRun)
      window.$message?.success('校验完成')
    else
      window.$message?.success('导入完成')
    await load()
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '导入失败')
  }
  finally {
    importer.loading = false
  }
}

function closeForm() {
  showForm.value = false
}

async function openEdit(row) {
  if (!isLeaderForRow(row)) {
    window.$message?.warning('无权限编辑项目')
    return
  }
  resetForm()
  edit.readOnly = false
  projectDetailStatus.value = row.status ?? null
  edit.form.id = row.id
  edit.form.projectName = row.projectName
  edit.form.projectCategory = row.projectCategory || 'OTHER'
  edit.form.leaderUserId = row.leaderUserId
  edit.form.memberUserIds = []
  edit.form.startDate = row.startDate ? new Date(row.startDate).getTime() : null
  edit.form.planEndDate = row.planEndDate ? new Date(row.planEndDate).getTime() : null
  showForm.value = true

  try {
    const res = await getProjectDetail(row.id)
    const proj = res.data?.project
    if (proj?.status)
      projectDetailStatus.value = proj.status
    if (proj?.remark != null)
      edit.form.remark = proj.remark
    await nextTick()
    await formPanelRef.value?.applyDetailMembers?.(res.data)
  }
  catch (e) {
    console.error(e)
  }
}

async function openView(row) {
  if (!row?.id)
    return
  resetForm()
  edit.readOnly = true
  projectDetailStatus.value = row.status ?? null

  edit.form.id = row.id
  edit.form.projectName = row.projectName
  edit.form.projectCategory = row.projectCategory || 'OTHER'
  edit.form.leaderUserId = row.leaderUserId
  edit.form.memberUserIds = []
  edit.form.startDate = row.startDate ? new Date(row.startDate).getTime() : null
  edit.form.planEndDate = row.planEndDate ? new Date(row.planEndDate).getTime() : null
  showForm.value = true

  try {
    const res = await getProjectDetail(row.id)
    const proj = res.data?.project
    if (proj?.status)
      projectDetailStatus.value = proj.status
    if (proj?.remark != null)
      edit.form.remark = proj.remark
    await nextTick()
    await formPanelRef.value?.applyDetailMembers?.(res.data)
  }
  catch (e) {
    console.error(e)
  }
}

async function save() {
  if (edit.readOnly)
    return
  try {
    await formPanelRef.value?.validate?.()
  }
  catch {
    return
  }

  // 轻提示：成员为空容易导致“配置了项目但成员无法填报”
  if (!edit.form.memberUserIds || edit.form.memberUserIds.length === 0) {
    window.$message?.warning('项目组成员为空：除负责人外的人员将看不到该项目进行月报填报')
  }

  edit.saving = true
  try {
    ensureLeaderInMembers()
    const body = {
      id: edit.form.id || undefined,
      projectName: edit.form.projectName?.trim(),
      projectCategory: edit.form.projectCategory,
      leaderUserId: edit.form.leaderUserId,
      memberUserIds: edit.form.memberUserIds || [],
      startDate: formatDateToIso(edit.form.startDate) || undefined,
      planEndDate: formatDateToIso(edit.form.planEndDate),
      remark: edit.form.remark || undefined,
    }
    if (edit.form.id)
      await updateProject(body)
    else
      await createProject(body)
    window.$message?.success('保存成功')
    showForm.value = false
    await load()
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '保存失败')
  }
  finally {
    edit.saving = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
/* 外层随布局区撑满，避免居中窄栏 */
.project-config-shell {
  width: 100%;
  max-width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  /* min-height: 100dvh; */
  box-sizing: border-box;
  padding: 12px clamp(12px, 2vw, 28px) 16px;
}

/* —— 列表页 —— */
.project-list-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.project-list-header {
  padding: 16px 16px 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  gap: 12px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.header-title i {
  font-size: 20px;
  color: #4f46e5;
}

.project-list-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  background: rgba(249, 250, 251, 0.6);
}

.project-list-content {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow: auto;
  padding: 0 12px 12px;
}

/* ProjectConfigFormPanel 自带表单布局样式 */

</style>

