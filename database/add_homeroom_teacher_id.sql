-- 为 student_profile_update_request 表添加 homeroom_teacher_id 列（修复 Unknown column 报错）
-- 在 Navicat 中选中数据库 bb 后执行本脚本即可

USE bb;

ALTER TABLE student_profile_update_request 
ADD COLUMN homeroom_teacher_id BIGINT DEFAULT NULL COMMENT '班主任教师ID' AFTER reviewer_id;
