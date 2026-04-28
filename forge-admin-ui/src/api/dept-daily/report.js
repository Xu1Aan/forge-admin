import { request } from '@/utils/http'

export function upsertUserMonthItem(body) {
  return request.post('/dept-daily/report/user-month-item', body)
}

export function pageUserMonthItem(params) {
  return request.get('/dept-daily/report/user-month-item/page', { params })
}

// 统览用：按指定 userId 查询月报明细（需要后端支持/权限）
export function pageUserMonthItemByUser(params) {
  return request.get('/dept-daily/report/user-month-item/page-by-user', { params })
}

export function upsertProjectMonth(body) {
  return request.post('/dept-daily/report/project-month', body)
}

export function pageProjectMonth(params) {
  return request.get('/dept-daily/report/project-month/page', { params })
}

