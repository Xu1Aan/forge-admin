import { request } from '@/utils'

/**
 * 泛微同步 API（经项目 axios，baseURL 为 `import.meta.env.VITE_REQUEST_PREFIX`，
 * 开发环境通常为 `/dev-api`，与「用户管理」等 `/system/*` 一致，由 Vite 代理到 forge-admin-server）
 */

/**
 * 提交泛微全量差分同步（仅创建 running 批次并启动后台任务，几乎立即返回；结果查 batches 或页面轮询）
 */
export function runWeaverFullSync() {
  return request.post('/system/external/weaver/sync')
}

/**
 * 分页查询同步批次
 * @param {{ pageNum?: number, pageSize?: number }} params
 */
export function pageWeaverSyncBatches(params) {
  return request.get('/system/external/weaver/batches', { params })
}
