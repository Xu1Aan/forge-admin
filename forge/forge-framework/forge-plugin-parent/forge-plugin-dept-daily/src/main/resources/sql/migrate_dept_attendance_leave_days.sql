-- 迁移脚本：考勤请假支持半天（leave_days）
-- 适用：MySQL 8.x

ALTER TABLE dept_attendance_item
  ADD COLUMN leave_days DECIMAL(3,1) NULL COMMENT '请假天数（day_status=LEAVE时可用）：默认1.0，支持0.5' AFTER leave_type;

-- 历史数据兼容：已有请假记录但 leave_days 为空的，按 1.0 处理
UPDATE dept_attendance_item
SET leave_days = 1.0
WHERE day_status = 'LEAVE' AND (leave_days IS NULL OR leave_days = 0);

