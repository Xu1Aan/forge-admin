import { request } from '@/utils/http'

export function pageProjects(params) {
  return request.get('/dept-daily/project/page', { params })
}

export function getProjectDetail(id) {
  return request.get(`/dept-daily/project/${id}`)
}

export function createProject(body) {
  return request.post('/dept-daily/project', body)
}

export function updateProject(body) {
  return request.put('/dept-daily/project', body)
}

/** 删除项目（谨慎：后端会校验是否存在月报数据） */
export function deleteProject(id, params) {
  return request.delete(`/dept-daily/project/${id}`, { params })
}

export function finishProject(id, body) {
  return request.post(`/dept-daily/project/${id}/finish`, body)
}

/** Excel导入项目（默认 dryRun=true 仅校验；dryRun=false 才落库） */
export function importProjectsExcel(file, params) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/dept-daily/project/import/excel', fd, {
    params,
    // FormData 上传必须跳过“请求体加密”，否则会被替换成 {data, algorithm} 导致文件丢失
    encrypt: false,
    headers: { 'Content-Type': 'multipart/form-data', 'X-Inner-Call': 'true' },
  })
}

export function listFillableProjects(params) {
  return request.get('/dept-daily/project/fillable', { params })
}

/** 批量查询用户简报（姓名、用户名、部门、电话等），用于项目成员表格 */
export function fetchProjectUsersBrief(userIds) {
  return request.post('/dept-daily/project/users/brief', userIds ?? [])
}
