import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { setupRouterGuards } from './guards'

// 手动定义的路由（登录页、SSO、带参数的路由等）
export const manualRoutes = [
  // 白名单页面
  {
    name: 'Login',
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录页', layout: 'empty' },
  },
  {
    name: 'SocialCallback',
    path: '/login/callback',
    component: () => import('@/views/login/callback.vue'),
    meta: { title: '社交登录回调', layout: 'empty' },
  },
  {
    name: 'WeaverCallback',
    path: '/login/weaver-callback',
    component: () => import('@/views/login/weaver-callback.vue'),
    meta: { title: '泛微登录回调', layout: 'empty' },
  },
  {
    name: '404',
    path: '/404',
    component: () => import('@/views/error-page/404.vue'),
    meta: { title: '页面飞走了', layout: 'empty' },
  },
  {
    name: '403',
    path: '/403',
    component: () => import('@/views/error-page/403.vue'),
    meta: { title: '没有权限', layout: 'empty' },
  },
  // 首页重定向
  {
    path: '/',
    redirect: '/home',
    meta: { title: '首页' },
  },
  // iframe 页面
  {
    name: 'iframe',
    path: '/iframe',
    component: () => import('@/views/iframe/index.vue'),
    meta: { title: 'iframe' },
  },
  // 通知公告
  {
    name: 'NoticeList',
    path: '/system/notice-list',
    component: () => import('@/views/system/notice-list.vue'),
    meta: { title: '通知公告' },
  },
  // 消息模板管理
  {
    name: 'MessageTemplate',
    path: '/message/template',
    component: () => import('@/views/message/template-list.vue'),
    meta: { title: '消息模板管理' },
  },
  // 消息管理
  {
    name: 'MessageManage',
    path: '/message/manage',
    component: () => import('@/views/message/manage.vue'),
    meta: { title: '消息管理' },
  },
  // 消息业务类型管理
  {
    name: 'MessageBizType',
    path: '/message/biz-type',
    component: () => import('@/views/message/biz-type.vue'),
    meta: { title: '消息业务类型管理' },
  },
  // 个人中心
  {
    name: 'UserProfile',
    path: '/profile',
    component: () => import('@/views/system/profile.vue'),
    meta: { title: '个人中心' },
  },
  // AI 动态CRUD页面（path 参数形式，兼容菜单注册的 /ai/crud-page/:configKey 格式）
  {
    name: 'AiCrudPageDynamic',
    path: '/ai/crud-page/:configKey',
    component: () => import('@/views/ai/crud-page.vue'),
    meta: { title: 'CRUD页面' },
  },
  // Nexus 布局演示页面
  {
    name: 'NexusRoleManagement',
    path: '/nexus/role-management',
    component: () => import('@/views/nexus/role-management.vue'),
    meta: { title: 'Nexus 角色管理', layout: 'nexus' },
  },
  // 部门日常-考勤填报
  {
    name: 'DeptDailyAttendance',
    path: '/dept-daily/attendance',
    component: () => import('@/views/dept-daily/attendance.vue'),
    meta: { title: '考勤填报' },
  },
  // 部门日常-个人工作月报
  {
    name: 'DeptDailyWorkReport',
    path: '/dept-daily/work-report',
    component: () => import('@/views/dept-daily/work-report.vue'),
    meta: { title: '个人工作月报' },
  },
  // 部门日常-项目月报
  {
    name: 'DeptDailyProjectReport',
    path: '/dept-daily/project-report',
    component: () => import('@/views/dept-daily/project-report.vue'),
    meta: { title: '项目月报' },
  },
  // 部门日常-项目与人员配置
  {
    name: 'DeptDailyProjectConfig',
    path: '/dept-daily/project-config',
    component: () => import('@/views/dept-daily/project-config.vue'),
    meta: { title: '项目与人员配置' },
  },
  // 部门日常-考勤统览
  {
    name: 'DeptDailyAttendanceOverview',
    path: '/dept-daily/attendance-overview',
    redirect: '/dept-daily/attendance-table',
    meta: { title: '考勤统览' },
  },
  // 部门日常-考勤一览表
  {
    name: 'DeptDailyAttendanceTable',
    path: '/dept-daily/attendance-table',
    component: () => import('@/views/dept-daily/attendance-table.vue'),
    meta: { title: '考勤一览表' },
  },
  // 部门日常-未填报人员（考勤）
  {
    name: 'DeptDailyAttendanceUnfilled',
    path: '/dept-daily/attendance-unfilled',
    component: () => import('@/views/dept-daily/attendance-unfilled.vue'),
    meta: { title: '未填报人员（考勤）' },
  },
  // 部门日常-月报统览
  {
    name: 'DeptDailyReportOverview',
    path: '/dept-daily/report-overview',
    redirect: '/dept-daily/report-user-overview',
    meta: { title: '月报统览' },
  },
  // 部门日常-项目统览（项目进展）
  {
    name: 'DeptDailyReportProjectOverview',
    path: '/dept-daily/report-project-overview',
    component: () => import('@/views/dept-daily/report-project-overview.vue'),
    meta: { title: '项目统览' },
  },
  // 部门日常-月报统览（员工月报统计）
  {
    name: 'DeptDailyReportUserOverview',
    path: '/dept-daily/report-user-overview',
    component: () => import('@/views/dept-daily/report-user-overview.vue'),
    meta: { title: '月报统览' },
  },
]

// 从 unplugin-vue-router 自动生成的路由
// eslint-disable-next-line
import { routes as autoRoutes } from 'vue-router/auto-routes'

// 开发环境打印所有路由
if (import.meta.env.DEV) {
  console.log('📋 所有注册的路由:', autoRoutes)
}

// 合并路由：手动路由 + 自动生成路由 + 兜底路由
const routes = [
  ...manualRoutes,
  ...autoRoutes,
  // 兜底路由
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

export const router = createRouter({
  history:
      import.meta.env.VITE_USE_HASH === 'true'
        ? createWebHashHistory(import.meta.env.VITE_PUBLIC_PATH || '/')
        : createWebHistory(import.meta.env.VITE_PUBLIC_PATH || '/'),
  routes,
  scrollBehavior: () => ({ left: 0, top: 0 }),
})

export async function setupRouter(app) {
  app.use(router)
  setupRouterGuards(router)
}
