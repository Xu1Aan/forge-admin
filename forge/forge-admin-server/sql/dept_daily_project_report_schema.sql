-- 部门日常管理：项目与月报（含人员配置）建表脚本
-- 适用：MySQL 8.x / InnoDB / utf8mb4
-- 说明：
-- - 预留 tenant_id 以支持多租户（如不需要可固定为 1）
-- - 预留 dept_id / office_id 以支持后续按部门/科室隔离与筛选
-- - user_id 对应系统用户主键（例如 sys_user.id）

SET NAMES utf8mb4;

-- =========================
-- 1) 项目表
-- =========================
DROP TABLE IF EXISTS dept_daily_project;
CREATE TABLE dept_daily_project (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id           BIGINT NOT NULL DEFAULT 1,
  dept_id             BIGINT NULL,                 -- 部门（可为空：不区分部门）
  office_id           BIGINT NULL,                 -- 科室（可为空：不区分科室）

  project_name        VARCHAR(200) NOT NULL,
  leader_user_id      BIGINT NOT NULL,             -- 项目负责人

  start_date          DATE NOT NULL,               -- 立项时间
  plan_end_date       DATE NOT NULL,               -- 预计截止日期

  status              VARCHAR(20) NOT NULL,        -- DRAFT/ACTIVE/DONE/CLOSED
  done_at             DATETIME NULL,
  done_by_user_id     BIGINT NULL,
  remark              VARCHAR(500) NULL,

  create_by           BIGINT NULL,
  create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept         BIGINT NULL,
  update_by           BIGINT NULL,
  update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_dd_proj_tenant_org (tenant_id, dept_id, office_id),
  INDEX idx_dd_proj_leader (leader_user_id),
  INDEX idx_dd_proj_status (status),
  INDEX idx_dd_proj_dates (start_date, plan_end_date),
  UNIQUE KEY uk_dd_proj_tenant_name (tenant_id, project_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 2) 项目成员表
-- =========================
DROP TABLE IF EXISTS dept_daily_project_member;
CREATE TABLE dept_daily_project_member (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id           BIGINT NOT NULL DEFAULT 1,
  project_id          BIGINT NOT NULL,
  user_id             BIGINT NOT NULL,

  role               VARCHAR(20) NOT NULL,         -- MEMBER/LEADER（可扩展：MANAGER）
  joined_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at            DATETIME NULL,
  is_active          TINYINT NOT NULL DEFAULT 1,

  create_by           BIGINT NULL,
  create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept         BIGINT NULL,
  update_by           BIGINT NULL,
  update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dd_proj_member_active (tenant_id, project_id, user_id, is_active),
  INDEX idx_dd_proj_member_project (tenant_id, project_id),
  INDEX idx_dd_proj_member_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 3) 个人工作月报（按项目填报）
-- =========================
DROP TABLE IF EXISTS dept_daily_user_month_report_item;
CREATE TABLE dept_daily_user_month_report_item (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id           BIGINT NOT NULL DEFAULT 1,
  dept_id             BIGINT NULL,
  office_id           BIGINT NULL,

  report_ym          CHAR(7) NOT NULL,             -- 'YYYY-MM'
  project_id         BIGINT NOT NULL,
  user_id            BIGINT NOT NULL,              -- 填报人

  progress_text      TEXT NOT NULL,                -- 进展情况
  work_days          DECIMAL(5,2) NULL,            -- 可选：投入人天/工时
  blockers           TEXT NULL,                    -- 可选：问题/风险
  next_plan          TEXT NULL,                    -- 可选：下月计划

  submitted_at       DATETIME NULL,
  status             VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT/SUBMITTED

  create_by           BIGINT NULL,
  create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept         BIGINT NULL,
  update_by           BIGINT NULL,
  update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dd_user_proj_ym (tenant_id, report_ym, project_id, user_id),
  INDEX idx_dd_user_ym (tenant_id, user_id, report_ym),
  INDEX idx_dd_proj_ym (tenant_id, project_id, report_ym),
  INDEX idx_dd_org_ym (tenant_id, dept_id, office_id, report_ym)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 4) 项目月报（负责人/领导汇总）
-- =========================
DROP TABLE IF EXISTS dept_daily_project_month_report;
CREATE TABLE dept_daily_project_month_report (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id           BIGINT NOT NULL DEFAULT 1,
  dept_id             BIGINT NULL,
  office_id           BIGINT NULL,

  report_ym          CHAR(7) NOT NULL,             -- 'YYYY-MM'
  project_id         BIGINT NOT NULL,

  summary_text       TEXT NOT NULL,                -- 总体进展/总结
  overall_status     VARCHAR(20) NULL,             -- GREEN/YELLOW/RED（可选）
  risks              TEXT NULL,
  next_plan          TEXT NULL,

  submitted_at       DATETIME NULL,
  status             VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT/SUBMITTED

  create_by           BIGINT NULL,
  create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept         BIGINT NULL,
  update_by           BIGINT NULL,
  update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_dd_proj_report_ym (tenant_id, report_ym, project_id),
  INDEX idx_dd_proj_report (tenant_id, project_id, report_ym),
  INDEX idx_dd_org_proj_report (tenant_id, dept_id, office_id, report_ym)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 5) 项目状态变更日志
-- =========================
DROP TABLE IF EXISTS dept_daily_project_status_log;
CREATE TABLE dept_daily_project_status_log (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,

  tenant_id          BIGINT NOT NULL DEFAULT 1,
  project_id         BIGINT NOT NULL,

  from_status        VARCHAR(20) NOT NULL,
  to_status          VARCHAR(20) NOT NULL,
  reason             VARCHAR(500) NULL,

  operated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  operated_by        BIGINT NOT NULL,

  create_by           BIGINT NULL,
  create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept         BIGINT NULL,
  update_by           BIGINT NULL,
  update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_dd_proj_log_time (tenant_id, project_id, operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

