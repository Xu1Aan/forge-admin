/** 部门日常 — 项目类别（与后端 ProjectCategory 枚举码一致） */
export const PROJECT_CATEGORY_OPTIONS = [
  { label: '信息化设计', value: 'INFO_DESIGN' },
  { label: '信息化开发', value: 'INFO_DEV' },
  { label: '科研项目', value: 'RESEARCH' },
  { label: '电气二次', value: 'ELECTRICAL_SEC' },
  { label: '其他', value: 'OTHER' },
]

const LABEL_BY = Object.fromEntries(PROJECT_CATEGORY_OPTIONS.map(o => [o.value, o.label]))

export function projectCategoryLabel(code) {
  if (code == null || code === '')
    return LABEL_BY.OTHER
  return LABEL_BY[code] ?? LABEL_BY.OTHER
}
