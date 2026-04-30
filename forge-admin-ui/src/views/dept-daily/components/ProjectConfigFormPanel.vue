<template>
  <n-card
    class="project-form-page"
    :bordered="false"
    :segmented="{ content: 'soft' }"
    size="medium"
  >
    <template #header>
      <div class="form-page-header-bar">
        <div class="form-page-header-bar__titles">
          <!-- <div class="form-page-kicker">
            {{ formPageKicker }}
          </div> -->
          <div class="form-page-title-main">
            {{ formPageHeading }}
          </div>
          <div class="form-page-sub">
            {{ formPageSubtitle }}
          </div>
        </div>
        <n-space :size="12" wrap class="form-page-header-bar__actions" justify="end">
          <n-button secondary size="medium" :disabled="edit.saving" @click="emit('close')">
            <template #icon>
              <i class="i-material-symbols:arrow-back-rounded text-18" />
            </template>
            返回列表
          </n-button>
          <n-button
            v-if="!edit.readOnly"
            type="primary"
            size="medium"
            :loading="edit.saving"
            @click="emit('save')"
          >
            <template #icon>
              <i class="i-material-symbols:save-rounded text-18" />
            </template>
            保存
          </n-button>
        </n-space>
      </div>
    </template>

    <div class="form-page-body">
      <n-form
        ref="formRef"
        :model="edit.form"
        :rules="rules"
        label-placement="left"
        label-width="136"
        label-align="right"
        require-mark-placement="right-hanging"
        size="medium"
        :show-feedback="true"
        class="project-config-form"
      >
        <n-card :bordered="false" size="small" class="form-section-card form-section-card--basic" :segmented="{ content: true }">
          <template #header>
            <div class="basic-section-head">
              <div class="basic-section-head__lead">
                <span class="basic-section-head__bar" aria-hidden="true" />
                <span class="basic-section-head__title">基础信息</span>
              </div>
              <n-text depth="3" class="basic-section-head__hint">
                请完整填写必填项
              </n-text>
            </div>
          </template>
          <n-grid class="basic-form-grid" :cols="isFormWide ? 2 : 1" :x-gap="24" :y-gap="12">
            <n-form-item-gi label="项目名称" path="projectName" class="basic-form-item">
              <n-input
                v-model:value="edit.form.projectName"
                placeholder="请输入项目名称"
                :disabled="edit.readOnly"
                show-count
                :maxlength="120"
                clearable
                class="basic-field"
              />
            </n-form-item-gi>
            <n-form-item-gi label="项目类别" path="projectCategory" class="basic-form-item">
              <n-select
                v-model:value="edit.form.projectCategory"
                placeholder="请选择项目类别"
                :options="categoryOptions"
                :consistent-menu-width="false"
                :disabled="edit.readOnly"
                clearable
                class="basic-field basic-field--stretch"
              />
            </n-form-item-gi>
            <n-form-item-gi label="项目负责人" path="leaderUserId" class="basic-form-item basic-form-item--leader">
              <div class="basic-leader-slot">
                <n-select
                  v-model:value="edit.form.leaderUserId"
                  filterable
                  remote
                  clearable
                  placeholder="搜索并选择负责人"
                  :options="leaderOptions"
                  :loading="userLoading"
                  :consistent-menu-width="false"
                  :disabled="edit.readOnly"
                  class="basic-field basic-field--stretch basic-leader-select"
                  @search="handleUserSearch"
                  @update:value="onLeaderChange"
                />
                <span class="basic-leader-slot__ico" aria-hidden="true">
                  <i class="i-material-symbols:search-rounded" />
                </span>
              </div>
            </n-form-item-gi>
            <n-form-item-gi label="立项时间" path="startDate" class="basic-form-item">
              <n-date-picker
                v-model:value="edit.form.startDate"
                type="date"
                clearable
                class="w-full basic-field basic-datepicker"
                placeholder="选择立项时间"
                :disabled="edit.readOnly"
              />
            </n-form-item-gi>
            <n-form-item-gi label="预计截止日期" path="planEndDate" class="basic-form-item">
              <n-date-picker
                v-model:value="edit.form.planEndDate"
                type="date"
                clearable
                class="w-full basic-field basic-datepicker"
                placeholder="选择预计截止日期"
                :disabled="edit.readOnly"
              />
            </n-form-item-gi>
            <n-gi v-if="isFormWide" />

            <n-form-item-gi label="项目状态" :show-feedback="false" :span="isFormWide ? 2 : 1" class="basic-form-item basic-form-item--block">
              <n-input
                :value="projectStatusLabel"
                disabled
                class="basic-field basic-field--readonly"
                placeholder="—"
              />
            </n-form-item-gi>
            <n-form-item-gi
              label="备注"
              path="remark"
              :span="isFormWide ? 2 : 1"
              class="basic-form-item basic-form-item--block basic-form-item--remark"
            >
              <n-input
                v-model:value="edit.form.remark"
                type="textarea"
                :autosize="{ minRows: 4, maxRows: 8 }"
                placeholder="请输入备注信息（可选），对项目的背景、目标、范围等进行补充说明"
                :disabled="edit.readOnly"
                :maxlength="300"
                show-count
                class="basic-field basic-field--remark"
              />
            </n-form-item-gi>
          </n-grid>
        </n-card>

        <n-card :bordered="false" size="small" class="form-section-card form-section-card--members" :segmented="{ content: true }">
          <template #header>
            <div class="member-section-head">
              <div class="member-section-head__lead">
                <span class="member-section-head__bar" aria-hidden="true" />
                <span class="member-section-head__title">项目成员</span>
              </div>
              <div class="member-section-head__meta">
                <n-tag size="small" round :bordered="false" type="info" class="member-section-head__count">
                  已选 {{ edit.form.memberUserIds.length }} 人
                </n-tag>
                <n-text depth="3" class="member-section-head__tip">
                  搜索并选择用户加入列表，负责人默认在列
                </n-text>
              </div>
            </div>
          </template>
          <div class="member-panel">
            <div class="member-toolbar-row">
              <div class="member-toolbar-row__label">
                <span class="member-toolbar-row__label-txt">成员检索</span>
                <!-- <n-text v-if="!edit.readOnly" depth="3" class="member-toolbar-row__label-sub">
                  支持按姓名或用户名筛选
                </n-text> -->
              </div>
              <div class="member-search-slot">
                <n-select
                  v-model:value="memberPickerValue"
                  filterable
                  remote
                  clearable
                  :clear-filter-after-select="true"
                  placeholder="按姓名 / 用户名搜索，从下拉中选择以加入表格"
                  :options="memberOptions"
                  :loading="userLoading"
                  :consistent-menu-width="false"
                  :disabled="edit.readOnly"
                  class="member-search-select"
                  @search="handleUserSearch"
                  @update:value="onPickMember"
                />
                <span class="member-search-slot__ico" aria-hidden="true">
                  <i class="i-material-symbols:person-search-rounded" />
                </span>
              </div>
            </div>
            <div class="member-table-shell">
              <n-data-table
                :columns="memberColumns"
                :data="memberTableRows"
                :loading="memberBriefLoading"
                :pagination="false"
                size="small"
                :single-line="false"
                striped
                :bordered="true"
                :row-key="r => r.userId"
                :scroll-x="isFormWide ? 1100 : 720"
                flex-height
                class="member-data-table"
              >
                <template #empty>
                  <n-empty description="暂无成员：请通过上方检索选择用户加入" size="small" />
                </template>
              </n-data-table>
            </div>
            <div class="member-foot-hint">
              <i class="i-material-symbols:info-outline-rounded member-foot-hint__ic" />
              <span>项目成员可参与本项目月度工作月报填报；立项保存并审批通过后项目进入可用流程。</span>
            </div>
          </div>
        </n-card>
      </n-form>
    </div>
  </n-card>
