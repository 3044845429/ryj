-- 大学生就业管理系统核心数据表设计 (MySQL)

CREATE DATABASE IF NOT EXISTS bb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bb;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS public_resource;
DROP TABLE IF EXISTS public_search_history;
DROP TABLE IF EXISTS system_notification;
DROP TABLE IF EXISTS teacher_guidance;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS interview;
DROP TABLE IF EXISTS job_application;
DROP TABLE IF EXISTS job_requirement;
DROP TABLE IF EXISTS job_posting;
DROP TABLE IF EXISTS employer;
DROP TABLE IF EXISTS resume_experience;
DROP TABLE IF EXISTS resume_skill;
DROP TABLE IF EXISTS resume;
DROP TABLE IF EXISTS employment_intention_city;
DROP TABLE IF EXISTS employment_intention;
DROP TABLE IF EXISTS student_award;
DROP TABLE IF EXISTS student_experience;
DROP TABLE IF EXISTS student_education;
DROP TABLE IF EXISTS student_profile;
DROP TABLE IF EXISTS student_profile_update_request;
DROP TABLE IF EXISTS profile_update_request;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    username      VARCHAR(50)  NOT NULL UNIQUE COMMENT '系统登录用户名，唯一',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密后的登录密码',
    full_name     VARCHAR(100) NOT NULL COMMENT '用户真实姓名',
    email         VARCHAR(120) NOT NULL COMMENT '联系邮箱',
    phone         VARCHAR(30) COMMENT '联系电话',
    role          ENUM('STUDENT','TEACHER','EMPLOYER','ADMIN') NOT NULL COMMENT '系统角色：学生/教师/企业/管理员',
    status        ENUM('ACTIVE','DISABLED') DEFAULT 'ACTIVE' COMMENT '账号状态：启用/禁用',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
) COMMENT='系统用户基础信息表';

CREATE TABLE IF NOT EXISTS student_profile (
    id                BIGINT PRIMARY KEY COMMENT '关联sys_user的ID，学生唯一标识',
    gender            VARCHAR(10) COMMENT '性别',
    age               INT COMMENT '年龄',
    major             VARCHAR(100) COMMENT '主修专业',
    biography         TEXT COMMENT '个人简介/自我描述',
    graduation_year   INT COMMENT '预计毕业年份',
    FOREIGN KEY (id) REFERENCES sys_user(id)
) COMMENT='学生个人基本资料表';

CREATE TABLE IF NOT EXISTS student_profile_update_request (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    student_id      BIGINT      NOT NULL COMMENT '关联学生用户ID',
    gender          VARCHAR(10) COMMENT '申请修改的性别',
    age             INT COMMENT '申请修改的年龄',
    major           VARCHAR(100) COMMENT '申请修改的专业',
    biography       TEXT COMMENT '申请修改的个人简介',
    graduation_year INT COMMENT '申请修改的毕业年份',
    status          ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' COMMENT '审核状态',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    reviewed_at     DATETIME DEFAULT NULL COMMENT '审核时间',
    reviewer_id     BIGINT DEFAULT NULL COMMENT '审核人ID',
    homeroom_teacher_id BIGINT DEFAULT NULL COMMENT '班主任教师ID',
    review_comment  TEXT COMMENT '审核备注',
    FOREIGN KEY (student_id) REFERENCES sys_user(id),
    FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) COMMENT='学生个人档案更新申请记录表';

CREATE TABLE IF NOT EXISTS profile_update_request (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    user_id         BIGINT      NOT NULL COMMENT '关联 sys_user 的ID',
    role            ENUM('TEACHER','EMPLOYER') NOT NULL COMMENT '用户角色：教师/企业',
    payload         TEXT        NOT NULL COMMENT '待更新的资料内容（JSON 字符串）',
    status          ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' COMMENT '审核状态',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    reviewed_at     DATETIME DEFAULT NULL COMMENT '审核时间',
    reviewer_id     BIGINT DEFAULT NULL COMMENT '审核管理员ID',
    review_comment  TEXT COMMENT '管理员审核意见',
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) COMMENT='教师/企业个人资料更新审批记录表';

