import { request } from '@/utils/http'

export function getDeptDailyOverviewSetting(params) {
  return request.get('/dept-daily/overview/setting', { params })
}

export function saveDeptDailyOverviewSetting(body) {
  return request.post('/dept-daily/overview/setting', body)
}

export function pageFillState(params) {
  return request.get('/dept-daily/overview/fill-state/page', { params })
}

export function refreshFillState(body) {
  return request.post('/dept-daily/overview/fill-state/refresh', body)
}

export function pageProjectProgress(params) {
  return request.get('/dept-daily/overview/report/project-progress/page', { params })
}

export function pageUserReportStat(params) {
  return request.get('/dept-daily/overview/report/user/page', { params })
}

export function pageAttendanceMonthTable(params) {
  return request.get('/dept-daily/overview/attendance/month-table/page', { params })
}

