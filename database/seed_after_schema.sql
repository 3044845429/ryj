-- 在运行 schema.sql 清空库之后执行本脚本，恢复可登录的测试账号（密码均为 123456）
-- 用法：mysql -u root -p bb < database/seed_after_schema.sql

USE bb;

-- 密码 123456 的 BCrypt 哈希（与项目 Spring Security 一致）
SET @pwd_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

-- 1. 管理员（用于登录后台）
INSERT INTO sys_user (id, username, password_hash, full_name, email, phone, role, status, created_at, updated_at)
VALUES (1, 'admin', @pwd_hash, '系统管理员', 'admin@example.com', NULL, 'ADMIN', 'ACTIVE', NOW(), NOW());

-- 2. 学生 student1
INSERT INTO sys_user (id, username, password_hash, full_name, email, phone, role, status, created_at, updated_at)
VALUES (2, 'student1', @pwd_hash, '示例学生', 'student1@example.com', NULL, 'STUDENT', 'ACTIVE', NOW(), NOW());
INSERT INTO student_profile (id, gender, age, major, biography, graduation_year)
VALUES (2, NULL, NULL, '计算机科学与技术', NULL, 2025);

-- 3. 教师
INSERT INTO sys_user (id, username, password_hash, full_name, email, phone, role, status, created_at, updated_at)
VALUES (3, 'teacher1', @pwd_hash, '张老师', 'teacher1@example.com', NULL, 'TEACHER', 'ACTIVE', NOW(), NOW());
INSERT INTO teacher (id, user_id, department, email, phone, major, focus, biography)
VALUES (1, 3, '计算机学院', 'teacher1@example.com', NULL, '软件工程', '就业指导', NULL);

-- 4. 企业用户
INSERT INTO sys_user (id, username, password_hash, full_name, email, phone, role, status, created_at, updated_at)
VALUES (4, 'employer1', @pwd_hash, '示例企业', 'employer1@example.com', NULL, 'EMPLOYER', 'ACTIVE', NOW(), NOW());
INSERT INTO employer (id, user_id, company_name, contact_person, contact_email, contact_phone, description, website)
VALUES (1, 4, '示例科技有限公司', '示例企业', 'employer1@example.com', NULL, NULL, NULL);

-- 5. 就业政策示例数据（可选，供「就业政策」页面展示）
INSERT INTO employment_policy (title, category, summary, source, published_at, sort_order, content) VALUES
('教育部关于做好高校毕业生就业创业工作的通知', 'NATIONAL', '落实就业优先战略，拓宽市场化社会化就业渠道。', '教育部', NOW(), 100, '一、强化就业工作组织领导。二、拓宽市场化社会化就业渠道。三、健全就业指导与服务体系。'),
('高校毕业生就业手续办理指南', 'PROCEDURE', '报到证、档案、户口迁移等手续办理流程。', '各省人社厅', NOW(), 90, '一、报到证：毕业时由学校统一办理。二、档案：可申请回生源地或挂靠人才市场。三、户口：迁回原籍或迁入就业地按当地政策办理。'),
('校园招聘与秋招春招时间线', 'CAMPUS', '秋招、春招时间节点与网申、笔试、面试注意事项。', '高校就业指导中心', NOW(), 85, '秋招：通常9-11月。春招：次年3-5月。建议提前准备简历与面试。'),
('劳动合同与试用期权益须知', 'RIGHTS', '试用期期限、工资、解除条件及社保规定。', '人社部', NOW(), 80, '试用期期限根据合同期限确定。试用期工资不得低于本单位相同岗位最低档工资或约定工资的百分之八十。');

-- 将自增指针移到 10，避免与上面固定 ID 冲突
ALTER TABLE sys_user AUTO_INCREMENT = 10;
ALTER TABLE teacher AUTO_INCREMENT = 10;
ALTER TABLE employer AUTO_INCREMENT = 10;

SELECT '✓ 种子数据已写入，可使用以下账号登录（密码均为 123456）：' AS '';
SELECT '  admin / 123456  → 管理员' AS '';
SELECT '  student1 / 123456 → 学生' AS '';
SELECT '  teacher1 / 123456 → 教师' AS '';
SELECT '  employer1 / 123456 → 企业' AS '';
