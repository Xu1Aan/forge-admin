<template>
  <div class="dept-daily-page project-config-shell">
    <n-card v-if="!showForm" title="项目与人员配置" class="project-config-list" :bordered="false">
      <div class="toolbar">
        <n-space justify="space-between" align="center" wrap>
          <n-space align="center" wrap>
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
            <n-button secondary :loading="loading" @click="reload">
              刷新
            </n-button>
          </n-space>

          <n-space :size="12">
            <n-space :size="12">
              <n-button v-if="canImportExcel" secondary @click="openImport">
                Excel导入
              </n-button>
              <n-button type="primary" @click="openCreate">
                新项目立项
              </n-button>
            </n-space>
          </n-space>
        </n-space>
      </div>

      <div class="project-list-table-area">
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
    </n-card>

    <n-card
      v-else
      class="project-form-page"
      :bordered="false"
      :segmented="{ footer: 'soft', content: true }"
      size="medium"
    >
      <template #header>
        <div class="form-page-header">
          <n-space align="center" :size="12" wrap>
            <n-button secondary @click="closeForm">
              返回列表
            </n-button>
            <div class="form-page-title-wrap">
              <span class="form-page-title">{{ formPageTitle }}</span>
              <span class="form-page-sub">{{ formPageSubtitle }}</span>
            </div>
          </n-space>
        </div>
      </template>

      <div class="form-page-body">
        <n-form ref="formRef" :model="edit.form" :rules="rules" label-placement="left" label-width="108">
        <n-grid :cols="isFormWide ? 2 : 1" :x-gap="20" :y-gap="16">
          <n-form-item-gi label="项目名" path="projectName">
            <n-input v-model:value="edit.form.projectName" placeholder="请输入项目名" :disabled="edit.readOnly" />
          </n-form-item-gi>
          <n-form-item-gi label="项目类别" path="projectCategory">
            <n-select
              v-model:value="edit.form.projectCategory"
              placeholder="请选择"
              :options="categoryOptions"
              :consistent-menu-width="false"
              :disabled="edit.readOnly"
            />
          </n-form-item-gi>
          <n-form-item-gi label="项目负责人" path="leaderUserId">
            <n-select
              v-model:value="edit.form.leaderUserId"
              filterable
              remote
              clearable
              placeholder="输入姓名/用户名搜索"
              :options="leaderOptions"
              :loading="userLoading"
              :consistent-menu-width="false"
              :disabled="edit.readOnly"
              @search="handleUserSearch"
              @update:value="onLeaderChange"
            />
          </n-form-item-gi>
          <n-form-item-gi label="立项时间" path="startDate">
            <n-date-picker v-model:value="edit.form.startDate" type="date" clearable class="w-full" :disabled="edit.readOnly" />
          </n-form-item-gi>
          <n-form-item-gi label="预计截止日期" path="planEndDate" :span="isFormWide ? 1 : 2">
            <n-date-picker v-model:value="edit.form.planEndDate" type="date" clearable class="w-full" :disabled="edit.readOnly" />
          </n-form-item-gi>
          <n-form-item-gi label="备注" path="remark" :span="2">
            <n-input v-model:value="edit.form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="选填" :disabled="edit.readOnly" />
          </n-form-item-gi>

          <n-gi :span="2">
            <n-divider title-placement="left" style="margin: 4px 0 12px">
              项目组成员
            </n-divider>
          </n-gi>
          <n-form-item-gi :span="2" :show-label="false" :show-feedback="false" class="member-form-item member-form-item--stretch">
            <div class="member-panel">
              <div class="member-panel-head">
                <span class="member-panel-hint">
                  {{ edit.readOnly ? `以下为项目成员列表（共 ${edit.form.memberUserIds.length} 人）` : `搜索添加成员，下方为已选列表（共 ${edit.form.memberUserIds.length} 人）` }}
                </span>
              </div>
              <n-select
                v-model:value="memberPickerValue"
                filterable
                remote
                clearable
                :clear-filter-after-select="true"
                placeholder="搜索姓名或用户名，选中即加入列表"
                :options="memberOptions"
                :loading="userLoading"
                :consistent-menu-width="false"
                class="member-add-select"
                :disabled="edit.readOnly"
                @search="handleUserSearch"
                @update:value="onPickMember"
              />
              <div class="member-table-wrap">
                <n-data-table
                  :columns="memberColumns"
                  :data="memberTableRows"
                  :loading="memberBriefLoading"
                  :pagination="false"
                  size="small"
                  :bordered="false"
                  :row-key="r => r.userId"
                  :scroll-x="isFormWide ? 1100 : 720"
                  flex-height
                  class="member-data-table"
                >
                  <template #empty>
                    <div class="member-empty">
                      暂无成员。请使用上方搜索添加；成员可参与该项目月报填报。
                    </div>
                  </template>
                </n-data-table>
              </div>
            </div>
          </n-form-item-gi>
        </n-grid>
      </n-form>
      </div>

      <template #footer>
        <n-space justify="end" class="form-page-footer">
          <n-button :disabled="edit.saving" @click="closeForm">
            {{ edit.readOnly ? '关闭' : '取消' }}
          </n-button>
          <n-button v-if="!edit.readOnly" type="primary" :loading="edit.saving" @click="save">
            保存
          </n-button>
        </n-space>
      </template>
    </n-card>

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
import { useMediaQuery } from '@vueuse/core'
import { NButton, NTag, NSpace } from 'naive-ui'
import { projectCategoryLabel, PROJECT_CATEGORY_OPTIONS } from '@/constants/dept-daily-project-category'
import { fetchProjectUsersBrief, pageProjects, getProjectDetail, createProject, updateProject, deleteProject, importProjectsExcel, finishProject } from '@/api/dept-daily/project'
import { pageUsers } from '@/api/system/user'
import { useUserStore } from '@/store'