CREATE TABLE IF NOT EXISTS student_education (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    student_id    BIGINT      NOT NULL COMMENT '关联学生资料表student_profile的ID',
    school        VARCHAR(120) NOT NULL COMMENT '就读院校名称',
    major         VARCHAR(120) COMMENT '就读专业',
    degree        VARCHAR(60) COMMENT '学历层次，如本科/硕士',
    start_date    DATE COMMENT '入学日期',
    end_date      DATE COMMENT '毕业/结束日期',
    description   TEXT COMMENT '经历描述或荣誉',
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) COMMENT='学生教育经历表';

CREATE TABLE IF NOT EXISTS student_experience (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    student_id    BIGINT      NOT NULL COMMENT '关联学生资料表student_profile的ID',
    title         VARCHAR(150) NOT NULL COMMENT '经历名称，如岗位/项目标题',
    organization  VARCHAR(150) COMMENT '所属单位或组织名称',
    start_date    DATE COMMENT '经历开始日期',
    end_date      DATE COMMENT '经历结束日期',
    description   TEXT COMMENT '经历详细描述',
    experience_type ENUM('INTERNSHIP','PROJECT','VOLUNTEER','OTHER') DEFAULT 'OTHER' COMMENT '经历类型：实习/项目/志愿/其他',
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) COMMENT='学生实践与工作经历表';

CREATE TABLE IF NOT EXISTS student_award (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    student_id  BIGINT      NOT NULL COMMENT '关联学生资料表student_profile的ID',
    name        VARCHAR(150) NOT NULL COMMENT '奖项名称',
    award_date  DATE COMMENT '获奖日期',
    level       VARCHAR(60) COMMENT '奖项级别，如校级/省级',
    description TEXT COMMENT '奖项说明或备注',
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) COMMENT='学生获奖信息表';

CREATE TABLE IF NOT EXISTS employment_intention (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    student_id        BIGINT      NOT NULL UNIQUE COMMENT '关联学生资料表student_profile的ID（唯一）',
    expected_position VARCHAR(120) COMMENT '期望职位名称',
    salary_range      VARCHAR(60) COMMENT '期望薪资范围',
    work_type         ENUM('FULL_TIME','PART_TIME','INTERNSHIP','FLEXIBLE') COMMENT '期望工作类型：全职/兼职/实习/灵活',
    notes             TEXT COMMENT '其他就业意向备注',
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) COMMENT='学生就业意向表';

CREATE TABLE IF NOT EXISTS employment_intention_city (
    intention_id BIGINT NOT NULL COMMENT '关联就业意向表employment_intention的ID',
    city         VARCHAR(80) NOT NULL COMMENT '意向就业城市',
    PRIMARY KEY (intention_id, city),
    FOREIGN KEY (intention_id) REFERENCES employment_intention(id)
) COMMENT='学生就业意向城市表';

CREATE TABLE IF NOT EXISTS resume (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    student_id    BIGINT      NOT NULL COMMENT '关联学生资料表student_profile的ID',
    title         VARCHAR(150) NOT NULL COMMENT '简历标题',
    summary       TEXT COMMENT '个人概述或求职目标',
    portfolio_url VARCHAR(255) COMMENT '作品集链接',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '简历创建时间',
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '简历最后更新时间',
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) COMMENT='学生简历主表';

CREATE TABLE IF NOT EXISTS resume_skill (
    resume_id BIGINT      NOT NULL COMMENT '关联简历表resume的ID',
    skill     VARCHAR(80) NOT NULL COMMENT '技能名称',
    proficiency TINYINT COMMENT '熟练度评级，数值越大越熟练',
    PRIMARY KEY (resume_id, skill),
    FOREIGN KEY (resume_id) REFERENCES resume(id)
) COMMENT='简历技能明细表';

