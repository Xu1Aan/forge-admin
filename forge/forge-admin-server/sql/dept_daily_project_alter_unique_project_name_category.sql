-- 允许同名项目（按项目类别区分）
-- 原约束：uk_dd_proj_tenant_name (tenant_id, project_name)
-- 新约束：uk_dd_proj_tenant_name_category (tenant_id, project_name, project_category)

ALTER TABLE dept_daily_project
  DROP INDEX uk_dd_proj_tenant_name;

ALTER TABLE dept_daily_project
  ADD UNIQUE KEY uk_dd_proj_tenant_name_category (tenant_id, project_name, project_category);