</template>

<script setup>
import { computed, h, nextTick, ref } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { NButton, NTag } from 'naive-ui'
import { PROJECT_CATEGORY_OPTIONS } from '@/constants/dept-daily-project-category'
import { fetchProjectUsersBrief } from '@/api/dept-daily/project'
import { pageUsers } from '@/api/system/user'

const props = defineProps({
  edit: { type: Object, required: true },
  /** 服务端项目 status，用于只读文案 */
  projectDetailStatus: {
    default: null,
    validator: v => v === null || v === undefined || typeof v === 'string',
  },
})

const emit = defineEmits(['close', 'save'])

const categoryOptions = PROJECT_CATEGORY_OPTIONS
const isFormWide = useMediaQuery('(min-width: 768px)')
const formRef = ref(null)

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

// const formPageKicker = computed(() => '项目与人员配置')

const projectNameText = computed(() => {
  const name = String(props.edit?.form?.projectName ?? '').trim()
  return name || '未命名项目'
})

const formPageHeading = computed(() => {
  if (props.edit.readOnly)
    return `查看项目 / ${projectNameText.value}`
  if (!props.edit.form.id)
    return '新建项目立项'
  return `编辑项目 / ${projectNameText.value}`
})

const formPageSubtitle = computed(() => {
  if (props.edit.readOnly)
    return '查看项目信息与成员配置'
  if (!props.edit.form.id)
    return '填写项目信息并配置项目成员，保存后进入审批流程'
  return '修改项目信息与成员，保存后生效（仅负责人可编辑）'
})