CREATE TABLE IF NOT EXISTS resume_experience (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    resume_id   BIGINT      NOT NULL COMMENT '关联简历表resume的ID',
    title       VARCHAR(150) NOT NULL COMMENT '经历标题，如岗位/项目名称',
    organization VARCHAR(150) COMMENT '所在企业或组织',
    start_date  DATE COMMENT '开始日期',
    end_date    DATE COMMENT '结束日期',
    description TEXT COMMENT '经历内容描述',
    FOREIGN KEY (resume_id) REFERENCES resume(id)
) COMMENT='简历附加经历表';

CREATE TABLE IF NOT EXISTS employer (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    user_id        BIGINT      NOT NULL COMMENT '关联sys_user的ID',
    company_name   VARCHAR(150) NOT NULL COMMENT '企业名称',
    contact_person VARCHAR(100) COMMENT '企业联系人',
    contact_email  VARCHAR(120) COMMENT '联系人邮箱',
    contact_phone  VARCHAR(40) COMMENT '联系人电话',
    description    TEXT COMMENT '企业简介',
    website        VARCHAR(200) COMMENT '企业官网网址',
    CONSTRAINT fk_employer_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='企业信息表';

CREATE TABLE IF NOT EXISTS job_posting (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    employer_id    BIGINT      NOT NULL COMMENT '关联企业表employer的ID',
    title          VARCHAR(150) NOT NULL COMMENT '职位标题',
    description    TEXT COMMENT '职位详细描述',
    salary_range   VARCHAR(80) COMMENT '薪资范围描述',
    location       VARCHAR(120) COMMENT '工作地点',
    work_type      ENUM('FULL_TIME','PART_TIME','INTERNSHIP','REMOTE') COMMENT '工作类型：全职/兼职/实习/远程',
    status         ENUM('OPEN','CLOSED','DRAFT') DEFAULT 'OPEN' COMMENT '岗位状态：开放/关闭/草稿',
    published_date DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '职位发布时间',
    closing_date   DATE COMMENT '职位截止日期',
    FOREIGN KEY (employer_id) REFERENCES employer(id)
) COMMENT='招聘岗位发布表';

CREATE TABLE IF NOT EXISTS job_requirement (
    job_id      BIGINT      NOT NULL COMMENT '关联岗位表job_posting的ID',
    requirement VARCHAR(200) NOT NULL COMMENT '岗位要求描述',
    PRIMARY KEY (job_id, requirement),
    FOREIGN KEY (job_id) REFERENCES job_posting(id)
) COMMENT='岗位要求列表';

CREATE TABLE IF NOT EXISTS job_application (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    job_id       BIGINT      NOT NULL COMMENT '关联招聘岗位表job_posting的ID',
    student_id   BIGINT      NOT NULL COMMENT '关联学生资料表student_profile的ID',
    resume_id    BIGINT      NOT NULL COMMENT '关联简历表resume的ID',
    status       ENUM('SUBMITTED','REVIEWING','INTERVIEW','OFFERED','REJECTED') DEFAULT 'SUBMITTED' COMMENT '申请状态：提交/筛选/面试/已录用/已拒绝',
    cover_letter TEXT COMMENT '求职信内容',
    applied_at   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
    FOREIGN KEY (job_id) REFERENCES job_posting(id),
    FOREIGN KEY (student_id) REFERENCES student_profile(id),
    FOREIGN KEY (resume_id) REFERENCES resume(id)
) COMMENT='学生岗位申请记录表';

CREATE TABLE IF NOT EXISTS interview (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    job_id          BIGINT      NOT NULL COMMENT '关联招聘岗位表job_posting的ID',
    application_id  BIGINT      NOT NULL COMMENT '关联岗位申请表job_application的ID',
    scheduled_time  DATETIME    NOT NULL COMMENT '面试预约时间',
    location        VARCHAR(200) COMMENT '线下面试地点',
    meeting_link    VARCHAR(255) COMMENT '线上面试会议链接',
    status          ENUM('SCHEDULED','COMPLETED','CANCELLED') DEFAULT 'SCHEDULED' COMMENT '面试状态：已安排/已完成/已取消',
    feedback        TEXT COMMENT '面试反馈或评语',
    FOREIGN KEY (job_id) REFERENCES job_posting(id),
    FOREIGN KEY (application_id) REFERENCES job_application(id)
) COMMENT='面试安排记录表';

CREATE TABLE IF NOT EXISTS teacher (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    user_id     BIGINT      NOT NULL COMMENT '关联系统用户表sys_user的ID',
    department  VARCHAR(120) COMMENT '所属院系或部门',
    email       VARCHAR(120) COMMENT '教师联系邮箱',
    phone       VARCHAR(30) COMMENT '教师联系电话',
    major       VARCHAR(120) COMMENT '指导专业方向',
    focus       VARCHAR(255) COMMENT '研究/指导方向',
    biography   TEXT COMMENT '教师个人简介',
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='教师信息表';

CREATE TABLE IF NOT EXISTS teacher_guidance (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    teacher_id  BIGINT      NOT NULL COMMENT '关联教师信息表teacher的ID',
    student_id  BIGINT      NOT NULL COMMENT '关联学生资料表student_profile的ID',
    note        TEXT COMMENT '指导记录内容',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) COMMENT='教师就业指导记录表';

CREATE TABLE IF NOT EXISTS public_search_history (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    user_id          BIGINT COMMENT '发起搜索的用户ID',
    keyword          VARCHAR(200) NOT NULL COMMENT '搜索关键字',
    role_filter      VARCHAR(60) COMMENT '角色筛选条件',
    category_filter  VARCHAR(120) COMMENT '模块筛选条件',
    location_filter  VARCHAR(120) COMMENT '地点筛选条件',
    advanced_options TEXT COMMENT '高级筛选参数',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间'
) COMMENT='公共模块搜索历史记录';

CREATE TABLE IF NOT EXISTS public_resource (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    uploader_id  BIGINT COMMENT '上传人用户ID',
    file_name    VARCHAR(255) NOT NULL COMMENT '文件原始名称',
    file_type    VARCHAR(120) COMMENT '文件类型',
    file_size    BIGINT COMMENT '文件大小(字节)',
    storage_path VARCHAR(500) NOT NULL COMMENT '文件物理存储路径',
    download_url VARCHAR(255) COMMENT '文件下载接口地址',
    description  TEXT COMMENT '文件说明或备注',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    FOREIGN KEY (uploader_id) REFERENCES sys_user(id)
) COMMENT='公共资料库文件表';

CREATE TABLE IF NOT EXISTS system_notification (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    user_id     BIGINT      NOT NULL COMMENT '关联系统用户表sys_user的ID',
    category    ENUM('SYSTEM','INTERVIEW','APPLICATION','GUIDANCE') DEFAULT 'SYSTEM' COMMENT '通知类型：系统/面试/申请/指导',
    title       VARCHAR(150) NOT NULL COMMENT '通知标题',
    content     TEXT COMMENT '通知正文内容',
    read_flag   TINYINT(1) DEFAULT 0 COMMENT '阅读标记：0未读，1已读',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='系统通知记录表';

CREATE TABLE IF NOT EXISTS student_profile_update_request (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，自增',
    student_id    BIGINT NOT NULL COMMENT '关联学生资料表student_profile的ID',
    gender        VARCHAR(10) COMMENT '更新的性别',
    age           INT COMMENT '更新的年龄',
    major         VARCHAR(100) COMMENT '更新的专业',
    biography     TEXT COMMENT '更新的个人简介',
    graduation_year INT COMMENT '更新的预计毕业年份',
    status        ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' COMMENT '审核状态：待审核/已批准/已拒绝',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '请求创建时间',
    reviewed_at   DATETIME COMMENT '审核时间',
    reviewer_id   BIGINT COMMENT '审核人ID（教师或管理员）',
    homeroom_teacher_id BIGINT DEFAULT NULL COMMENT '班主任教师ID',
    review_comment TEXT COMMENT '审核意见',
    FOREIGN KEY (student_id) REFERENCES student_profile(id),
    FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) COMMENT='学生资料更新请求表';

