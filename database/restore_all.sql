-- 一键恢复数据（在已执行 schema.sql 建表后使用）
-- 用法：mysql -u root -p bb < database/restore_all.sql
-- 说明：合并 seed + 首页演示数据 + 岗位数据，统一密码为 123456，避免 ID 冲突

USE bb;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 统一密码：123456 的 BCrypt 哈希
SET @pwd = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

-- ========== 1. 系统用户（含管理员、学生、教师、企业） ==========
INSERT INTO sys_user (id, username, password_hash, full_name, email, phone, role, status, created_at, updated_at) VALUES
    (1, 'admin', @pwd, '系统管理员', 'admin@example.com', NULL, 'ADMIN', 'ACTIVE', NOW(), NOW()),
    (2, 'student1', @pwd, '示例学生', 'student1@example.com', NULL, 'STUDENT', 'ACTIVE', NOW(), NOW()),
    (3, 'teacher1', @pwd, '张老师', 'teacher1@example.com', NULL, 'TEACHER', 'ACTIVE', NOW(), NOW()),
    (4, 'employer1', @pwd, '示例企业', 'employer1@example.com', NULL, 'EMPLOYER', 'ACTIVE', NOW(), NOW()),
    (9001, 'admin_portal', @pwd, '就业办管理员', 'career-admin@university.edu.cn', '010-10000001', 'ADMIN', 'ACTIVE', '2024-02-01 09:00:00', '2024-02-01 09:00:00'),
    (2001, 'teacher_liu', @pwd, '刘畅', 'liu.chang@university.edu.cn', '010-10000021', 'TEACHER', 'ACTIVE', '2024-02-02 09:00:00', '2024-02-02 09:00:00'),
    (2002, 'teacher_wang', @pwd, '王璐', 'wang.lu@university.edu.cn', '010-10000022', 'TEACHER', 'ACTIVE', '2024-02-02 09:05:00', '2024-02-02 09:05:00'),
    (2003, 'teacher_zhao', @pwd, '赵楠', 'zhao.nan@university.edu.cn', '010-10000023', 'TEACHER', 'ACTIVE', '2024-02-02 09:10:00', '2024-02-02 09:10:00'),
    (2004, 'teacher_zhang', @pwd, '张晓玲', 'zhang.xiaoling@university.edu.cn', '010-10000024', 'TEACHER', 'ACTIVE', '2024-02-02 09:15:00', '2024-02-02 09:15:00'),
    (3001, 'student_han', @pwd, '韩雪', 'han.xue@student.edu.cn', '13600000001', 'STUDENT', 'ACTIVE', '2024-02-03 10:00:00', '2024-02-03 10:00:00'),
    (3002, 'student_chen', @pwd, '陈宇', 'chen.yu@student.edu.cn', '13600000002', 'STUDENT', 'ACTIVE', '2024-02-03 10:05:00', '2024-02-03 10:05:00'),
    (3003, 'student_li', @pwd, '李想', 'li.xiang@student.edu.cn', '13600000003', 'STUDENT', 'ACTIVE', '2024-02-03 10:10:00', '2024-02-03 10:10:00'),
    (4001, 'employer_tencent', @pwd, '腾讯科技', 'hr@tencent.com', '0755-86013388', 'EMPLOYER', 'ACTIVE', '2024-01-10 09:00:00', '2024-01-10 09:00:00'),
    (4002, 'employer_alibaba', @pwd, '阿里巴巴', 'hr@alibaba.com', '0571-85022088', 'EMPLOYER', 'ACTIVE', '2024-01-11 09:00:00', '2024-01-11 09:00:00'),
    (4003, 'employer_bytedance', @pwd, '字节跳动', 'hr@bytedance.com', '010-84594000', 'EMPLOYER', 'ACTIVE', '2024-01-12 09:00:00', '2024-01-12 09:00:00'),
    (4004, 'employer_huawei', @pwd, '华为技术', 'hr@huawei.com', '0755-28780808', 'EMPLOYER', 'ACTIVE', '2024-01-13 09:00:00', '2024-01-13 09:00:00'),
    (4005, 'employer_baidu', @pwd, '百度', 'hr@baidu.com', '010-59928888', 'EMPLOYER', 'ACTIVE', '2024-01-14 09:00:00', '2024-01-14 09:00:00'),
    (4006, 'employer_meituan', @pwd, '美团', 'hr@meituan.com', '010-84594000', 'EMPLOYER', 'ACTIVE', '2024-01-15 09:00:00', '2024-01-15 09:00:00'),
    (4007, 'employer_jd', @pwd, '京东', 'hr@jd.com', '010-59993000', 'EMPLOYER', 'ACTIVE', '2024-01-16 09:00:00', '2024-01-16 09:00:00'),
    (4008, 'employer_netease', @pwd, '网易', 'hr@163.com', '020-85105163', 'EMPLOYER', 'ACTIVE', '2024-01-17 09:00:00', '2024-01-17 09:00:00')
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    phone = VALUES(phone),
    role = VALUES(role),
    status = VALUES(status),
    password_hash = VALUES(password_hash);

