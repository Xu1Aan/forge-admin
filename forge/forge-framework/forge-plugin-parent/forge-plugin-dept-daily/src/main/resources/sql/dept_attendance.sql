-- 部门日常：考勤填报（最小可用）

CREATE TABLE IF NOT EXISTS `dept_calendar_day` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '1' COMMENT '租户编号（按项目租户拦截规则存1）',
    `year`        int          NOT NULL COMMENT '年份',
    `day`         date         NOT NULL COMMENT '日期',
    `name`        varchar(50)           DEFAULT NULL COMMENT '名称（元旦/周六等）',
    `is_off_day`  tinyint      NOT NULL COMMENT '是否休息日：1休息/0工作',
    `source`      varchar(20)  NOT NULL COMMENT '来源：HOLIDAYS/WEEKENDS',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_dept` bigint                DEFAULT NULL COMMENT '创建部门',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_day` (`tenant_id`, `day`),
    KEY `idx_tenant_year` (`tenant_id`, `year`),
    KEY `idx_tenant_year_off` (`tenant_id`, `year`, `is_off_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节假日/调休日历（缓存第三方接口结果）';


CREATE TABLE IF NOT EXISTS `dept_attendance_sheet` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`    bigint       NOT NULL DEFAULT '1' COMMENT '租户编号',
    `user_id`      bigint       NOT NULL COMMENT '用户ID',
    `year`         int          NOT NULL COMMENT '填报年份',
    `month`        int          NOT NULL COMMENT '填报月份(1-12)',
    `status`       varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿/SUBMITTED已提交',
    `submitted_at` datetime              DEFAULT NULL COMMENT '提交时间',
    `remark`       varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_dept`  bigint                DEFAULT NULL COMMENT '创建部门',
    `create_by`    bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_ym` (`tenant_id`,`user_id`,`year`,`month`),
    KEY `idx_tenant_ym` (`tenant_id`,`year`,`month`),
    KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤月度填报单';


CREATE TABLE IF NOT EXISTS `dept_attendance_item` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   bigint       NOT NULL DEFAULT '1' COMMENT '租户编号',
    `sheet_id`    bigint       NOT NULL COMMENT '月度填报单ID',
    `user_id`     bigint       NOT NULL COMMENT '用户ID（冗余便于查询）',
    `work_date`   date         NOT NULL COMMENT '日期',
    `day_status`  varchar(20)  NOT NULL COMMENT '日状态：WORK出勤/REST休息/TRAVEL出差/LEAVE请假',
    `leave_type`  varchar(30)           DEFAULT NULL COMMENT '请假类型（day_status=LEAVE时必填）',
    `leave_days`  decimal(3,1)          DEFAULT NULL COMMENT '请假天数（day_status=LEAVE时可用）：默认1.0，支持0.5',
    `remark`      varchar(200)          DEFAULT NULL COMMENT '当日备注',
    `create_dept` bigint                DEFAULT NULL COMMENT '创建部门',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sheet_date` (`sheet_id`,`work_date`),
    KEY `idx_tenant_user_date` (`tenant_id`,`user_id`,`work_date`),
    KEY `idx_tenant_date_status` (`tenant_id`,`work_date`,`day_status`),
    CONSTRAINT `fk_att_sheet` FOREIGN KEY (`sheet_id`) REFERENCES `dept_attendance_sheet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤日明细（仅保存覆盖项）';