const userStore = useUserStore()
/** 表单区两列布局（与成员表横向滚动宽度联动） */
const isFormWide = useMediaQuery('(min-width: 768px)')
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

const formRef = ref(null)
/** 是否在页面内展示配置表单（替代原弹窗） */
const showForm = ref(false)

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

const formPageTitle = computed(() => {
  const name = (edit.form.projectName || '').trim()
  if (!edit.form.id) {
    if (!name)
      return '新项目配置'
    return `${name}项目配置`
  }
  return `${name || '未命名项目'}项目配置`
})

const formPageSubtitle = computed(() =>
  edit.readOnly
    ? '查看项目信息与成员'
    : edit.form.id
      ? '修改项目信息与成员，保存后生效（仅负责人可编辑）'
      : '填写下方信息完成立项；保存后进入“待审批”，审批通过后再进行月报填报',
)

const rules = {
  projectName: { required: true, message: '请输入项目名', trigger: ['blur', 'input'] },
  projectCategory: { required: true, message: '请选择项目类别', trigger: ['change'] },
  leaderUserId: { required: true, type: 'number', message: '请选择负责人', trigger: ['change'] },
  planEndDate: { required: true, type: 'number', message: '请选择预计截止日期', trigger: ['change'] },
}

const userLoading = ref(false)
const leaderOptions = ref([])
const memberOptions = ref([])
/** 仅用于「添加成员」单选，选中后写入 memberUserIds 并清空 */
const memberPickerValue = ref(null)
let userSearchSeq = 0

const memberTableRows = ref([])
const memberBriefLoading = ref(false)

function mapBriefRowToOption(row) {
  if (row?.userId == null)
    return null
  return {
    value: row.userId,
    label: `${row.realName || row.username || ''}${row.username ? `（${row.username}）` : ''}`,
  }
}