-- ========== 2. 学生档案 ==========
INSERT INTO student_profile (id, gender, age, major, biography, graduation_year) VALUES
    (2, NULL, NULL, '计算机科学与技术', NULL, 2025),
    (3001, '女', 22, '计算机科学与技术', '热衷人工智能与校园科技创新，参与多项科研项目。', 2024),
    (3002, '男', 23, '软件工程', '熟悉前后端开发流程，关注用户体验设计。', 2024),
    (3003, '男', 21, '信息管理与信息系统', '擅长数据分析，关注就业市场趋势与政策。', 2025)
ON DUPLICATE KEY UPDATE
    gender = VALUES(gender),
    age = VALUES(age),
    major = VALUES(major),
    biography = VALUES(biography),
    graduation_year = VALUES(graduation_year);

-- ========== 3. 教师（保留 teacher1 为 id=1，首页教师为 id=2..5） ==========
INSERT INTO teacher (id, user_id, department, email, phone, major, focus, biography) VALUES
    (1, 3, '计算机学院', 'teacher1@example.com', NULL, '软件工程', '就业指导', NULL),
    (2, 2001, '计算机科学与技术学院', 'liu.chang@university.edu.cn', '010-10000021', NULL, NULL, NULL),
    (3, 2002, '软件工程学院', 'wang.lu@university.edu.cn', '010-10000022', NULL, NULL, NULL),
    (4, 2003, '信息管理学院', 'zhao.nan@university.edu.cn', '010-10000023', NULL, NULL, NULL),
    (5, 2004, '计算机学院', 'zhang.xiaoling@university.edu.cn', '010-10000024', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
    department = VALUES(department),
    email = VALUES(email),
    phone = VALUES(phone);

-- ========== 4. 企业（保留 employer1 为 id=1，其余 id=2..9） ==========
INSERT INTO employer (id, user_id, company_name, contact_person, contact_email, contact_phone, description, website) VALUES
    (1, 4, '示例科技有限公司', '示例企业', 'employer1@example.com', NULL, NULL, NULL),
    (2, 4001, '腾讯科技有限公司', '张婷', 'zhang.ting@tencent.com', '0755-86013388', '中国领先的互联网增值服务提供商，业务涵盖社交、游戏、金融等多个领域。', 'https://www.tencent.com'),
    (3, 4002, '阿里巴巴集团', '李明', 'li.ming@alibaba.com', '0571-85022088', '全球领先的电商和云计算企业，致力于让天下没有难做的生意。', 'https://www.alibaba.com'),
    (4, 4003, '字节跳动科技有限公司', '王芳', 'wang.fang@bytedance.com', '010-84594000', '全球化科技公司，旗下拥有抖音、今日头条等热门产品。', 'https://www.bytedance.com'),
    (5, 4004, '华为技术有限公司', '刘强', 'liu.qiang@huawei.com', '0755-28780808', '全球领先的ICT基础设施和智能终端提供商。', 'https://www.huawei.com'),
    (6, 4005, '百度在线网络技术有限公司', '赵敏', 'zhao.min@baidu.com', '010-59928888', '全球领先的人工智能公司，拥有强大的搜索引擎技术。', 'https://www.baidu.com'),
    (7, 4006, '美团', '孙悦', 'sun.yue@meituan.com', '010-84594000', '中国领先的生活服务电子商务平台。', 'https://www.meituan.com'),
    (8, 4007, '京东集团', '周杰', 'zhou.jie@jd.com', '010-59993000', '中国领先的综合性电商平台和零售基础设施服务商。', 'https://www.jd.com'),
    (9, 4008, '网易公司', '郑娜', 'zheng.na@163.com', '020-85105163', '中国领先的互联网技术公司，专注于游戏、邮箱、音乐等业务。', 'https://www.163.com')
ON DUPLICATE KEY UPDATE
    company_name = VALUES(company_name),
    contact_person = VALUES(contact_person),
    contact_email = VALUES(contact_email),
    contact_phone = VALUES(contact_phone),
    description = VALUES(description),
    website = VALUES(website);

-- ========== 5. 招聘职位（employer_id 2~9 对应腾讯~网易） ==========
INSERT INTO job_posting (id, employer_id, title, description, salary_range, location, work_type, status, published_date, closing_date) VALUES
    (1, 2, '前端开发工程师', '负责公司核心产品的前端架构设计与开发，使用Vue3、React等主流框架。要求熟悉前端工程化，有良好的代码规范意识。', '15-30K', '深圳', 'FULL_TIME', 'OPEN', '2024-02-01 10:00:00', '2024-04-30'),
    (2, 2, 'Java后端开发工程师', '参与微信后台服务开发，负责高并发系统的设计与优化。要求熟悉Spring Boot、MyBatis等框架，有分布式系统经验优先。', '20-40K', '深圳', 'FULL_TIME', 'OPEN', '2024-02-02 10:00:00', '2024-04-30'),
    (3, 2, '产品经理实习生', '协助产品经理进行需求调研、产品设计和项目跟进。要求对互联网产品有热情，逻辑思维清晰。', '150-250/天', '深圳', 'INTERNSHIP', 'OPEN', '2024-02-03 10:00:00', '2024-03-31'),
    (4, 3, '算法工程师', '负责推荐系统、搜索算法的研发与优化。要求熟悉机器学习、深度学习算法，有大规模数据处理经验。', '25-50K', '杭州', 'FULL_TIME', 'OPEN', '2024-02-01 11:00:00', '2024-05-31'),
    (5, 3, '云计算开发工程师', '参与阿里云产品的研发，负责分布式存储、计算平台的设计与实现。', '22-45K', '杭州', 'FULL_TIME', 'OPEN', '2024-02-02 11:00:00', '2024-05-31'),
    (6, 3, '前端开发工程师', '负责淘宝、天猫等电商平台的前端开发，优化用户体验。', '18-35K', '杭州', 'FULL_TIME', 'OPEN', '2024-02-03 11:00:00', '2024-04-30'),
    (7, 4, 'Android开发工程师', '负责抖音App的功能开发与性能优化。要求熟悉Android开发，有音视频开发经验优先。', '20-40K', '北京', 'FULL_TIME', 'OPEN', '2024-02-01 12:00:00', '2024-04-30'),
    (8, 4, '数据分析师', '负责产品数据分析，提供数据支持和业务洞察。要求熟悉SQL、Python，有数据可视化经验。', '15-30K', '北京', 'FULL_TIME', 'OPEN', '2024-02-02 12:00:00', '2024-04-30'),
    (9, 4, '测试开发工程师', '负责自动化测试平台的开发与维护，提升测试效率。', '18-35K', '北京', 'FULL_TIME', 'OPEN', '2024-02-03 12:00:00', '2024-04-30'),
    (10, 5, '嵌入式软件工程师', '负责通信设备的嵌入式软件开发。要求熟悉C/C++，了解Linux系统。', '15-30K', '深圳', 'FULL_TIME', 'OPEN', '2024-02-01 13:00:00', '2024-05-31'),
    (11, 5, '5G研发工程师', '参与5G核心网、基站等产品的研发。要求通信、计算机相关专业，有通信协议开发经验优先。', '20-40K', '深圳', 'FULL_TIME', 'OPEN', '2024-02-02 13:00:00', '2024-05-31'),
    (12, 6, 'AI研究员', '从事自然语言处理、计算机视觉等方向的研究。要求有深度学习研究经验，发表过高水平论文优先。', '30-60K', '北京', 'FULL_TIME', 'OPEN', '2024-02-01 14:00:00', '2024-05-31'),
    (13, 6, 'Python开发工程师', '负责百度AI平台的后端开发。要求熟悉Python、Django/Flask框架。', '18-35K', '北京', 'FULL_TIME', 'OPEN', '2024-02-02 14:00:00', '2024-04-30'),
    (14, 7, 'Go开发工程师', '负责美团外卖后台服务开发，处理高并发业务场景。要求熟悉Go语言，有微服务架构经验。', '20-40K', '北京', 'FULL_TIME', 'OPEN', '2024-02-01 15:00:00', '2024-04-30'),
    (15, 7, '运营实习生', '协助运营团队进行活动策划、数据分析等工作。', '120-200/天', '北京', 'INTERNSHIP', 'OPEN', '2024-02-02 15:00:00', '2024-03-31'),
    (16, 8, '供应链算法工程师', '负责智能仓储、配送路径优化等算法研发。', '22-45K', '北京', 'FULL_TIME', 'OPEN', '2024-02-01 16:00:00', '2024-05-31'),
    (17, 8, '全栈开发工程师', '参与电商平台全栈开发，负责前后端技术选型和架构设计。', '20-40K', '北京', 'FULL_TIME', 'OPEN', '2024-02-02 16:00:00', '2024-04-30'),
    (18, 9, '游戏开发工程师', '负责网易游戏的客户端或服务端开发。要求熟悉Unity或UE4，有游戏开发经验优先。', '18-38K', '广州', 'FULL_TIME', 'OPEN', '2024-02-01 17:00:00', '2024-04-30'),
    (19, 9, 'UI设计师', '负责产品界面设计，输出高质量的视觉设计稿。要求有良好的审美能力和设计功底。', '12-25K', '广州', 'FULL_TIME', 'OPEN', '2024-02-02 17:00:00', '2024-04-30'),
    (20, 9, '音乐产品经理', '负责网易云音乐产品规划与迭代。要求对音乐行业有深入了解。', '18-35K', '广州', 'FULL_TIME', 'OPEN', '2024-02-03 17:00:00', '2024-04-30')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    salary_range = VALUES(salary_range),
    location = VALUES(location),
    work_type = VALUES(work_type),
    status = VALUES(status),
    published_date = VALUES(published_date),
    closing_date = VALUES(closing_date);

-- ========== 6. 职位要求 ==========
INSERT INTO job_requirement (job_id, requirement) VALUES
    (1, '计算机相关专业本科及以上学历'),
    (1, '3年以上前端开发经验'),
    (1, '精通Vue3或React框架'),
    (1, '熟悉前端工程化工具'),
    (2, '计算机相关专业本科及以上学历'),
    (2, '熟悉Java、Spring Boot、MyBatis'),
    (2, '有分布式系统开发经验'),
    (2, '良好的代码规范和文档习惯'),
    (3, '本科及以上在读，计算机、设计等相关专业'),
    (3, '对互联网产品有热情'),
    (3, '每周至少实习3天，持续3个月以上'),
    (4, '计算机、数学等相关专业硕士及以上学历'),
    (4, '熟悉常见机器学习、深度学习算法'),
    (4, '熟练使用Python、TensorFlow或PyTorch'),
    (4, '有推荐系统或搜索引擎开发经验优先'),
    (5, '计算机相关专业本科及以上学历'),
    (5, '熟悉分布式系统原理'),
    (5, '有大规模系统开发经验'),
    (6, '计算机相关专业本科及以上学历'),
    (6, '熟悉React、Vue等主流框架'),
    (6, '有电商或大型Web应用开发经验优先'),
    (7, '计算机相关专业本科及以上学历'),
    (7, '熟悉Android开发，熟练使用Kotlin或Java'),
    (7, '有音视频开发经验优先'),
    (8, '统计学、数学、计算机等相关专业本科及以上'),
    (8, '熟练使用SQL、Python进行数据分析'),
    (8, '熟悉常用数据可视化工具'),
    (9, '计算机相关专业本科及以上学历'),
    (9, '熟悉自动化测试框架'),
    (9, '有测试平台开发经验优先'),
    (10, '计算机、电子等相关专业本科及以上'),
    (10, '熟练掌握C/C++语言'),
    (10, '了解Linux操作系统'),
    (11, '通信工程、计算机等相关专业硕士及以上'),
    (11, '熟悉通信协议栈'),
    (11, '有5G或4G开发经验优先'),
    (12, '计算机、数学等相关专业博士学历'),
    (12, '在NLP或CV领域有深入研究'),
    (12, '在顶级会议发表过论文'),
    (13, '计算机相关专业本科及以上学历'),
    (13, '熟练使用Python语言'),
    (13, '熟悉Django或Flask框架'),
    (14, '计算机相关专业本科及以上学历'),
    (14, '熟练使用Go语言'),
    (14, '有微服务开发经验'),
    (15, '本科及以上在读'),
    (15, '对互联网运营感兴趣'),
    (15, '有数据分析能力'),
    (16, '计算机、数学、运筹学等相关专业硕士及以上'),
    (16, '熟悉优化算法'),
    (16, '有物流、供应链算法经验优先'),
    (17, '计算机相关专业本科及以上学历'),
    (17, '熟悉前后端开发技术'),
    (17, '有全栈项目经验'),
    (18, '计算机相关专业本科及以上学历'),
    (18, '熟悉Unity3D或Unreal Engine'),
    (18, '热爱游戏，有游戏开发经验优先'),
    (19, '设计相关专业本科及以上学历'),
    (19, '精通Sketch、Figma等设计工具'),
    (19, '有良好的审美和创意能力'),
    (20, '本科及以上学历'),
    (20, '对音乐行业有深入了解'),
    (20, '3年以上产品经理经验')
ON DUPLICATE KEY UPDATE requirement = VALUES(requirement);

-- ========== 7. 系统通知 ==========
INSERT INTO system_notification (id, user_id, category, title, content, read_flag, created_at) VALUES
    (1, 9001, 'SYSTEM', '春季校园招聘全面启动', '2024 年春季双选会正式开放报名，欢迎企业与学生踊跃参与。', 0, '2024-02-20 09:00:00'),
    (2, 9001, 'GUIDANCE', '就业指导公开课上线', '名师带来求职准备、面试技巧专题课程，可在平台预约观看。', 0, '2024-02-22 14:30:00'),
    (3, 9001, 'APPLICATION', '重点企业内推名额开放', '腾讯、阿里等重点企业开放校招内推名额，请尽快完善简历。', 0, '2024-02-25 10:15:00'),
    (4, 9001, 'INTERVIEW', '面试礼仪训练营报名通知', '就业中心联合企业导师推出线下面试礼仪训练营，名额有限。', 0, '2024-02-28 16:45:00')
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    title = VALUES(title),
    content = VALUES(content),
    read_flag = VALUES(read_flag),
    created_at = VALUES(created_at);

-- ========== 8. 公共资料 ==========
INSERT INTO public_resource (id, uploader_id, file_name, file_type, file_size, storage_path, download_url, description, created_at) VALUES
    (1, 9001, '通用简历模板.txt', 'text/plain', 427, 'uploads/resources/resume-template.txt', '/api/public/files/1/download', '标准简历编写结构与要点提示', '2024-02-18 08:30:00'),
    (2, 2001, '面试准备清单.txt', 'text/plain', 491, 'uploads/resources/interview-checklist.txt', '/api/public/files/2/download', '教师精选的线下面试准备要点', '2024-02-19 09:00:00'),
    (3, 2002, '校企合作指南.txt', 'text/plain', 461, 'uploads/resources/employer-cooperation-guide.txt', '/api/public/files/3/download', '面向企业的合作流程与服务说明', '2024-02-21 11:20:00')
ON DUPLICATE KEY UPDATE
    uploader_id = VALUES(uploader_id),
    file_name = VALUES(file_name),
    file_type = VALUES(file_type),
    file_size = VALUES(file_size),
    storage_path = VALUES(storage_path),
    download_url = VALUES(download_url),
    description = VALUES(description),
    created_at = VALUES(created_at);

-- ========== 9. 搜索历史 ==========
INSERT INTO public_search_history (id, user_id, keyword, role_filter, category_filter, location_filter, advanced_options, created_at) VALUES
    (1, 3001, '前端开发', 'STUDENT', 'job,notification', '北京', 'keyword=%E5%89%8D%E7%AB%AF%E5%BC%80%E5%8F%91&role=STUDENT', '2024-02-24 20:15:00'),
    (2, 3001, '面试礼仪', 'STUDENT', 'resource', NULL, 'keyword=%E9%9D%A2%E8%AF%95%E7%A4%BC%E4%BB%AA&category=resource', '2024-02-26 09:30:00'),
    (3, 3002, '内推', 'STUDENT', 'job', '深圳', 'keyword=%E5%86%85%E6%8E%A8&location=%E6%B7%B1%E5%9C%B3', '2024-02-27 18:40:00')
ON DUPLICATE KEY UPDATE
    keyword = VALUES(keyword),
    role_filter = VALUES(role_filter),
    category_filter = VALUES(category_filter),
    location_filter = VALUES(location_filter),
    advanced_options = VALUES(advanced_options),
    created_at = VALUES(created_at);

-- ========== 10. 就业政策（若表不存在可注释本段） ==========
-- 若 schema 中无 employment_policy 表，请先建表或跳过本段
CREATE TABLE IF NOT EXISTS employment_policy (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(200) NOT NULL COMMENT '政策标题',
    category     VARCHAR(60)  NOT NULL COMMENT '分类',
    summary      VARCHAR(500) COMMENT '摘要',
    source       VARCHAR(120) COMMENT '来源',
    published_at DATETIME COMMENT '发布时间',
    sort_order   INT DEFAULT 0 COMMENT '排序',
    content      TEXT COMMENT '正文',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='就业政策表';

INSERT INTO employment_policy (title, category, summary, source, published_at, sort_order, content) VALUES
('教育部关于做好高校毕业生就业创业工作的通知', 'NATIONAL', '落实就业优先战略，拓宽市场化社会化就业渠道。', '教育部', NOW(), 100, '一、强化就业工作组织领导。二、拓宽市场化社会化就业渠道。三、健全就业指导与服务体系。'),
('高校毕业生就业手续办理指南', 'PROCEDURE', '报到证、档案、户口迁移等手续办理流程。', '各省人社厅', NOW(), 90, '一、报到证：毕业时由学校统一办理。二、档案：可申请回生源地或挂靠人才市场。三、户口：迁回原籍或迁入就业地按当地政策办理。'),
('校园招聘与秋招春招时间线', 'CAMPUS', '秋招、春招时间节点与网申、笔试、面试注意事项。', '高校就业指导中心', NOW(), 85, '秋招：通常9-11月。春招：次年3-5月。建议提前准备简历与面试。'),
('劳动合同与试用期权益须知', 'RIGHTS', '试用期期限、工资、解除条件及社保规定。', '人社部', NOW(), 80, '试用期期限根据合同期限确定。试用期工资不得低于本单位相同岗位最低档工资或约定工资的百分之八十。')
ON DUPLICATE KEY UPDATE title = VALUES(title), category = VALUES(category), summary = VALUES(summary), source = VALUES(source), published_at = VALUES(published_at), sort_order = VALUES(sort_order), content = VALUES(content);

-- ========== 11. 自增起点 ==========
ALTER TABLE sys_user AUTO_INCREMENT = 10000;
ALTER TABLE teacher AUTO_INCREMENT = 10;
ALTER TABLE employer AUTO_INCREMENT = 10;
ALTER TABLE job_posting AUTO_INCREMENT = 21;

-- ========== 完成 ==========
SELECT '✓ 数据恢复完成。可登录账号（密码均为 123456）：' AS '';
SELECT '  admin / student1 / teacher1 / employer1' AS '';
SELECT '  admin_portal / teacher_liu, teacher_wang, teacher_zhao, teacher_zhang' AS '';
SELECT '  student_han, student_chen, student_li' AS '';
SELECT '  employer_tencent, employer_alibaba, ... employer_netease' AS '';
