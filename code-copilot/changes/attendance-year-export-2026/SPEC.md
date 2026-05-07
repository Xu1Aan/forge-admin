## 背景
当前系统已有“考勤一览表”页面（按月展示人员 1..31 的考勤状态及汇总），但缺少按模板导出 Excel 的能力。
用户提供了 `26数字信息化院2026年度考勤表.xls` 模板，并要求按指定符号规则导出年度考勤表。

## 现状
- 前端：`forge-admin-ui/src/views/dept-daily/attendance-table.vue` 支持按年月、员工类型、关键字筛选并分页展示。
- 后端：`DeptDailyOverviewService.pageAttendanceMonthTable` 可按组织范围/员工类型/关键字计算当月每人每日状态（WORK/REST/TRAVEL/LEAVE）与汇总。
- 请假类型：`dept_attendance_item.leave_type` 存 code（SICK/PERSONAL/...），原月表 VO 未携带请假类型，无法映射到模板符号。
- 下载类接口不能走 `@ApiEncrypt/@ApiDecrypt`（会破坏二进制响应）。

## 目标
- 新增“年度考勤表”导出：一次下载一个 `.xlsx`，包含 12 个 sheet（`yyyy.MM`）。
- 支持选择导出当月（单 sheet）或导出全年（12 sheet）。
- 导出内容与模板结构一致：表头（序号/姓名/日期/1..31/合计）+ 星期行 + 人员明细 + 统计列（出勤/出差/休假）。
- 按符号规则填充每日格：
  - 日勤：`/`
  - 出差：`√`
  - 休息/公休：`休`
  - 请假：按 leaveType 映射（病/事/婚/丧/年/产/探/伤…）
- 最后一列增加“填报状态”：仅当未填报（NONE）时写“未填写”，草稿/已提交留空。
- 导出人员顺序可配置：按“姓名名单顺序”导出，未在名单中的排在最后（为后续其他部门/科室复用）。

## 业务规则
- 组织范围规则沿用统览接口：
  - 未指定 `deptId/officeId` 时：管理员/租户管理员为全租户；普通用户默认限制为其主组织范围。
  - 指定组织时包含其所有子孙组织人员。
- 统计列：
  - 出勤：WORK 天数
  - 出差：TRAVEL 天数
  - 休假：REST + LEAVE 天数（与模板“休假”口径对齐）

## 接口设计
- `GET /dept-daily/overview/attendance/year-export`
  - params: `year`（必填）, `deptId`, `officeId`, `employeeType`, `keyword`
  - response: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - 说明：该接口不加密，直接下载。

- `GET /dept-daily/overview/attendance/export`
  - params: `year`（必填）, `month`（可选；不传=全年，传=当月）, `deptId`, `officeId`, `employeeType`, `keyword`
  - response: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - 说明：该接口不加密，直接下载。

## 实现方案
- 后端新增导出服务：使用 Apache POI 生成工作簿与样式，逐月写入 12 个 sheet。
- 后端新增导出数据方法：返回含 `dayStatus + leaveType` 的当月明细，保证符号映射准确。
- 前端在“考勤一览表”页面加“导出当月/导出全年”按钮，通过 `responseType: 'blob'` 下载。
- 新增“导出顺序设置”：按部门/科室/员工类型维度存储在 `dept_daily_report_setting.attendance_export_order`（文本，每行一个姓名）。

## 风险与兼容
- 模板为 `.xls`：本次实现用代码生成 `.xlsx`，结构对齐模板但样式为“合理设计”的通用样式（细边框、居中、冻结窗格）。
- 迟到/早退/旷工等当前系统未建模：导出按现有状态体系输出，后续若补齐数据模型可扩展映射。

## 测试策略
- 选择一个有覆盖项的月份（出差、请假、周末）导出并人工核对：
  - 星期行是否正确
  - 1..31 天是否对齐、月底空白是否为空
  - 请假符号是否随 leaveType 变化
  - 统计列是否与页面汇总一致

