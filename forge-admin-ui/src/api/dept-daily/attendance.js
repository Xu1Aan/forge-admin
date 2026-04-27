import { request } from '@/utils/http'

export function oneClickFillMonth(year, month) {
  return request.post('/dept-daily/attendance/sheet/one-click-fill', null, { params: { year, month } })
}

export function getMonthView(year, month) {
  return request.get('/dept-daily/attendance/sheet/view', { params: { year, month } })
}

export function toggleDay(year, month, payload) {
  return request.post('/dept-daily/attendance/day/toggle', payload, { params: { year, month } })
}

export function submitMonth(year, month) {
  return request.post('/dept-daily/attendance/sheet/submit', null, { params: { year, month } })
}

