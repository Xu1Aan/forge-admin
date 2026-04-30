-- 行政区划字典表（与 SysRegion / SysRegionMapper 一致）
-- 存量库需在业务库执行一次；国标区划明细请自行导入或通过管理端维护
CREATE TABLE IF NOT EXISTS `sys_region_code`
(
    `code`         varchar(20)  NOT NULL COMMENT '行政区划代码（GB/T 2260）',
    `name`         varchar(100) NOT NULL COMMENT '区划简称',
    `level`        tinyint      NOT NULL COMMENT '级别(1-省,2-市,3-区/县,4-街道)',
    `parent_code`  varchar(20)           DEFAULT NULL COMMENT '父级代码',
    `full_name`    varchar(255)          DEFAULT NULL COMMENT '全称',
    `city_code`    varchar(20)           DEFAULT NULL COMMENT '地市编码',
    PRIMARY KEY (`code`),
    KEY            `idx_parent_code` (`parent_code`),
    KEY            `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行政区划字典表';

-- 可选：仅根节点占位，便于联调（按需取消注释）
-- REPLACE INTO sys_region_code (code, name, level, parent_code, full_name, city_code)
-- VALUES ('150000', '内蒙古自治区', 1, NULL, '内蒙古自治区', NULL);