function applyRowsToPickerOptions(rows) {
  const opts = (rows || []).map(mapBriefRowToOption).filter(Boolean)
  leaderOptions.value = mergeUserOptions(leaderOptions.value, opts)
  // 成员添加下拉（memberPicker）只负责“新增选择”：
  // 编辑回显时先重置 memberOptions，避免上一次搜索结果残留。
  const selectedIds = new Set((edit.form.memberUserIds || []).map(v => Number(v)))
  const optsForMember = opts.filter(o => !selectedIds.has(Number(o.value)))
  memberOptions.value = optsForMember
}

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

function normalizeMemberRoles(rows) {
  const leaderId = normalizeUserId(edit.form.leaderUserId)
  return (rows || []).map((row) => {
    const uid = normalizeUserId(row?.userId)
    if (uid == null)
      return row
    const role = uid === leaderId ? 'LEADER' : 'MEMBER'
    return { ...row, memberRole: row?.memberRole || role }
  })
}

async function refreshMemberTable() {
  memberBriefLoading.value = true
  try {
    ensureLeaderInMembers()
    const ids = edit.form.memberUserIds || []
    if (!ids.length) {
      memberTableRows.value = []
      return
    }
    const res = await fetchProjectUsersBrief(ids)
    memberTableRows.value = normalizeMemberRoles(res.data || [])
    applyRowsToPickerOptions(memberTableRows.value)
  }
  catch (e) {
    console.error(e)
    window.$message?.error(e?.response?.data?.message || e?.message || '加载成员信息失败')
  }
  finally {
    memberBriefLoading.value = false
  }
}

function cellText(v) {
  return v != null && String(v).trim() !== '' ? v : '—'
}

