-- 项目表新增：项目类别（存量 NULL 语义同 OTHER，建议在业务中补录）
ALTER TABLE dept_daily_project
  ADD COLUMN project_category VARCHAR(32) NULL COMMENT '项目类别码 INFO_DESIGN/INFO_DEV/RESEARCH/ELECTRICAL_SEC/OTHER' AFTER project_name;

CREATE INDEX idx_dd_proj_category ON dept_daily_project (tenant_id, project_category);