const projectStatusLabel = computed(() => {
  if (!props.edit.form.id)
    return '待审批（保存立项后）'
  return formatProjectStatus(props.projectDetailStatus).label
})

const rules = {
  projectName: { required: true, message: '请输入项目名称', trigger: ['blur', 'input'] },
  projectCategory: { required: true, message: '请选择项目类别', trigger: ['change'] },
  leaderUserId: { required: true, type: 'number', message: '请选择项目负责人', trigger: ['change'] },
  startDate: { required: true, type: 'number', message: '请选择立项时间', trigger: ['change'] },
  planEndDate: { required: true, type: 'number', message: '请选择预计截止日期', trigger: ['change'] },
}

const userLoading = ref(false)
const leaderOptions = ref([])
const memberOptions = ref([])
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
  const selectedIds = new Set((props.edit.form.memberUserIds || []).map(v => Number(v)))
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
  return (props.edit.form.memberUserIds || []).some(x => Number(x) === Number(uid))
}

function ensureLeaderInMembers() {
  const leaderId = normalizeUserId(props.edit.form.leaderUserId)
  if (leaderId == null)
    return
  if (!hasMember(leaderId))
    props.edit.form.memberUserIds = [leaderId, ...(props.edit.form.memberUserIds || [])]
}

function normalizeMemberRoles(rows) {
  const leaderId = normalizeUserId(props.edit.form.leaderUserId)
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
    const ids = props.edit.form.memberUserIds || []
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

const memberColumns = computed(() => {
  const readonly = props.edit.readOnly
  return [
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
          return h(NTag, { size: 'small', type: 'primary', bordered: false, round: true }, { default: () => '负责人' })
        }
        if (row.memberRole === 'MEMBER') {
          return h(NTag, { size: 'small', type: 'success', bordered: false, round: true }, { default: () => '成员' })
        }
        return row.memberRole
      },
    },
    {
      title: '操作',
      key: 'actions',
      width: 76,
      fixed: 'right',
      render: row => (readonly
        ? '—'
        : h(
            NButton,
            { size: 'small', quaternary: true, type: 'error', onClick: () => removeMember(row.userId) },
            { default: () => '移除' },
          )),
    },
  ]
})

