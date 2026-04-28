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

export function finishProject(id, body) {
  return request.post(`/dept-daily/project/${id}/finish`, body)
}

export function listFillableProjects(params) {
  return request.get('/dept-daily/project/fillable', { params })
}

