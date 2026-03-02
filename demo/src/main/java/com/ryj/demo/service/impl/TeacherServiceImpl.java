package com.ryj.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ryj.demo.dto.TeacherDashboardResponse;
import com.ryj.demo.dto.TeacherProfileApprovalDetail;
import com.ryj.demo.dto.TeacherProfileResponse;
import com.ryj.demo.dto.TeacherProfileUpdateRequest;
import com.ryj.demo.entity.*;
import com.ryj.demo.mapper.TeacherMapper;
import com.ryj.demo.service.*;
import com.ryj.demo.service.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    private final SysUserService sysUserService;
    private final StudentProfileService studentProfileService;
    private final StudentProfileUpdateRequestService profileUpdateRequestService;
    private final TeacherGuidanceService guidanceService;
    private final JobApplicationService jobApplicationService;
    private final InterviewService interviewService;
    private final EmployerService employerService;
    private final JobPostingService jobPostingService;
    private final SystemNotificationService notificationService;

    public TeacherServiceImpl(
            SysUserService sysUserService,
            StudentProfileService studentProfileService,
            @Lazy StudentProfileUpdateRequestService profileUpdateRequestService,
            TeacherGuidanceService guidanceService,
            JobApplicationService jobApplicationService,
            InterviewService interviewService,
            EmployerService employerService,
            JobPostingService jobPostingService,
            SystemNotificationService notificationService) {
        this.sysUserService = sysUserService;
        this.studentProfileService = studentProfileService;
        this.profileUpdateRequestService = profileUpdateRequestService;
        this.guidanceService = guidanceService;
        this.jobApplicationService = jobApplicationService;
        this.interviewService = interviewService;
        this.employerService = employerService;
        this.jobPostingService = jobPostingService;
        this.notificationService = notificationService;
    }

    @Override
    public Teacher getByUserId(Long userId) {
        Teacher teacher = this.getOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (teacher != null) {
            return teacher;
        }

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("未找到关联的用户信息");
        }
        if (user.getRole() != SysUser.Role.TEACHER) {
            throw new IllegalArgumentException("当前账号不是教师角色");
        }

        Teacher newTeacher = new Teacher();
        newTeacher.setUserId(userId);
        newTeacher.setEmail(user.getEmail());
        newTeacher.setPhone(user.getPhone());
        this.save(newTeacher);
        return this.getOne(new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
    }

    @Override
    public TeacherProfileResponse getProfile(Long teacherId) {
        Teacher teacher = this.getById(teacherId);
        if (teacher == null) {
            throw new IllegalArgumentException("教师信息不存在");
        }
        SysUser teacherUser = sysUserService.getById(teacher.getUserId());
        return buildProfileResponse(teacher, teacherUser);
    }

    @Override
    public boolean updateProfile(Long teacherId, TeacherProfileUpdateRequest request) {
        Teacher teacher = this.getById(teacherId);
        if (teacher == null) {
            throw new IllegalArgumentException("教师信息不存在");
        }

        boolean teacherUpdated = false;
        if (request.getDepartment() != null) {
            teacher.setDepartment(blankToNull(request.getDepartment()));
            teacherUpdated = true;
        }
        if (request.getMajor() != null) {
            teacher.setMajor(blankToNull(request.getMajor()));
            teacherUpdated = true;
        }
        if (request.getEmail() != null) {
            teacher.setEmail(blankToNull(request.getEmail()));
            teacherUpdated = true;
        }
        if (request.getPhone() != null) {
            teacher.setPhone(blankToNull(request.getPhone()));
            teacherUpdated = true;
        }
        if (request.getFocus() != null) {
            teacher.setFocus(blankToNull(request.getFocus()));
            teacherUpdated = true;
        }
        if (request.getBiography() != null) {
            teacher.setBiography(blankToNull(request.getBiography()));
            teacherUpdated = true;
        }
        if (teacherUpdated) {
            this.updateById(teacher);
        }

        SysUser teacherUser = sysUserService.getById(teacher.getUserId());
        if (teacherUser != null) {
            boolean userUpdated = false;
            if (request.getName() != null && StringUtils.hasText(request.getName())) {
                teacherUser.setFullName(request.getName().trim());
                userUpdated = true;
            }
            if (request.getEmail() != null) {
                teacherUser.setEmail(blankToNull(request.getEmail()));
                userUpdated = true;
            }
            if (request.getPhone() != null) {
                teacherUser.setPhone(blankToNull(request.getPhone()));
                userUpdated = true;
            }
            if (userUpdated) {
                sysUserService.updateById(teacherUser);
            }
        }

        return true;
    }

    @Override
    public TeacherDashboardResponse getDashboardData(Long teacherId) {
        TeacherDashboardResponse response = new TeacherDashboardResponse();

        // 1. 获取教师基本信息
        Teacher teacher = this.getById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在");
        }

        SysUser teacherUser = sysUserService.getById(teacher.getUserId());

        response.setProfile(toDashboardProfile(buildProfileResponse(teacher, teacherUser)));

        // 2. 获取所有被指导的学生
        List<TeacherGuidance> allGuidances = guidanceService.list(
            new LambdaQueryWrapper<TeacherGuidance>().eq(TeacherGuidance::getTeacherId, teacherId)
        );
        
        Set<Long> guidedStudentIds = allGuidances.stream()
            .map(TeacherGuidance::getStudentId)
            .collect(Collectors.toSet());

        // 3. 获取待审核的档案更新请求（只看分配给当前老师的）
        List<StudentProfileUpdateRequest> pendingRequests = profileUpdateRequestService.list(
            new LambdaQueryWrapper<StudentProfileUpdateRequest>()
                .eq(StudentProfileUpdateRequest::getStatus, "PENDING")
                .eq(StudentProfileUpdateRequest::getHomeroomTeacherId, teacherId)
        );

        List<TeacherDashboardResponse.PendingApproval> pendingApprovals = new ArrayList<>();
        for (StudentProfileUpdateRequest request : pendingRequests) {
            SysUser student = sysUserService.getById(request.getStudentId());
            TeacherDashboardResponse.PendingApproval approval = new TeacherDashboardResponse.PendingApproval();
            approval.setRequestId(request.getId());
            approval.setStudentId(request.getStudentId());
            approval.setStudentName(student != null ? student.getFullName() : "未知学生");
            approval.setMajor(request.getMajor());
            approval.setGraduationYear(request.getGraduationYear());
            approval.setSubmittedAt(request.getCreatedAt());
            approval.setBiography(request.getBiography());
            pendingApprovals.add(approval);
        }
        response.setPendingApprovals(pendingApprovals);

        // 4. 获取所有被指导学生的详细信息
        List<TeacherDashboardResponse.GuidedStudent> guidedStudents = new ArrayList<>();
        for (Long studentId : guidedStudentIds) {
            SysUser student = sysUserService.getById(studentId);
            StudentProfile profile1 = studentProfileService.getById(studentId);
            
            // 获取该学生的待审核请求数
            long pendingCount = profileUpdateRequestService.count(
                new LambdaQueryWrapper<StudentProfileUpdateRequest>()
                    .eq(StudentProfileUpdateRequest::getStudentId, studentId)
                    .eq(StudentProfileUpdateRequest::getStatus, "PENDING")
            );
            
            // 获取该学生的活跃申请数
            long activeAppCount = jobApplicationService.count(
                new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getStudentId, studentId)
                    .in(JobApplication::getStatus, Arrays.asList(
                        JobApplication.Status.SUBMITTED,
                        JobApplication.Status.REVIEWING,
                        JobApplication.Status.INTERVIEW
                    ))
            );
            
            // 获取最新面试状态
            JobApplication latestApp = jobApplicationService.getOne(
                new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getStudentId, studentId)
                    .orderByDesc(JobApplication::getAppliedAt)
                    .last("LIMIT 1")
            );
            
            Interview latestInterview = null;
            if (latestApp != null) {
                latestInterview = interviewService.getOne(
                    new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getApplicationId, latestApp.getId())
                        .orderByDesc(Interview::getScheduledTime)
                        .last("LIMIT 1")
                );
            }
            
            // 获取最近指导记录
            TeacherGuidance latestGuidance = guidanceService.getOne(
                new LambdaQueryWrapper<TeacherGuidance>()
                    .eq(TeacherGuidance::getTeacherId, teacherId)
                    .eq(TeacherGuidance::getStudentId, studentId)
                    .orderByDesc(TeacherGuidance::getCreatedAt)
                    .last("LIMIT 1")
            );
            
            // 获取该学生申请的企业名称
            List<JobApplication> apps = jobApplicationService.list(
                new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getStudentId, studentId)
                    .orderByDesc(JobApplication::getAppliedAt)
                    .last("LIMIT 5")
            );
            
            List<String> employerNames = new ArrayList<>();
            for (JobApplication app : apps) {
                if (app.getJobId() != null) {
                    JobPosting job = jobPostingService.getById(app.getJobId());
                    if (job != null && job.getEmployerId() != null) {
                        Employer employer = employerService.getById(job.getEmployerId());
                        if (employer != null && !employerNames.contains(employer.getCompanyName())) {
                            employerNames.add(employer.getCompanyName());
                        }
                    }
                }
            }
            
            TeacherDashboardResponse.GuidedStudent guidedStudent = new TeacherDashboardResponse.GuidedStudent();
            guidedStudent.setStudentId(studentId);
            guidedStudent.setStudentName(student != null ? student.getFullName() : "未知");
            guidedStudent.setMajor(profile1 != null ? profile1.getMajor() : null);
            guidedStudent.setPendingRequestCount((int) pendingCount);
            guidedStudent.setActiveApplicationCount((int) activeAppCount);
            guidedStudent.setLatestInterviewStatus(latestInterview != null ? latestInterview.getStatus().toString() : null);
            guidedStudent.setLatestGuidanceAt(latestGuidance != null ? latestGuidance.getCreatedAt() : null);
            guidedStudent.setLatestGuidanceNote(latestGuidance != null ? latestGuidance.getNote() : null);
            guidedStudent.setEmployerNames(employerNames);
            
            guidedStudents.add(guidedStudent);
        }
        response.setGuidedStudents(guidedStudents);

        // 5. 获取校企协同信息
        Map<Long, TeacherDashboardResponse.EmployerCollaboration> employerMap = new HashMap<>();
        for (Long studentId : guidedStudentIds) {
            List<JobApplication> apps = jobApplicationService.list(
                new LambdaQueryWrapper<JobApplication>().eq(JobApplication::getStudentId, studentId)
            );
            
            for (JobApplication app : apps) {
                if (app.getJobId() == null) continue;
                JobPosting job = jobPostingService.getById(app.getJobId());
                if (job == null || job.getEmployerId() == null) continue;
                
                Employer employer = employerService.getById(job.getEmployerId());
                if (employer == null) continue;
                
                TeacherDashboardResponse.EmployerCollaboration collab = employerMap.computeIfAbsent(
                    employer.getId(),
                    id -> {
                        TeacherDashboardResponse.EmployerCollaboration c = new TeacherDashboardResponse.EmployerCollaboration();
                        c.setEmployerId(id);
                        c.setCompanyName(employer.getCompanyName());
                        c.setJobCount(0);
                        c.setStudentCount(0);
                        c.setStudentNames(new ArrayList<>());
                        return c;
                    }
                );
                
                SysUser student = sysUserService.getById(studentId);
                String studentName = student != null ? student.getFullName() : "未知";
                if (!collab.getStudentNames().contains(studentName)) {
                    collab.getStudentNames().add(studentName);
                    collab.setStudentCount(collab.getStudentCount() + 1);
                }
                
                if (collab.getLatestInteraction() == null || 
                    app.getAppliedAt().isAfter(collab.getLatestInteraction())) {
                    collab.setLatestInteraction(app.getAppliedAt());
                }
            }
        }
        
        // 计算每个企业的职位数
        for (TeacherDashboardResponse.EmployerCollaboration collab : employerMap.values()) {
            long jobCount = jobPostingService.count(
                new LambdaQueryWrapper<JobPosting>().eq(JobPosting::getEmployerId, collab.getEmployerId())
            );
            collab.setJobCount((int) jobCount);
        }
        
        response.setEmployerCollaborations(new ArrayList<>(employerMap.values()));

        // 6. 获取最近指导记录
        List<TeacherGuidance> recentGuidances = guidanceService.list(
            new LambdaQueryWrapper<TeacherGuidance>()
                .eq(TeacherGuidance::getTeacherId, teacherId)
                .orderByDesc(TeacherGuidance::getCreatedAt)
                .last("LIMIT 10")
        );
        
        List<TeacherDashboardResponse.GuidanceNote> guidanceNotes = new ArrayList<>();
        for (TeacherGuidance guidance : recentGuidances) {
            SysUser student = sysUserService.getById(guidance.getStudentId());
            TeacherDashboardResponse.GuidanceNote note = new TeacherDashboardResponse.GuidanceNote();
            note.setId(guidance.getId());
            note.setStudentId(guidance.getStudentId());
            note.setStudentName(student != null ? student.getFullName() : "未知");
            note.setNote(guidance.getNote());
            note.setCreatedAt(guidance.getCreatedAt());
            guidanceNotes.add(note);
        }
        response.setRecentGuidanceNotes(guidanceNotes);

        // 7. 获取学生就业动向
        List<TeacherDashboardResponse.StudentActivity> activities = new ArrayList<>();
        for (Long studentId : guidedStudentIds) {
            List<JobApplication> apps = jobApplicationService.list(
                new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getStudentId, studentId)
                    .orderByDesc(JobApplication::getAppliedAt)
                    .last("LIMIT 3")
            );
            
            for (JobApplication app : apps) {
                SysUser student = sysUserService.getById(studentId);
                JobPosting job = app.getJobId() != null ? jobPostingService.getById(app.getJobId()) : null;
                Employer employer = (job != null && job.getEmployerId() != null) 
                    ? employerService.getById(job.getEmployerId()) : null;
                
                TeacherDashboardResponse.StudentActivity activity = new TeacherDashboardResponse.StudentActivity();
                activity.setApplicationId(app.getId());
                activity.setStudentId(studentId);
                activity.setStudentName(student != null ? student.getFullName() : "未知");
                activity.setJobId(app.getJobId());
                activity.setJobTitle(job != null ? job.getTitle() : null);
                activity.setEmployerName(employer != null ? employer.getCompanyName() : null);
                activity.setStatus(app.getStatus());
                activity.setAppliedAt(app.getAppliedAt());
                activities.add(activity);
            }
        }
        
        activities.sort((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()));
        if (activities.size() > 15) {
            activities = activities.subList(0, 15);
        }
        response.setRecentStudentActivities(activities);

        // 8. 构建概览数据
        TeacherDashboardResponse.Overview overview = new TeacherDashboardResponse.Overview();
        overview.setTotalGuidedStudents(guidedStudentIds.size());
        overview.setPendingApprovalCount(pendingApprovals.size());
        
        // 统计活跃面试数
        int activeInterviewCount = 0;
        for (Long studentId : guidedStudentIds) {
            long count = interviewService.count(
                new LambdaQueryWrapper<Interview>()
                    .eq(Interview::getStatus, Interview.Status.SCHEDULED)
                    .in(Interview::getApplicationId, 
                        jobApplicationService.list(
                            new LambdaQueryWrapper<JobApplication>()
                                .eq(JobApplication::getStudentId, studentId)
                        ).stream().map(JobApplication::getId).collect(Collectors.toList())
                    )
            );
            activeInterviewCount += count;
        }
        overview.setActiveInterviewCount(activeInterviewCount);
        overview.setCollaborationCount(employerMap.size());
        response.setOverview(overview);

        return response;
    }

    @Override
    @Transactional
    public Boolean approveProfileUpdate(Long teacherId, Long requestId, String reviewComment) {
        StudentProfileUpdateRequest request = profileUpdateRequestService.getById(requestId);
        if (request == null) {
            throw new RuntimeException("申请记录不存在");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("该申请已被处理，当前状态：" + request.getStatus());
        }
        
        // 验证该申请是否分配给当前教师
        if (request.getHomeroomTeacherId() != null && !request.getHomeroomTeacherId().equals(teacherId)) {
            throw new RuntimeException("该申请未分配给您，无权审核");
        }
        
        Teacher teacher = this.getById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在，无法执行审核");
        }

        // 1. 更新学生档案信息（通过后立即生效）
        StudentProfile profile = studentProfileService.getById(request.getStudentId());
        if (profile == null) {
            profile = new StudentProfile();
            profile.setId(request.getStudentId());
        }
        
        // 应用所有档案更新字段
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getAge() != null) {
            profile.setAge(request.getAge());
        }
        if (request.getMajor() != null) {
            profile.setMajor(request.getMajor());
        }
        if (request.getBiography() != null) {
            profile.setBiography(request.getBiography());
        }
        if (request.getGraduationYear() != null) {
            profile.setGraduationYear(request.getGraduationYear());
        }
        
        // 保存更新后的学生档案
        boolean profileUpdated = studentProfileService.saveOrUpdate(profile);
        if (!profileUpdated) {
            throw new RuntimeException("更新学生档案失败");
        }
        
        // 2. 更新申请记录状态为"教师已审核通过"
        request.setStatus("APPROVED");
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewerId(teacher.getUserId());
        request.setReviewComment(reviewComment != null && !reviewComment.trim().isEmpty() 
            ? reviewComment.trim() 
            : "审核通过");
        
        boolean requestUpdated = profileUpdateRequestService.updateById(request);
        if (!requestUpdated) {
            throw new RuntimeException("更新申请状态失败");
        }

        // 通知学生：档案审核已通过
        notifyStudentProfileReview(request.getStudentId(), true, null);

        return true;
    }

    @Override
    @Transactional
    public Boolean rejectProfileUpdate(Long teacherId, Long requestId, String reviewComment) {
        StudentProfileUpdateRequest request = profileUpdateRequestService.getById(requestId);
        if (request == null) {
            throw new RuntimeException("申请记录不存在");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("该申请已被处理，当前状态：" + request.getStatus());
        }
        
        // 验证该申请是否分配给当前教师
        if (request.getHomeroomTeacherId() != null && !request.getHomeroomTeacherId().equals(teacherId)) {
            throw new RuntimeException("该申请未分配给您，无权审核");
        }
        
        Teacher teacher = this.getById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师信息不存在，无法执行审核");
        }

        // 验证必须填写退回原因
        if (reviewComment == null || reviewComment.trim().isEmpty()) {
            throw new RuntimeException("退回申请时必须填写原因");
        }
        
        // 更新申请状态为"已退回"，学生档案不更新
        request.setStatus("REJECTED");
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewerId(teacher.getUserId());
        request.setReviewComment(reviewComment.trim());

        boolean updated = profileUpdateRequestService.updateById(request);
        if (!updated) {
            throw new RuntimeException("更新申请状态失败");
        }

        // 通知学生：档案审核已退回
        notifyStudentProfileReview(request.getStudentId(), false, reviewComment.trim());

        return true;
    }

    /** 给学生发送档案审核结果通知 */
    private void notifyStudentProfileReview(Long studentUserId, boolean approved, String comment) {
        if (studentUserId == null) return;
        SystemNotification n = new SystemNotification();
        n.setUserId(studentUserId);
        n.setCategory(SystemNotification.Category.APPLICATION);
        n.setTitle(approved ? "档案审核已通过" : "档案审核已退回");
        n.setContent(approved ? "您的个人档案更新申请已通过教师审核。" : "您的档案更新申请已被退回，请查看教师意见后修改再提交。");
        if (comment != null && !comment.isEmpty()) {
            n.setContent(n.getContent() + " 教师意见：" + comment);
        }
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);
    }

    @Override
    public TeacherProfileApprovalDetail getProfileApprovalDetail(Long teacherId, Long requestId) {
        StudentProfileUpdateRequest request = profileUpdateRequestService.getById(requestId);
        if (request == null) {
            throw new RuntimeException("申请记录不存在");
        }

        if (request.getHomeroomTeacherId() != null && !request.getHomeroomTeacherId().equals(teacherId)) {
            throw new RuntimeException("该申请未分配给您，无权查看");
        }

        TeacherProfileApprovalDetail detail = new TeacherProfileApprovalDetail();
        detail.setRequestId(request.getId());
        detail.setStudentId(request.getStudentId());
        SysUser studentUser = sysUserService.getById(request.getStudentId());
        detail.setStudentName(studentUser != null ? studentUser.getFullName() : "未知学生");
        detail.setStatus(request.getStatus());
        detail.setSubmittedAt(request.getCreatedAt());
        detail.setReviewedAt(request.getReviewedAt());
        detail.setReviewerId(request.getReviewerId());
        detail.setReviewComment(request.getReviewComment());

        StudentProfile currentProfile = studentProfileService.getById(request.getStudentId());
        detail.setCurrentProfile(toProfileSnapshot(currentProfile));
        detail.setRequestedProfile(toProfileSnapshot(request));

        return detail;
    }

    private TeacherProfileApprovalDetail.ProfileSnapshot toProfileSnapshot(StudentProfile profile) {
        TeacherProfileApprovalDetail.ProfileSnapshot snapshot = new TeacherProfileApprovalDetail.ProfileSnapshot();
        if (profile == null) {
            return snapshot;
        }
        snapshot.setGender(profile.getGender());
        snapshot.setAge(profile.getAge());
        snapshot.setMajor(profile.getMajor());
        snapshot.setBiography(profile.getBiography());
        snapshot.setGraduationYear(profile.getGraduationYear());
        return snapshot;
    }

    private TeacherProfileApprovalDetail.ProfileSnapshot toProfileSnapshot(StudentProfileUpdateRequest request) {
        TeacherProfileApprovalDetail.ProfileSnapshot snapshot = new TeacherProfileApprovalDetail.ProfileSnapshot();
        if (request == null) {
            return snapshot;
        }
        snapshot.setGender(request.getGender());
        snapshot.setAge(request.getAge());
        snapshot.setMajor(request.getMajor());
        snapshot.setBiography(request.getBiography());
        snapshot.setGraduationYear(request.getGraduationYear());
        return snapshot;
    }

    @Override
    public SysUser getUserIfExists(Long userId) {
        try {
            if (userId == null) return null;
            return sysUserService.getById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private TeacherProfileResponse buildProfileResponse(Teacher teacher, SysUser teacherUser) {
        TeacherProfileResponse response = new TeacherProfileResponse();
        response.setTeacherId(teacher.getId());
        response.setUserId(teacher.getUserId());
        response.setName(teacherUser != null ? teacherUser.getFullName() : null);
        response.setDepartment(teacher.getDepartment());
        response.setMajor(teacher.getMajor());
        response.setEmail(coalesce(teacher.getEmail(), teacherUser != null ? teacherUser.getEmail() : null));
        response.setPhone(coalesce(teacher.getPhone(), teacherUser != null ? teacherUser.getPhone() : null));
        response.setBiography(teacher.getBiography());
        response.setFocus(teacher.getFocus());

        String initialsSource = response.getName();
        if (!StringUtils.hasText(initialsSource) && teacherUser != null) {
            initialsSource = teacherUser.getUsername();
        }
        if (StringUtils.hasText(initialsSource)) {
            response.setAvatarInitials(initialsSource.trim().substring(0, 1).toUpperCase());
        } else {
            response.setAvatarInitials("T");
        }
        return response;
    }

    private TeacherDashboardResponse.TeacherProfile toDashboardProfile(TeacherProfileResponse profile) {
        TeacherDashboardResponse.TeacherProfile result = new TeacherDashboardResponse.TeacherProfile();
        result.setTeacherId(profile.getTeacherId());
        result.setUserId(profile.getUserId());
        result.setName(StringUtils.hasText(profile.getName()) ? profile.getName() : "未知");
        result.setDepartment(profile.getDepartment());
        result.setMajor(profile.getMajor());
        result.setEmail(profile.getEmail());
        result.setPhone(profile.getPhone());
        result.setBiography(StringUtils.hasText(profile.getBiography())
            ? profile.getBiography()
            : "就业指导教师，致力于帮助学生实现职业发展目标");
        result.setFocus(StringUtils.hasText(profile.getFocus()) ? profile.getFocus() : "就业指导 | 职业规划");
        result.setAvatarInitials(StringUtils.hasText(profile.getAvatarInitials()) ? profile.getAvatarInitials() : "T");
        return result;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String coalesce(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return null;
    }
}
