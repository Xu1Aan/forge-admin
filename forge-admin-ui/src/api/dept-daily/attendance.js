import { request } from '@/utils/http'

export function oneClickFillMonth(year, month) {
  return request.post('/dept-daily/attendance/sheet/one-click-fill', null, { params: { year, month } })
}

export function getMonthView(year, month) {
  return request.get('/dept-daily/attendance/sheet/view', { params: { year, month } })
}

/** 全量重拉 jiejiari 到 dept_calendar_day，与官网不一致时可调用 */
export function refreshAttendanceCalendar(year) {
  return request.post('/dept-daily/attendance/calendar/refresh', null, { params: { year } })
}

export function toggleDay(year, month, payload) {
  return request.post('/dept-daily/attendance/day/toggle', payload, { params: { year, month } })
}

/** 细调某日状态/假种/备注（不绕 toggle 步进） */
export function updateAttendanceDay(year, month, body) {
  return request.put('/dept-daily/attendance/day', body, { params: { year, month } })
}

export function submitMonth(year, month) {
  return request.post('/dept-daily/attendance/sheet/submit', null, { params: { year, month } })
}

