-- 为 sys_resource 增加 client_code，与 SysResource 实体及 /auth/current/menu 按客户端筛选一致
-- 存量库执行本脚本一次；新库请使用已包含该列的初始化建表脚本
ALTER TABLE `sys_resource`
    ADD COLUMN `client_code` varchar(50) NOT NULL DEFAULT 'pc' COMMENT '客户端编码（如 pc，与 sys_client 对应）' AFTER `remark`,
    ADD KEY `idx_tenant_client` (`tenant_id`, `client_code`);
