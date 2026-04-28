import { request } from '@/utils/http'

export function pageUsers(params) {
  return request.get('/system/user/page', { params })
}

export function getUserById(id) {
  return request.post('/system/user/getById', null, { params: { id } })
}

