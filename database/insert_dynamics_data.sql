-- 资讯动态示例数据（供资讯动态页浏览）
-- 执行前请确保 sys_user 中至少存在 id=1 的用户（如管理员），否则请先修改下方 user_id 为已有用户ID
-- 若表已有数据可先清空： DELETE FROM system_notification;

INSERT INTO system_notification (user_id, category, title, content, read_flag, created_at) VALUES
(1, 'SYSTEM', '就业服务系统升级通知', '为提升使用体验，就业服务系统将于本周末进行例行升级维护。维护期间部分功能可能短暂不可用，请提前保存未提交的数据。感谢您的理解与支持。', 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'SYSTEM', '2025届毕业生就业摸底调查启动', '请各院系配合完成2025届毕业生就业意向摸底调查，填报截止时间为本月末。数据将用于精准推送岗位与指导服务。', 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 'INTERVIEW', '春季校园招聘会面试安排提醒', '本周五下午将举行春季校园招聘会集中面试，请已投递简历的同学留意短信与站内消息，按时参加。面试地点：就业指导中心二楼。', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 'INTERVIEW', '某科技公司一面结果公布', '参与某科技公司一面同学的结果已通过系统发布，请登录「求职申请」查看。通过者将在一周内收到二面安排。', 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 'APPLICATION', '简历投递状态更新说明', '即日起，企业查看简历、标记「感兴趣」等操作会实时同步到您的申请状态，请定期登录查看进展。', 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 'APPLICATION', '热门岗位推荐：软件开发方向', '根据您的专业与意向，系统为您推荐了若干软件开发类岗位，请至「职位招聘」查看并投递。', 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1, 'GUIDANCE', '就业指导开放预约', '就业指导中心本周开放一对一咨询预约，涵盖简历修改、面试模拟与职业规划。请有需要的同学在系统中预约时段。', 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'GUIDANCE', '求职心理调适线上讲座', '本周三晚将举办「求职季心理调适」线上讲座，欢迎报名参与。链接与会议号将提前一天发送至站内信。', 0, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1, 'SYSTEM', '个人信息与隐私保护声明更新', '我们更新了个人信息与隐私保护相关说明，请在使用服务前阅读最新版本。如有疑问可联系就业办。', 0, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1, 'INTERVIEW', '多家企业联合宣讲会时间确定', '多家合作企业联合宣讲会定于下周二下午两点，地点：大礼堂。届时将介绍企业文化与招聘计划，欢迎到场参加。', 0, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 若自增主键需从某值起，可执行： ALTER TABLE system_notification AUTO_INCREMENT = 1;