async function onPickMember(val) {
  if (props.edit.readOnly)
    return
  const uid = normalizeUserId(val)
  if (uid == null)
    return
  const cur = props.edit.form.memberUserIds
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
  if (props.edit.readOnly)
    return
  props.edit.form.memberUserIds = props.edit.form.memberUserIds.filter(x => x !== uid)
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

    const selectedIds = new Set((props.edit.form.memberUserIds || []).filter(v => v != null))
    const optsForMember = opts.filter(o => !selectedIds.has(Number(o.value)))

    const leaderId = normalizeUserId(props.edit.form.leaderUserId)
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

async function onLeaderChange(v) {
  const uid = normalizeUserId(v)
  if (uid == null)
    return
  if (!hasMember(uid))
    props.edit.form.memberUserIds = [uid, ...(props.edit.form.memberUserIds || [])]
  await refreshMemberTable()
}

/**
 * 父级在拉取详情后回填成员表格（与服务端返回结构一致）
 */
async function applyDetailMembers(data) {
  const apiRows = data?.memberRows
  const membersFallback = data?.members || []
  if (apiRows != null) {
    memberTableRows.value = normalizeMemberRoles(apiRows)
    props.edit.form.memberUserIds = memberTableRows.value.map(r => r.userId).filter(Boolean)
    ensureLeaderInMembers()
    applyRowsToPickerOptions(apiRows)
    return
  }
  props.edit.form.memberUserIds = membersFallback
    .filter(m => m?.isActive === 1)
    .map(m => m.userId)
    .filter(Boolean)
  ensureLeaderInMembers()
  await refreshMemberTable()
}

function validate() {
  return formRef.value?.validate?.()
}

defineExpose({
  validate,
  applyDetailMembers,
})
</script>

<style scoped>
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
  overflow: auto;
}

.project-form-page :deep(.n-card-footer) {
  flex-shrink: 0;
}

.form-page-header-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.form-page-header-bar__titles {
  flex: 1;
  min-width: min(100%, 260px);
}

.form-page-header-bar__actions {
  flex-shrink: 0;
}

.form-page-kicker {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--n-text-color-3);
  margin-bottom: 6px;
}

.form-page-title-main {
  font-size: clamp(1.1rem, 2.1vw, 1.35rem);
  font-weight: 700;
  line-height: 1.3;
  color: var(--n-title-text-color);
  margin-bottom: 6px;
}

.form-page-sub {
  font-size: 13px;
  line-height: 1.55;
  color: var(--n-text-color-3);
  max-width: 52rem;
}

.form-page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  overflow: hidden auto;
  width: 100%;
  padding-bottom: 8px;
}

.project-config-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.project-config-form :deep(.n-card-header) {
  padding-top: 14px;
  padding-bottom: 12px;
}

.form-section-card {
  border-radius: 12px !important;
  border: 1px solid var(--n-border-color) !important;
  background: var(--n-card-color);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.form-section-card--basic :deep(.n-card-header) {
  padding-top: 12px;
  padding-bottom: 10px;
}

.basic-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 16px;
  width: 100%;
}

.basic-section-head__lead {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 24px;
}

.basic-section-head__bar {
  display: inline-block;
  flex-shrink: 0;
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(
    180deg,
    rgba(59, 130, 246, 1) 0%,
    rgba(37, 99, 235, 1) 100%
  );
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.12);
}

.basic-section-head__title {
  font-size: 16px;
  font-weight: 650;
  color: var(--n-title-text-color);
  letter-spacing: 0.01em;
}

.basic-section-head__hint {
  font-size: 12px;
  white-space: nowrap;
}

.basic-form-grid {
  width: 100%;
}

.basic-form-item.basic-form-item--block :deep(.n-form-item-blank),
.basic-form-item.basic-form-item--block :deep(.n-form-item__body),
.basic-form-item.basic-form-item--block :deep(.n-form-item .n-form-item-blank) {
  width: 100%;
}

.basic-field {
  border-radius: 8px !important;
}

.basic-field.basic-field--stretch {
  width: 100%;
}

.basic-leader-slot {
  position: relative;
  width: 100%;
}

.basic-leader-slot__ico {
  position: absolute;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  font-size: 18px;
  line-height: 1;
  color: rgba(100, 116, 139, 0.7);
  z-index: 1;
}

.basic-leader-select :deep(.n-base-selection-tags),
.basic-leader-select :deep(.n-base-selection-input) {
  padding-right: 1.85rem !important;
}

.basic-field--remark :deep(.n-input-word-count) {
  margin-top: 2px;
}