const memberColumns = [
  {
    title: '姓名',
    key: 'realName',
    width: 120,
    ellipsis: { tooltip: true },
    render: row => cellText(row.realName),
  },
  {
    title: '用户名',
    key: 'username',
    width: 128,
    ellipsis: { tooltip: true },
    render: row => cellText(row.username),
  },
  {
    title: '部门',
    key: 'deptName',
    minWidth: 110,
    ellipsis: { tooltip: true },
    render: row => cellText(row.deptName),
  },
  {
    title: '电话',
    key: 'phone',
    width: 132,
    render: row => cellText(row.phone),
  },
  {
    title: '项目角色',
    key: 'memberRole',
    width: 100,
    render: (row) => {
      if (!row.memberRole)
        return '—'
      if (row.memberRole === 'LEADER') {
        return h(NTag, { size: 'small', type: 'warning', bordered: false }, { default: () => '负责人' })
      }
      if (row.memberRole === 'MEMBER') {
        return h(NTag, { size: 'small', type: 'info', bordered: false }, { default: () => '成员' })
      }
      return row.memberRole
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 76,
    fixed: 'right',
    render: row => (edit.readOnly
      ? '—'
      : h(
          NButton,
          { size: 'small', quaternary: true, type: 'error', onClick: () => removeMember(row.userId) },
          { default: () => '移除' },
        )),
  },
]

async function onPickMember(val) {
  if (edit.readOnly)
    return
  const uid = normalizeUserId(val)
  if (uid == null)
    return
  const cur = edit.form.memberUserIds
  if (cur.some(x => Number(x) === uid)) {
    window.$message?.warning('该成员已在列表中')
    memberPickerValue.value = undefined
    await nextTick()
    return
  }
  cur.push(uid)
  memberPickerValue.value = undefined
  await nextTick()
  await refreshMemberTable()
}

function removeMember(uid) {
  if (edit.readOnly)
    return
  edit.form.memberUserIds = edit.form.memberUserIds.filter(x => x !== uid)
  refreshMemberTable()
}

function mapUserToOption(u) {
  const id = u?.id ?? u?.userId
  if (id == null)
    return null
  return {
    label: `${u.realName || u.username || u.nickName || ''}${u.username ? `（${u.username}）` : ''}`,
    value: id,
  }
}

/** 远程搜索新来的 options 与原选项合并（按 id 去重），避免已选的姓名被刷成 id */
function mergeUserOptions(existing, incoming) {
  const map = new Map()
  for (const o of existing || []) {
    if (o != null && o.value != null)
      map.set(o.value, o)
  }
  for (const o of incoming || []) {
    if (o != null && o.value != null)
      map.set(o.value, o)
  }
  return [...map.values()]
}

async function handleUserSearch(q) {
  const kw = (q || '').trim()
  if (!kw)
    return
  const seq = ++userSearchSeq
  userLoading.value = true
  try {
    const res = await pageUsers({
      pageNum: 1,
      pageSize: 20,
      realName: kw || undefined,
      userStatus: 1,
    })
    if (seq !== userSearchSeq)
      return
    const records = res.data?.records || res.data?.list || []
    const opts = records.map(u => mapUserToOption(u)).filter(Boolean)

    // 成员添加下拉：过滤掉“已选成员”（负责人也属于已选成员）
    const selectedIds = new Set((edit.form.memberUserIds || []).filter(v => v != null))
    const optsForMember = opts.filter(o => !selectedIds.has(Number(o.value)))

    // 负责人下拉：保证当前负责人选项一定存在，避免切换关键字后 label 丢失
    const leaderId = normalizeUserId(edit.form.leaderUserId)
    const selectedFromTable = (memberTableRows.value || []).map(mapBriefRowToOption).filter(Boolean)

    const keepLeader = leaderId == null
      ? []
      : mergeUserOptions(
          (leaderOptions.value || []).filter(o => Number(o.value) === leaderId),
          (selectedFromTable || []).filter(o => Number(o.value) === leaderId),
        )

    leaderOptions.value = mergeUserOptions(keepLeader, opts)
    memberOptions.value = optsForMember
  }
  catch (e) {
    console.error(e)
  }
  finally {
    if (seq === userSearchSeq)
      userLoading.value = false
  }
}

function resetForm() {
  edit.form.id = null
  edit.form.projectName = ''
  edit.form.leaderUserId = null
  edit.form.memberUserIds = []
  edit.readOnly = false
  memberPickerValue.value = undefined
  memberTableRows.value = []
  edit.form.startDate = null
  edit.form.planEndDate = null
  edit.form.remark = ''
  edit.form.projectCategory = null
}

async function onLeaderChange(v) {
  const uid = normalizeUserId(v)
  if (uid == null)
    return
  if (!hasMember(uid))
    edit.form.memberUserIds = [uid, ...(edit.form.memberUserIds || [])]
  await refreshMemberTable()
}

function openCreate() {
  resetForm()
  edit.readOnly = false
  leaderOptions.value = []
  memberOptions.value = []
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
  edit.form.id = row.id
  edit.form.projectName = row.projectName
  edit.form.projectCategory = row.projectCategory || 'OTHER'
  edit.form.leaderUserId = row.leaderUserId
  edit.form.memberUserIds = []
  edit.form.startDate = row.startDate ? new Date(row.startDate).getTime() : null
  edit.form.planEndDate = row.planEndDate ? new Date(row.planEndDate).getTime() : null
  memberPickerValue.value = undefined
  showForm.value = true

  // 回显成员与备注：服务端 memberRows 带部门、电话及项目角色
  try {
    const res = await getProjectDetail(row.id)
    const proj = res.data?.project
    if (proj?.remark != null)
      edit.form.remark = proj.remark
    const apiRows = res.data?.memberRows
    const membersFallback = res.data?.members || []
    if (apiRows != null) {
      memberTableRows.value = normalizeMemberRoles(apiRows)
      edit.form.memberUserIds = memberTableRows.value.map(r => r.userId).filter(Boolean)
      ensureLeaderInMembers()
      applyRowsToPickerOptions(apiRows)
    }
    else {
      edit.form.memberUserIds = membersFallback
        .filter(m => m?.isActive === 1)
        .map(m => m.userId)
        .filter(Boolean)
      ensureLeaderInMembers()
      await refreshMemberTable()
    }
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

  edit.form.id = row.id
  edit.form.projectName = row.projectName
  edit.form.projectCategory = row.projectCategory || 'OTHER'
  edit.form.leaderUserId = row.leaderUserId
  edit.form.memberUserIds = []
  edit.form.startDate = row.startDate ? new Date(row.startDate).getTime() : null
  edit.form.planEndDate = row.planEndDate ? new Date(row.planEndDate).getTime() : null
  memberPickerValue.value = undefined
  showForm.value = true

  // 回显：复用 getProjectDetail 以展示成员列表与备注
  try {
    const res = await getProjectDetail(row.id)
    const proj = res.data?.project
    if (proj?.remark != null)
      edit.form.remark = proj.remark

    const apiRows = res.data?.memberRows
    if (apiRows != null) {
      memberTableRows.value = normalizeMemberRoles(apiRows)
      edit.form.memberUserIds = memberTableRows.value.map(r => r.userId).filter(Boolean)
      ensureLeaderInMembers()
      applyRowsToPickerOptions(apiRows)
      return
    }

    const membersFallback = res.data?.members || []
    edit.form.memberUserIds = membersFallback
      .filter(m => m?.isActive === 1)
      .map(m => m.userId)
      .filter(Boolean)
    ensureLeaderInMembers()
    await refreshMemberTable()
  }
  catch (e) {
    console.error(e)
  }
}

async function save() {
  if (edit.readOnly)
    return
  try {
    await formRef.value?.validate?.()
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
  min-height: 100dvh;
  box-sizing: border-box;
  padding: 12px clamp(12px, 2vw, 28px) 16px;
}

.toolbar {
  flex-shrink: 0;
  margin-bottom: 12px;
}

/* —— 列表页 —— */
.project-config-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: calc(100dvh - 88px);
  width: 100%;
}

.project-config-list :deep(.n-card-header) {
  flex-shrink: 0;
}

.project-config-list :deep(.n-card__content),
.project-config-list :deep(.n-card-content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.project-list-table-area {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow: auto;
}

/* —— 表单页 —— */
.project-form-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: calc(100dvh - 72px);
  width: 100%;
}

.project-form-page :deep(.n-card),
.project-form-page :deep(.n-card.n-card--bordered) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  width: 100%;
}

.project-form-page :deep(.n-card__content),
.project-form-page :deep(.n-card-content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.project-form-page :deep(.n-card-footer) {
  flex-shrink: 0;
}

.form-page-header {
  width: 100%;
}

.form-page-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.form-page-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--n-title-text-color);
}

