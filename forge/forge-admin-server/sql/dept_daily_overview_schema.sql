-- 部门日常管理：考勤统览 / 月报统览 / 未填报统计 / 起始月份设置
-- 适用：MySQL 8.x / InnoDB / utf8mb4
-- 约定：
-- - 员工类型 employee_type：1=正式员工，2=劳务派遣（对齐 sys_user.employee_type）
-- - 预留 tenant_id / dept_id / office_id 以支持按租户、部门、科室隔离与筛选
-- - 基础字段遵循框架 BaseEntity：create_by/create_time/create_dept/update_by/update_time

SET NAMES utf8mb4;

-- =========================
-- 1) 统览起始月份设置（支持分部门/分科室/分员工类型）
-- =========================
DROP TABLE IF EXISTS dept_daily_report_setting;
CREATE TABLE dept_daily_report_setting (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id    BIGINT NOT NULL DEFAULT 1,
  dept_id      BIGINT NULL,
  office_id    BIGINT NULL,
  employee_type TINYINT NULL COMMENT '1=正式员工，2=劳务派遣；NULL=全部',

  attendance_start_ym     CHAR(7) NULL COMMENT '考勤统览起始月 YYYY-MM',
  work_report_start_ym    CHAR(7) NULL COMMENT '个人工作月报起始月 YYYY-MM',
  project_report_start_ym CHAR(7) NULL COMMENT '项目月报起始月 YYYY-MM',

  create_by    BIGINT NULL,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept  BIGINT NULL,
  update_by    BIGINT NULL,
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dd_setting_scope (tenant_id, dept_id, office_id, employee_type),
  INDEX idx_dd_setting_tenant_org (tenant_id, dept_id, office_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 2) 未填报/填报状态统计（按模块 + 年月 + 用户）
-- 用途：快速生成“未填报人员列表”（姓名、员工类型、未填报月份等）
-- =========================
DROP TABLE IF EXISTS dept_daily_fill_state;
CREATE TABLE dept_daily_fill_state (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id     BIGINT NOT NULL DEFAULT 1,
  dept_id       BIGINT NULL,
  office_id     BIGINT NULL,
  employee_type TINYINT NULL COMMENT '1=正式员工，2=劳务派遣（冗余，便于筛选）',

  module        VARCHAR(20) NOT NULL COMMENT 'ATTENDANCE=考勤，WORK_REPORT=个人月报，PROJECT_REPORT=项目月报(可选)',
  year          INT NOT NULL,
  month         INT NOT NULL,
  ym            CHAR(7) NOT NULL COMMENT 'YYYY-MM（冗余便于查询）',

  user_id       BIGINT NOT NULL,

  status        VARCHAR(20) NOT NULL COMMENT 'NONE/DRAFT/SUBMITTED',
  last_calc_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次计算刷新时间',

  create_by     BIGINT NULL,
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept   BIGINT NULL,
  update_by     BIGINT NULL,
  update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dd_fill (tenant_id, module, ym, user_id),
  INDEX idx_dd_fill_scope (tenant_id, module, ym, dept_id, office_id, employee_type, status),
  INDEX idx_dd_fill_user (tenant_id, user_id, module, ym)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 3) 个人月报总表（推荐）：用于员工月报统计/未填报统计的“月度提交状态”锚点
-- 说明：明细仍在 dept_daily_user_month_report_item，sheet 代表“该用户该月是否已提交”
-- =========================
DROP TABLE IF EXISTS dept_daily_user_month_report_sheet;
CREATE TABLE dept_daily_user_month_report_sheet (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id    BIGINT NOT NULL DEFAULT 1,
  dept_id      BIGINT NULL,
  office_id    BIGINT NULL,
  employee_type TINYINT NULL COMMENT '1=正式员工，2=劳务派遣（冗余，便于筛选）',

  report_ym    CHAR(7) NOT NULL COMMENT 'YYYY-MM',
  user_id      BIGINT NOT NULL,

  status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED',
  submitted_at DATETIME NULL,

  create_by    BIGINT NULL,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept  BIGINT NULL,
  update_by    BIGINT NULL,
  update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dd_user_sheet (tenant_id, report_ym, user_id),
  INDEX idx_dd_user_sheet_scope (tenant_id, report_ym, dept_id, office_id, employee_type, status),
  INDEX idx_dd_user_sheet_user (tenant_id, user_id, report_ym)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