.basic-field--readonly :deep(.n-input__placeholder),
.basic-field--readonly :deep(.n-input .n-input__input-el) {
  opacity: 0.95;
}

.basic-field--readonly :deep(.n-input .n-input__state-border) {
  opacity: 0.35;
}

.form-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
}

.form-section-head__ttl {
  font-size: 15px;
  font-weight: 650;
  color: var(--n-title-text-color);
}

.form-section-head__hint {
  font-size: 13px;
  color: var(--n-text-color-3);
}

.form-section-card--members :deep(.n-card-header) {
  padding-top: 12px;
  padding-bottom: 10px;
}

.member-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 20px;
  width: 100%;
}

.member-section-head__lead {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-section-head__bar {
  display: inline-block;
  flex-shrink: 0;
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(
    180deg,
    rgba(59, 130, 246, 1) 0%,
    rgba(37, 99, 235, 1) 100%
  );
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.12);
}

.member-section-head__title {
  font-size: 16px;
  font-weight: 650;
  color: var(--n-title-text-color);
  letter-spacing: 0.01em;
}

.member-section-head__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  justify-content: flex-end;
}

.member-section-head__count {
  font-weight: 600;
}

.member-section-head__tip {
  font-size: 12px;
}

.member-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
}

.member-toolbar-row {
  display: grid;
  grid-template-columns: auto minmax(min(520px, 100%), 1fr);
  align-items: start;
  column-gap: 20px;
  row-gap: 10px;
  width: 100%;
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(128, 128, 128, 0.04);
  border: 1px solid var(--n-border-color);
}

.member-toolbar-row__label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 6px;
  min-width: 88px;
}

.member-toolbar-row__label-txt {
  font-size: 13px;
  font-weight: 600;
  color: var(--n-text-color-2);
  white-space: nowrap;
}

.member-toolbar-row__label-sub {
  font-size: 12px;
}

.member-search-slot {
  position: relative;
  width: 100%;
  min-width: 0;
}

.member-search-slot__ico {
  position: absolute;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  font-size: 18px;
  line-height: 1;
  color: rgba(100, 116, 139, 0.7);
  z-index: 1;
}

.member-search-select {
  width: 100%;
  border-radius: 8px !important;
}

.member-search-select :deep(.n-base-selection-tags),
.member-search-select :deep(.n-base-selection-input) {
  padding-right: 1.85rem !important;
}

.member-search-select :deep(.n-base-selection) {
  min-height: 40px !important;
  border-radius: 8px !important;
}

.member-table-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  width: 100%;
  border-radius: 10px;
  overflow: hidden;
}

.member-foot-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--n-text-color-3);
  background: rgba(128, 128, 128, 0.06);
  border-radius: 8px;
  border: 1px solid var(--n-border-color);
}

.member-foot-hint__ic {
  flex-shrink: 0;
  font-size: 18px;
  margin-top: 1px;
  opacity: 0.75;
}

.member-data-table {
  flex: 1;
  width: 100%;
  min-height: 220px;
  height: clamp(260px, calc(100dvh - 392px), 72vh);
}

.member-data-table :deep(.n-data-table-th) {
  font-weight: 500;
}

@media (max-width: 719px) {
  .member-toolbar-row {
    grid-template-columns: 1fr;
  }

  .member-toolbar-row__label {
    flex-direction: row;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 6px 10px;
    padding-top: 0;
    min-width: 0;
  }
}

:root.dark .member-section-head__bar {
  background: linear-gradient(
    180deg,
    rgba(96, 165, 250, 0.95) 0%,
    rgba(59, 130, 246, 0.85) 100%
  );
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.35);
}

:root.dark .member-search-slot__ico {
  color: rgba(148, 163, 184, 0.82);
}

:root.dark .basic-section-head__bar {
  background: linear-gradient(
    180deg,
    rgba(96, 165, 250, 0.95) 0%,
    rgba(59, 130, 246, 0.85) 100%
  );
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.35);
}

:root.dark .basic-leader-slot__ico {
  color: rgba(148, 163, 184, 0.82);
}
</style>
