import { request } from '@/utils/http'

export function pageUsers(params) {
  return request.get('/system/user/page', { params })
}

