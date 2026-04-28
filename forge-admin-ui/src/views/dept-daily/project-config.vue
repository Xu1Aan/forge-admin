<template>
  <div class="dept-daily-page p-4">
    <n-card title="项目与人员配置" :bordered="false">
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

          <n-space>
            <n-button type="primary" @click="openCreate">
              新项目立项
            </n-button>
          </n-space>
        </n-space>
      </div>

      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="r => r.id"
        remote
        @update:page="onPage"
        @update:page-size="onPageSize"
      />
    </n-card>

    <n-modal v-model:show="edit.show" preset="card" :title="editTitle" style="width: 720px">
      <n-form ref="formRef" :model="edit.form" :rules="rules" label-placement="left" label-width="120">
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi label="项目名" path="projectName">
            <n-input v-model:value="edit.form.projectName" placeholder="请输入项目名" />
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
              @search="handleUserSearch"
            />
          </n-form-item-gi>

          <n-form-item-gi label="项目组成员" path="memberUserIds">
            <n-select
              v-model:value="edit.form.memberUserIds"
              multiple
              filterable
              remote
              clearable
              placeholder="输入姓名/用户名搜索"
              :options="memberOptions"
              :loading="userLoading"
              :consistent-menu-width="false"
              @search="handleUserSearch"
            />
          </n-form-item-gi>

          <n-form-item-gi label="立项时间" path="startDate">
            <n-date-picker v-model:value="edit.form.startDate" type="date" clearable />
          </n-form-item-gi>
          <n-form-item-gi label="预计截止日期" path="planEndDate">
            <n-date-picker v-model:value="edit.form.planEndDate" type="date" clearable />
          </n-form-item-gi>
          <n-form-item-gi label="备注" path="remark" :span="2">
            <n-input v-model:value="edit.form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="选填" />
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button :disabled="edit.saving" @click="edit.show = false">
            取消
          </n-button>
          <n-button type="primary" :loading="edit.saving" @click="save">
            保存
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref, computed } from 'vue'
import { NButton, NTag, NSpace } from 'naive-ui'
import { pageProjects, getProjectDetail, createProject, updateProject, finishProject } from '@/api/dept-daily/project'
import { pageUsers } from '@/api/system/user'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const loading = ref(false)
const rows = ref([])

const now = new Date()
const queryYear = ref(now.getFullYear())
const keyword = ref('')

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

function canFinish(row) {
  const uid = userStore.userId
  const perms = userStore.permissions || []
  return uid && (uid === row.leaderUserId || userStore.isAdmin || userStore.isTenantAdmin || perms.includes('dept-daily:project:finish'))
}

const columns = [
  {
    title: '序号',
    key: 'no',
    width: 70,
    render: (_, idx) => (pagination.page - 1) * pagination.pageSize + idx + 1,
  },
  { title: '项目名', key: 'projectName', minWidth: 220 },
  { title: '项目负责人', key: 'leaderName', width: 140 },
  { title: '参与项目人数', key: 'memberCount', width: 120 },
  { title: '项目立项时间', key: 'startDate', width: 140 },
  { title: '项目预计截止时间', key: 'planEndDate', width: 160 },
  {
    title: '项目完成',
    key: 'status',
    width: 110,
    render: (row) => h(NTag, { type: row.status === 'DONE' ? 'success' : 'default', size: 'small' }, { default: () => (row.status === 'DONE' ? '已完成' : '进行中') }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render: (row) => h(NSpace, { size: 8 }, () => [
      h(NButton, { size: 'small', secondary: true, onClick: () => openEdit(row) }, { default: () => '编辑' }),
      h(NButton, {
        size: 'small',
        type: row.status === 'DONE' ? 'warning' : 'success',
        disabled: !canFinish(row),
        onClick: () => toggleFinish(row),
      }, { default: () => (row.status === 'DONE' ? '恢复进行中' : '标记完成') }),
    ]),
  },
]

async function load() {
  loading.value = true
  try {
    const res = await pageProjects({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      year: queryYear.value,
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

function formatDateToIso(ms) {
  if (!ms) return null
  const d = new Date(ms)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const formRef = ref(null)
const edit = reactive({
  show: false,
  saving: false,
  form: {
    id: null,
    projectName: '',
    leaderUserId: null,
    memberUserIds: [],
    startDate: null,   // DatePicker: timestamp
    planEndDate: null, // DatePicker: timestamp
    remark: '',
  },
})

const editTitle = computed(() => (edit.form.id ? '编辑项目' : '新项目立项'))

const rules = {
  projectName: { required: true, message: '请输入项目名', trigger: ['blur', 'input'] },
  leaderUserId: { required: true, type: 'number', message: '请选择负责人', trigger: ['change'] },
  planEndDate: { required: true, type: 'number', message: '请选择预计截止日期', trigger: ['change'] },
}

const userLoading = ref(false)
const leaderOptions = ref([])
const memberOptions = ref([])

async function handleUserSearch(q) {
  const kw = (q || '').trim()
  if (!kw) return
  userLoading.value = true
  try {
    const res = await pageUsers({ pageNum: 1, pageSize: 20, realName: kw })
    const records = res.data?.records || res.data?.list || []
    const opts = records.map(u => ({
      label: `${u.realName || u.username || u.nickName || ''}${u.username ? `（${u.username}）` : ''}`,
      value: u.id || u.userId,
    }))
    leaderOptions.value = opts
    memberOptions.value = opts
  }
  catch (e) {
    console.error(e)
  }
  finally {
    userLoading.value = false
  }
}

function resetForm() {
  edit.form.id = null
  edit.form.projectName = ''
  edit.form.leaderUserId = null
  edit.form.memberUserIds = []
  edit.form.startDate = null
  edit.form.planEndDate = null
  edit.form.remark = ''
}

function openCreate() {
  resetForm()
  edit.show = true
}

async function openEdit(row) {
  resetForm()
  edit.form.id = row.id
  edit.form.projectName = row.projectName
  edit.form.leaderUserId = row.leaderUserId
  edit.form.memberUserIds = []
  edit.form.startDate = row.startDate ? new Date(row.startDate).getTime() : null
  edit.form.planEndDate = row.planEndDate ? new Date(row.planEndDate).getTime() : null
  edit.show = true

  // 回显成员：避免“打开编辑直接保存”导致成员被清空，从而填报页看不到项目
  try {
    const res = await getProjectDetail(row.id)
    const members = res.data?.members || []
    edit.form.memberUserIds = members
      .filter(m => m?.isActive === 1)
      .map(m => m.userId)
      .filter(Boolean)
  }
  catch (e) {
    console.error(e)
  }
}

async function save() {
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
    const body = {
      id: edit.form.id || undefined,
      projectName: edit.form.projectName?.trim(),
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
    edit.show = false
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

async function toggleFinish(row) {
  const done = row.status !== 'DONE'
  const title = done ? '确认标记完成？' : '确认恢复为进行中？'
  window.$dialog?.warning({
    title,
    content: done ? '项目将被标记为已完成。' : '项目将恢复为进行中。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await finishProject(row.id, { done })
      window.$message?.success('操作成功')
      await load()
    },
  })
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.dept-daily-page {
  min-height: 100%;
}
.toolbar {
  margin-bottom: 12px;
}
</style>