.form-page-sub {
  font-size: 13px;
  line-height: 1.45;
  color: var(--n-text-color-3);
}

.form-page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden auto;
  width: 100%;
}

.form-page-body :deep(.n-form) {
  width: 100%;
}

.form-page-footer {
  padding-top: 4px;
}

.member-form-item :deep(.n-form-item-blank) {
  min-height: 0;
}

.member-form-item--stretch {
  align-self: stretch;
}

.member-form-item--stretch :deep(.n-form-item) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.member-form-item--stretch :deep(.n-form-item .n-form-item-blank),
.member-form-item--stretch :deep(.n-form-item__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.member-panel {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-radius: 8px;
  border: 1px solid var(--n-border-color);
  background: rgba(128, 128, 128, 0.04);
}

.member-panel-head {
  padding: 10px 12px 0;
}

.member-panel-hint {
  font-size: 13px;
  color: var(--n-text-color-3);
  line-height: 1.45;
}

.member-add-select {
  padding: 8px 12px 12px;
  width: 100%;
  flex-shrink: 0;
}

.member-table-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0 12px 12px;
  border-top: 1px solid var(--n-divider-color);
}

/* flex-height：父级给稳定高度便于表体纵向滚动随视窗变化 */
.member-data-table {
  flex: 1;
  width: 100%;
  min-height: 220px;
  height: clamp(260px, calc(100dvh - 392px), 72vh);
}

.member-data-table :deep(.n-data-table-th) {
  font-weight: 500;
}

.member-empty {
  padding: 20px 8px;
  text-align: center;
  font-size: 13px;
  color: var(--n-text-color-3);
  line-height: 1.5;
}
</style>

