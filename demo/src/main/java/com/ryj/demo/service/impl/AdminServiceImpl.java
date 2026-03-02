package com.ryj.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ryj.demo.dto.AdminDashboardResponse;
import com.ryj.demo.dto.AdminNotificationRequest;
import com.ryj.demo.dto.AdminUserRequest;
import com.ryj.demo.dto.TeacherProfileUpdateRequest;
import com.ryj.demo.dto.EmployerProfileRequest;
import com.ryj.demo.entity.*;
import com.ryj.demo.mapper.SysUserMapper;
import com.ryj.demo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements AdminService {

    private final PasswordEncoder passwordEncoder;
    private final StudentProfileService studentProfileService;
    private final EmployerService employerService;
    private final TeacherService teacherService;
    private final JobPostingService jobPostingService;
    private final JobApplicationService jobApplicationService;
    private final ResumeService resumeService;
    private final SystemNotificationService notificationService;
    private final InterviewService interviewService;
    private final TeacherGuidanceService guidanceService;
    private final PublicSearchHistoryService searchHistoryService;
    private final PublicResourceService publicResourceService;
    private final EmploymentIntentionService employmentIntentionService;
    private final EmploymentIntentionCityService employmentIntentionCityService;
    private final ResumeSkillService resumeSkillService;
    private final ResumeExperienceService resumeExperienceService;
    private final StudentAwardService studentAwardService;
    private final JobRequirementService jobRequirementService;
    private final StudentProfileUpdateRequestService studentProfileUpdateRequestService;
    private final ProfileUpdateRequestService profileUpdateRequestService;
    private final SysUserService sysUserService;

    @Override
    public void requireAdminRole(Long userId) {
        SysUser user = getById(userId);
        if (user == null || user.getRole() != SysUser.Role.ADMIN) {
            throw new IllegalArgumentException("仅管理员可以访问该功能");
        }
    }

    @Override
    public AdminDashboardResponse getDashboardData(Long adminUserId) {
        requireAdminRole(adminUserId);
        
        AdminDashboardResponse response = new AdminDashboardResponse();
        
        // 构建Header
        AdminDashboardResponse.Header header = new AdminDashboardResponse.Header();
        header.setTotalUsers(count());
        header.setActiveUsers(count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, SysUser.Status.ACTIVE)));
        header.setTotalStudents(count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, SysUser.Role.STUDENT)));
        header.setTotalEmployers(count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, SysUser.Role.EMPLOYER)));
        header.setTotalTeachers(count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, SysUser.Role.TEACHER)));
        header.setTotalJobPostings(jobPostingService.count());
        header.setActiveJobPostings(jobPostingService.count(new LambdaQueryWrapper<JobPosting>()
                .eq(JobPosting::getStatus, JobPosting.Status.OPEN)));
        header.setTotalApplications(jobApplicationService.count());
        header.setPendingNotifications(notificationService.count(new LambdaQueryWrapper<SystemNotification>()
                .eq(SystemNotification::getReadFlag, false)));
        response.setHeader(header);
        
        // 构建模块信息
        response.setModules(buildModules());
        
        // 构建用户统计
        response.setUserStatistics(buildUserStatistics());
        
        // 构建内容统计
        response.setContentStatistics(buildContentStatistics());
        
        // 构建系统统计
        response.setSystemStatistics(buildSystemStatistics());
        
        // 最近注册的用户
        response.setRecentUsers(buildRecentUsers());
        
        // 最近活动（简化版，实际应该从日志表读取）
        response.setRecentActivities(buildRecentActivities());
        
        return response;
    }

    @Override
    @Transactional
    public SysUser createOrUpdateUser(AdminUserRequest request, Long userId) {
        requireAdminRole(userId);
        
        SysUser user;
        if (request.getUsername() != null) {
            Optional<SysUser> existing = findByUsername(request.getUsername());
            if (existing.isPresent() && !existing.get().getId().equals(userId)) {
                throw new IllegalArgumentException("用户名已存在");
            }
            user = existing.orElse(new SysUser());
        } else {
            user = new SysUser();
        }
        
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            try {
                user.setRole(SysUser.Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的角色类型: " + request.getRole());
            }
        }
        if (request.getStatus() != null) {
            try {
                user.setStatus(SysUser.Status.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的状态类型: " + request.getStatus());
            }
        } else if (user.getId() == null) {
            user.setStatus(SysUser.Status.ACTIVE);
        }
        
        if (user.getId() == null) {
            save(user);
            // 如果是学生，自动创建student_profile
            if (user.getRole() == SysUser.Role.STUDENT) {
                StudentProfile profile = new StudentProfile();
                profile.setId(user.getId());
                studentProfileService.save(profile);
            }
            // 如果是企业，自动创建employer
            else if (user.getRole() == SysUser.Role.EMPLOYER) {
                Employer employer = new Employer();
                employer.setUserId(user.getId());
                employer.setCompanyName(Optional.ofNullable(user.getFullName())
                        .filter(name -> !name.isBlank())
                        .orElse(user.getUsername() + "企业"));
                employer.setContactPerson(user.getFullName());
                employer.setContactEmail(user.getEmail());
                employer.setContactPhone(user.getPhone());
                employerService.save(employer);
            }
        } else {
            updateById(user);
        }
        
        return user;
    }

    @Override
    @Transactional
    public boolean updateUserStatus(Long userId, SysUser.Status status) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status);
        return updateById(user);
    }

    @Override
    @Transactional
    public boolean updateUserRole(Long userId, SysUser.Role role) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setRole(role);
        return updateById(user);
    }

    @Override
    @Transactional
    public boolean sendSystemNotification(Long adminUserId, AdminNotificationRequest request) {
        if (request.getUserId() != null) {
            // 发送给指定用户，但不发送给管理员自己
            if (request.getUserId().equals(adminUserId)) {
                // 管理员给自己发消息，直接返回成功但不实际发送
                return true;
            }
            SystemNotification notification = new SystemNotification();
            notification.setUserId(request.getUserId());
            notification.setCategory(request.getCategory());
            notification.setTitle(request.getTitle());
            notification.setContent(request.getContent());
            notification.setReadFlag(request.getReadFlag());
            notification.setCreatedAt(LocalDateTime.now());
            return notificationService.save(notification);
        } else {
            // 发送给所有用户，但排除管理员自己
            List<SysUser> allUsers = list();
            for (SysUser user : allUsers) {
                // 跳过管理员自己
                if (user.getId().equals(adminUserId)) {
                    continue;
                }
                SystemNotification notification = new SystemNotification();
                notification.setUserId(user.getId());
                notification.setCategory(request.getCategory());
                notification.setTitle(request.getTitle());
                notification.setContent(request.getContent());
                notification.setReadFlag(false);
                notification.setCreatedAt(LocalDateTime.now());
                notificationService.save(notification);
            }
            return true;
        }
    }

    private List<AdminDashboardResponse.ModuleInfo> buildModules() {
        List<AdminDashboardResponse.ModuleInfo> modules = new ArrayList<>();
        modules.add(module("overview", "管理员总览", "系统整体数据概览", List.of("用户统计", "内容统计", "系统监控")));
        modules.add(module("users", "用户管理", "管理所有用户账户", List.of("用户创建", "角色分配", "状态管理")));
        modules.add(module("content", "内容管理", "管理系统内容", List.of("通知管理", "内容审核", "信息维护")));
        modules.add(module("statistics", "数据统计", "查看系统使用数据", List.of("用户活跃度", "内容发布量", "系统使用统计")));
        modules.add(module("system", "系统配置", "系统参数配置", List.of("参数设置", "权限配置", "系统维护")));
        return modules;
    }

    private AdminDashboardResponse.ModuleInfo module(String key, String name, String description, List<String> capabilities) {
        AdminDashboardResponse.ModuleInfo module = new AdminDashboardResponse.ModuleInfo();
        module.setKey(key);
        module.setName(name);
        module.setDescription(description);
        module.setCapabilities(capabilities);
        return module;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        // 记录删除操作开始
        System.out.println("开始删除用户: " + userId + ", 角色: " + user.getRole());
        
        // 先删除所有可能关联的teacher记录（无论用户角色是什么，因为可能存在数据不一致的情况）
        // 使用list()方法更安全，避免getOne()可能抛出的异常
        List<Teacher> teachers = teacherService.list(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId));
        for (Teacher teacher : teachers) {
            Long teacherId = teacher.getId();
            // 删除教师指导记录
            guidanceService.remove(new LambdaQueryWrapper<TeacherGuidance>()
                    .eq(TeacherGuidance::getTeacherId, teacherId));
            // 删除教师信息
            teacherService.removeById(teacherId);
        }
        
        // 根据用户角色删除相关数据
        if (user.getRole() == SysUser.Role.STUDENT) {
            // 删除学生相关数据
            Long studentId = user.getId();
            
            // 删除教师指导记录
            guidanceService.list(new LambdaQueryWrapper<TeacherGuidance>()
                    .eq(TeacherGuidance::getStudentId, studentId))
                    .forEach(guidance -> guidanceService.removeById(guidance.getId()));
            
            // 删除面试记录（通过job_application关联）
            List<JobApplication> applications = jobApplicationService.list(
                    new LambdaQueryWrapper<JobApplication>()
                            .eq(JobApplication::getStudentId, studentId));
            for (JobApplication app : applications) {
                interviewService.list(new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getApplicationId, app.getId()))
                        .forEach(interview -> interviewService.removeById(interview.getId()));
            }
            
            // 删除求职申请
            jobApplicationService.remove(new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getStudentId, studentId));
            
            // 删除就业意向城市
            List<EmploymentIntention> intentions = employmentIntentionService.list(
                    new LambdaQueryWrapper<EmploymentIntention>()
                            .eq(EmploymentIntention::getStudentId, studentId));
            for (EmploymentIntention intention : intentions) {
                employmentIntentionCityService.replaceCities(intention.getId(), Collections.emptyList());
            }
            
            // 删除就业意向
            employmentIntentionService.remove(new LambdaQueryWrapper<EmploymentIntention>()
                    .eq(EmploymentIntention::getStudentId, studentId));
            
            // 删除简历技能和经验
            List<Resume> resumes = resumeService.list(new LambdaQueryWrapper<Resume>()
                    .eq(Resume::getStudentId, studentId));
            for (Resume resume : resumes) {
                // ResumeSkillService使用自定义方法
                resumeSkillService.replaceSkills(resume.getId(), Collections.emptyList());
                resumeExperienceService.remove(new LambdaQueryWrapper<ResumeExperience>()
                        .eq(ResumeExperience::getResumeId, resume.getId()));
            }
            
            // 删除简历
            resumeService.remove(new LambdaQueryWrapper<Resume>()
                    .eq(Resume::getStudentId, studentId));
            
            // 删除学生获奖记录
            studentAwardService.remove(new LambdaQueryWrapper<StudentAward>()
                    .eq(StudentAward::getStudentId, studentId));
            
            // 删除学生资料更新请求
            studentProfileUpdateRequestService.remove(new LambdaQueryWrapper<StudentProfileUpdateRequest>()
                    .eq(StudentProfileUpdateRequest::getStudentId, studentId));
            
            // 删除学生资料
            studentProfileService.removeById(studentId);
            
        } else if (user.getRole() == SysUser.Role.EMPLOYER) {
            // 删除企业相关数据
            List<Employer> employers = employerService.list(new LambdaQueryWrapper<Employer>()
                    .eq(Employer::getUserId, userId));
            for (Employer employer : employers) {
                Long employerId = employer.getId();
                
                // 删除岗位要求
                List<JobPosting> postings = jobPostingService.list(
                        new LambdaQueryWrapper<JobPosting>()
                                .eq(JobPosting::getEmployerId, employerId));
                for (JobPosting posting : postings) {
                    jobRequirementService.replaceRequirements(posting.getId(), Collections.emptyList());
                }
                
                // 删除岗位发布
                jobPostingService.remove(new LambdaQueryWrapper<JobPosting>()
                        .eq(JobPosting::getEmployerId, employerId));
                
                // 删除企业信息
                employerService.removeById(employerId);
            }
        }
        
        // 删除系统通知
        notificationService.remove(new LambdaQueryWrapper<SystemNotification>()
                .eq(SystemNotification::getUserId, userId));
        
        // 删除搜索历史
        searchHistoryService.remove(new LambdaQueryWrapper<PublicSearchHistory>()
                .eq(PublicSearchHistory::getUserId, userId));
        
        // 删除公共资源（上传的文件）- 必须在使用者之前删除，因为有外键约束
        // 只删除 uploader_id 等于 userId 的记录
        long deletedResources = publicResourceService.count(new LambdaQueryWrapper<PublicResource>()
                .eq(PublicResource::getUploaderId, userId));
        System.out.println("找到 " + deletedResources + " 条公共资源记录需要删除");
        boolean resourceDeleted = publicResourceService.remove(new LambdaQueryWrapper<PublicResource>()
                .eq(PublicResource::getUploaderId, userId));
        System.out.println("删除公共资源结果: " + resourceDeleted);
        
        // 最后删除用户
        System.out.println("准备删除用户记录: " + userId);
        boolean result = removeById(userId);
        System.out.println("删除用户结果: " + result);
        if (!result) {
            throw new RuntimeException("删除用户失败");
        }
        return result;
    }

    @Override
    public Page<ProfileUpdateRequest> getProfileUpdateRequests(Long adminUserId,
                                                               String role,
                                                               String status,
                                                               long page,
                                                               long size) {
        requireAdminRole(adminUserId);
        LambdaQueryWrapper<ProfileUpdateRequest> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isBlank()) {
            wrapper.eq(ProfileUpdateRequest::getRole, role.toUpperCase());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ProfileUpdateRequest::getStatus, status.toUpperCase());
        }
        wrapper.orderByDesc(ProfileUpdateRequest::getCreatedAt);
        return profileUpdateRequestService.page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public boolean approveProfileUpdateRequest(Long adminUserId, Long requestId, String reviewComment) {
        requireAdminRole(adminUserId);
        ProfileUpdateRequest req = profileUpdateRequestService.getById(requestId);
        if (req == null) {
            throw new IllegalArgumentException("未找到资料更新申请");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalStateException("该申请已被处理，当前状态为: " + req.getStatus());
        }

        SysUser user = sysUserService.getById(req.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("关联用户不存在");
        }

        // 根据角色应用变更
        try {
            if ("TEACHER".equalsIgnoreCase(req.getRole())) {
                applyTeacherProfileUpdate(user, req.getPayload());
            } else if ("EMPLOYER".equalsIgnoreCase(req.getRole())) {
                applyEmployerProfileUpdate(user, req.getPayload());
            } else {
                throw new IllegalArgumentException("不支持的角色类型: " + req.getRole());
            }
        } catch (Exception e) {
            throw new RuntimeException("应用资料变更失败: " + e.getMessage(), e);
        }

        req.setStatus("APPROVED");
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewerId(adminUserId);
        req.setReviewComment(reviewComment != null ? reviewComment.trim() : null);
        profileUpdateRequestService.updateById(req);

        // 通知用户审核通过
        SystemNotification n = new SystemNotification();
        n.setUserId(user.getId());
        n.setCategory(SystemNotification.Category.SYSTEM);
        n.setTitle("个人资料变更已通过审核");
        n.setContent("您提交的个人资料变更申请已通过管理员审核。");
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);

        return true;
    }

    @Override
    @Transactional
    public boolean rejectProfileUpdateRequest(Long adminUserId, Long requestId, String reviewComment) {
        requireAdminRole(adminUserId);
        ProfileUpdateRequest req = profileUpdateRequestService.getById(requestId);
        if (req == null) {
            throw new IllegalArgumentException("未找到资料更新申请");
        }
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalStateException("该申请已被处理，当前状态为: " + req.getStatus());
        }

        SysUser user = sysUserService.getById(req.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("关联用户不存在");
        }

        req.setStatus("REJECTED");
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewerId(adminUserId);
        req.setReviewComment(reviewComment != null ? reviewComment.trim() : null);
        profileUpdateRequestService.updateById(req);

        // 通知用户审核被驳回
        SystemNotification n = new SystemNotification();
        n.setUserId(user.getId());
        n.setCategory(SystemNotification.Category.SYSTEM);
        n.setTitle("个人资料变更未通过审核");
        String reason = (reviewComment != null && !reviewComment.isBlank())
                ? "，原因：" + reviewComment.trim()
                : "";
        n.setContent("您提交的个人资料变更申请未通过管理员审核" + reason + "。");
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);

        return true;
    }

    private void applyTeacherProfileUpdate(SysUser user, String payload) throws Exception {
        if (payload == null || payload.isBlank()) {
            return;
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        TeacherProfileUpdateRequest dto = mapper.readValue(payload, TeacherProfileUpdateRequest.class);

        // 找到 teacher 记录（教师账号应该已经绑定 teacher 记录）
        Teacher teacher = teacherService.getByUserId(user.getId());
        if (teacher == null || teacher.getId() == null) {
            throw new IllegalStateException("未找到教师档案记录，无法应用更新");
        }
        teacherService.updateProfile(teacher.getId(), dto);
    }

    private void applyEmployerProfileUpdate(SysUser user, String payload) throws Exception {
        if (payload == null || payload.isBlank()) {
            return;
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        EmployerProfileRequest dto = mapper.readValue(payload, EmployerProfileRequest.class);

        Employer employer = employerService.findByUserId(user.getId())
                .orElseGet(() -> {
                    Employer created = new Employer();
                    created.setUserId(user.getId());
                    return created;
                });
        if (dto.getCompanyName() != null) {
            employer.setCompanyName(dto.getCompanyName().trim());
        }
        employer.setContactPerson(normalize(dto.getContactPerson()));
        employer.setContactEmail(normalize(dto.getContactEmail()));
        employer.setContactPhone(normalize(dto.getContactPhone()));
        employer.setDescription(normalize(dto.getDescription()));
        employer.setWebsite(normalize(dto.getWebsite()));

        if (employer.getId() == null) {
            employerService.save(employer);
        } else {
            employerService.updateById(employer);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AdminDashboardResponse.UserStatistics buildUserStatistics() {
        AdminDashboardResponse.UserStatistics stats = new AdminDashboardResponse.UserStatistics();
        stats.setTotalUsers(count());
        stats.setActiveUsers(count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, SysUser.Status.ACTIVE)));
        stats.setDisabledUsers(count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, SysUser.Status.DISABLED)));
        
        Map<SysUser.Role, Long> usersByRole = new HashMap<>();
        Map<SysUser.Role, Long> activeUsersByRole = new HashMap<>();
        for (SysUser.Role role : SysUser.Role.values()) {
            usersByRole.put(role, count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, role)));
            activeUsersByRole.put(role, count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, role)
                    .eq(SysUser::getStatus, SysUser.Status.ACTIVE)));
        }
        stats.setUsersByRole(usersByRole);
        stats.setActiveUsersByRole(activeUsersByRole);
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        stats.setNewUsersLast30Days(count(new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreatedAt, thirtyDaysAgo)));
        
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        stats.setActiveUsersLast7Days(count(new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreatedAt, sevenDaysAgo)
                .eq(SysUser::getStatus, SysUser.Status.ACTIVE)));
        
        // 最近30天每日新增用户统计
        stats.setDailyNewUsersLast30Days(buildDailyUserStats(30, true));
        
        // 最近30天每日活跃用户统计（基于updatedAt）
        stats.setDailyActiveUsersLast30Days(buildDailyActiveUserStats(30));
        
        return stats;
    }

    private AdminDashboardResponse.ContentStatistics buildContentStatistics() {
        AdminDashboardResponse.ContentStatistics stats = new AdminDashboardResponse.ContentStatistics();
        stats.setTotalJobPostings(jobPostingService.count());
        stats.setActiveJobPostings(jobPostingService.count(new LambdaQueryWrapper<JobPosting>()
                .eq(JobPosting::getStatus, JobPosting.Status.OPEN)));
        stats.setTotalApplications(jobApplicationService.count());
        stats.setTotalResumes(resumeService.count());
        stats.setTotalNotifications(notificationService.count());
        stats.setUnreadNotifications(notificationService.count(new LambdaQueryWrapper<SystemNotification>()
                .eq(SystemNotification::getReadFlag, false)));
        
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // 最近7天统计
        stats.setJobPostingsLast7Days(jobPostingService.count(new LambdaQueryWrapper<JobPosting>()
                .ge(JobPosting::getPublishedDate, sevenDaysAgo)));
        stats.setApplicationsLast7Days(jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                .ge(JobApplication::getAppliedAt, sevenDaysAgo)));
        stats.setResumesLast7Days(resumeService.count(new LambdaQueryWrapper<Resume>()
                .ge(Resume::getCreatedAt, sevenDaysAgo)));
        stats.setNotificationsLast7Days(notificationService.count(new LambdaQueryWrapper<SystemNotification>()
                .ge(SystemNotification::getCreatedAt, sevenDaysAgo)));
        
        // 最近30天每日统计
        stats.setDailyJobPostingsLast30Days(buildDailyContentStats("jobPosting", 30));
        stats.setDailyApplicationsLast30Days(buildDailyContentStats("application", 30));
        stats.setDailyResumesLast30Days(buildDailyContentStats("resume", 30));
        stats.setDailyNotificationsLast30Days(buildDailyContentStats("notification", 30));
        
        // 内容按类型统计
        Map<String, Long> contentByType = new HashMap<>();
        contentByType.put("职位发布", jobPostingService.count());
        contentByType.put("求职申请", jobApplicationService.count());
        contentByType.put("简历", resumeService.count());
        contentByType.put("系统通知", notificationService.count());
        stats.setContentByType(contentByType);
        
        return stats;
    }

    private AdminDashboardResponse.SystemStatistics buildSystemStatistics() {
        AdminDashboardResponse.SystemStatistics stats = new AdminDashboardResponse.SystemStatistics();
        stats.setTotalInterviews(interviewService.count());
        stats.setScheduledInterviews(interviewService.count(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, Interview.Status.SCHEDULED)));
        stats.setCompletedInterviews(interviewService.count(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, Interview.Status.COMPLETED)));
        stats.setTotalGuidanceRecords(guidanceService.count());
        stats.setTotalSearchHistory(searchHistoryService.count());
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // 最近30天系统使用统计
        stats.setSearchesLast30Days(searchHistoryService.count(new LambdaQueryWrapper<PublicSearchHistory>()
                .ge(PublicSearchHistory::getCreatedAt, thirtyDaysAgo)));
        stats.setGuidanceRecordsLast30Days(guidanceService.count(new LambdaQueryWrapper<TeacherGuidance>()
                .ge(TeacherGuidance::getCreatedAt, thirtyDaysAgo)));
        stats.setInterviewsLast30Days(interviewService.count(new LambdaQueryWrapper<Interview>()
                .ge(Interview::getScheduledTime, thirtyDaysAgo)));
        stats.setPublicResourcesLast30Days(publicResourceService.count(new LambdaQueryWrapper<PublicResource>()
                .ge(PublicResource::getCreatedAt, thirtyDaysAgo)));
        
        // 最近30天每日搜索统计
        stats.setDailySearchesLast30Days(buildDailySearchStats(30));
        
        // 系统活动按类型统计
        Map<String, Long> systemActivityByType = new HashMap<>();
        systemActivityByType.put("面试", interviewService.count());
        systemActivityByType.put("就业指导", guidanceService.count());
        systemActivityByType.put("搜索记录", searchHistoryService.count());
        systemActivityByType.put("公共资源", publicResourceService.count());
        stats.setSystemActivityByType(systemActivityByType);
        
        return stats;
    }

    private List<AdminDashboardResponse.UserOverview> buildRecentUsers() {
        List<SysUser> users = list(new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt)
                .last("limit 10"));
        
        return users.stream().map(user -> {
            AdminDashboardResponse.UserOverview overview = new AdminDashboardResponse.UserOverview();
            overview.setId(user.getId());
            overview.setUsername(user.getUsername());
            overview.setFullName(user.getFullName());
            overview.setRole(user.getRole());
            overview.setStatus(user.getStatus());
            overview.setEmail(user.getEmail());
            overview.setPhone(user.getPhone());
            overview.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            overview.setLastActiveAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
            return overview;
        }).collect(Collectors.toList());
    }

    private List<AdminDashboardResponse.ActivityLog> buildRecentActivities() {
        // 简化版：从最近的通知和用户创建记录构建活动日志
        // 实际应该从专门的操作日志表读取
        List<AdminDashboardResponse.ActivityLog> activities = new ArrayList<>();
        
        // 从最近创建的用户构建活动
        List<SysUser> recentUsers = list(new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt)
                .last("limit 5"));
        
        for (SysUser user : recentUsers) {
            AdminDashboardResponse.ActivityLog log = new AdminDashboardResponse.ActivityLog();
            log.setAction("用户注册");
            log.setDescription("新用户注册: " + user.getUsername());
            log.setUserId(user.getId().toString());
            log.setUserName(user.getFullName() != null ? user.getFullName() : user.getUsername());
            log.setTimestamp(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            log.setCategory("用户管理");
            activities.add(log);
        }
        
        return activities;
    }

    private Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)));
    }
    
    /**
     * 构建每日用户统计（新增用户）
     */
    private List<AdminDashboardResponse.DailyStat> buildDailyUserStats(int days, boolean newUsers) {
        List<AdminDashboardResponse.DailyStat> stats = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            long count;
            if (newUsers) {
                count = count(new LambdaQueryWrapper<SysUser>()
                        .ge(SysUser::getCreatedAt, startOfDay)
                        .le(SysUser::getCreatedAt, endOfDay));
            } else {
                count = count(new LambdaQueryWrapper<SysUser>()
                        .ge(SysUser::getUpdatedAt, startOfDay)
                        .le(SysUser::getUpdatedAt, endOfDay)
                        .eq(SysUser::getStatus, SysUser.Status.ACTIVE));
            }
            
            AdminDashboardResponse.DailyStat stat = new AdminDashboardResponse.DailyStat();
            stat.setDate(date.format(formatter));
            stat.setCount(count);
            stats.add(stat);
        }
        
        return stats;
    }
    
    /**
     * 构建每日活跃用户统计（基于updatedAt）
     */
    private List<AdminDashboardResponse.DailyStat> buildDailyActiveUserStats(int days) {
        return buildDailyUserStats(days, false);
    }
    
    /**
     * 构建每日内容统计（职位、申请、简历、通知等）
     */
    private List<AdminDashboardResponse.DailyStat> buildDailyContentStats(String contentType, int days) {
        List<AdminDashboardResponse.DailyStat> stats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            long count = 0;
            switch (contentType) {
                case "jobPosting":
                    count = jobPostingService.count(new LambdaQueryWrapper<JobPosting>()
                            .ge(JobPosting::getPublishedDate, startOfDay)
                            .le(JobPosting::getPublishedDate, endOfDay));
                    break;
                case "application":
                    count = jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                            .ge(JobApplication::getAppliedAt, startOfDay)
                            .le(JobApplication::getAppliedAt, endOfDay));
                    break;
                case "resume":
                    count = resumeService.count(new LambdaQueryWrapper<Resume>()
                            .ge(Resume::getCreatedAt, startOfDay)
                            .le(Resume::getCreatedAt, endOfDay));
                    break;
                case "notification":
                    count = notificationService.count(new LambdaQueryWrapper<SystemNotification>()
                            .ge(SystemNotification::getCreatedAt, startOfDay)
                            .le(SystemNotification::getCreatedAt, endOfDay));
                    break;
            }
            
            AdminDashboardResponse.DailyStat stat = new AdminDashboardResponse.DailyStat();
            stat.setDate(date.format(formatter));
            stat.setCount(count);
            stats.add(stat);
        }
        
        return stats;
    }
    
    /**
     * 构建每日搜索统计
     */
    private List<AdminDashboardResponse.DailyStat> buildDailySearchStats(int days) {
        List<AdminDashboardResponse.DailyStat> stats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            long count = searchHistoryService.count(new LambdaQueryWrapper<PublicSearchHistory>()
                    .ge(PublicSearchHistory::getCreatedAt, startOfDay)
                    .le(PublicSearchHistory::getCreatedAt, endOfDay));
            
            AdminDashboardResponse.DailyStat stat = new AdminDashboardResponse.DailyStat();
            stat.setDate(date.format(formatter));
            stat.setCount(count);
            stats.add(stat);
        }
        
        return stats;
    }
}

