package com.ryj.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ryj.demo.common.ApiResponse;
import com.ryj.demo.dto.JobApplicationRequest;
import com.ryj.demo.dto.StudentApplicationItem;
import com.ryj.demo.entity.Employer;
import com.ryj.demo.entity.JobApplication;
import com.ryj.demo.entity.JobPosting;
import com.ryj.demo.entity.SystemNotification;
import com.ryj.demo.service.EmployerService;
import com.ryj.demo.service.JobApplicationService;
import com.ryj.demo.service.JobPostingService;
import com.ryj.demo.service.SystemNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final JobPostingService jobPostingService;
    private final EmployerService employerService;
    private final SystemNotificationService notificationService;

    @PostMapping
    public ApiResponse<JobApplication> create(@Valid @RequestBody JobApplicationRequest request) {
        JobApplication application = new JobApplication();
        application.setJobId(request.getJobId());
        application.setStudentId(request.getStudentId());
        application.setResumeId(request.getResumeId());
        application.setStatus(request.getStatus() != null ? request.getStatus() : JobApplication.Status.SUBMITTED);
        application.setCoverLetter(request.getCoverLetter());
        jobApplicationService.save(application);
        // 学生提交申请后，通知岗位所属企业有新的职位申请
        if (application.getJobId() != null) {
            notifyEmployerNewApplication(application.getJobId());
        }
        return ApiResponse.success(application);
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> updateStatus(@PathVariable Long id, @RequestBody JobApplicationRequest request) {
        JobApplication application = jobApplicationService.getById(id);
        if (application == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (request.getStatus() != null) {
            application.setStatus(request.getStatus());
        }
        application.setCoverLetter(request.getCoverLetter());
        boolean updated = jobApplicationService.updateById(application);
        if (updated && request.getStatus() != null && application.getStudentId() != null) {
            notifyStudentApplicationStatus(application.getStudentId(), request.getStatus());
        }
        return ApiResponse.success(updated);
    }

    /**
     * 通知企业用户：其岗位收到新的学生申请
     */
    private void notifyEmployerNewApplication(Long jobId) {
        if (jobId == null) return;
        JobPosting job = jobPostingService.getById(jobId);
        if (job == null || job.getEmployerId() == null) {
            return;
        }
        Employer employer = employerService.getById(job.getEmployerId());
        if (employer == null || employer.getUserId() == null) {
            return;
        }

        SystemNotification n = new SystemNotification();
        n.setUserId(employer.getUserId());
        n.setCategory(SystemNotification.Category.APPLICATION);
        n.setTitle("收到新的职位申请");
        String title = job.getTitle() != null ? job.getTitle() : "职位";
        n.setContent("您的岗位「" + title + "」收到一份新的学生申请，请尽快前往系统查看并处理。");
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);
    }

    private void notifyStudentApplicationStatus(Long studentUserId, JobApplication.Status status) {
        if (studentUserId == null) return;
        String title;
        String content;
        switch (status) {
            case REVIEWING -> { title = "投递已进入审核"; content = "您投递的职位申请已进入企业审核，请留意后续通知。"; }
            case INTERVIEW -> { title = "面试邀请"; content = "企业已对您的投递发起面试安排，请及时查看并确认。"; }
            case OFFERED -> { title = "收到录用通知"; content = "恭喜！您已收到企业的录用通知，请及时查看并处理。"; }
            case REJECTED -> { title = "申请未通过"; content = "您投递的职位申请未通过，可继续投递其他岗位。"; }
            default -> { return; }
        }
        SystemNotification n = new SystemNotification();
        n.setUserId(studentUserId);
        n.setCategory(SystemNotification.Category.APPLICATION);
        n.setTitle(title);
        n.setContent(content);
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);
    }

    @GetMapping
    public ApiResponse<Page<JobApplication>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) Long jobId,
                                                  @RequestParam(required = false) Long studentId) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(JobApplication::getJobId, jobId);
        }
        if (studentId != null) {
            wrapper.eq(JobApplication::getStudentId, studentId);
        }
        Page<JobApplication> result = jobApplicationService.page(new Page<>(page, size), wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 学生端「我的申请」列表，带职位名称与公司名称，支持按状态筛选。
     */
    @GetMapping("/my")
    public ApiResponse<Page<StudentApplicationItem>> myApplications(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) JobApplication.Status status) {
        LambdaQueryWrapper<JobApplication> wrapper = new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getStudentId, studentId)
                .orderByDesc(JobApplication::getAppliedAt);
        if (status != null) {
            wrapper.eq(JobApplication::getStatus, status);
        }
        Page<JobApplication> rawPage = jobApplicationService.page(new Page<>(page, size), wrapper);
        List<StudentApplicationItem> items = buildStudentApplicationItems(rawPage.getRecords());
        Page<StudentApplicationItem> result = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        result.setRecords(items);
        return ApiResponse.success(result);
    }

    private List<StudentApplicationItem> buildStudentApplicationItems(List<JobApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, JobPosting> jobCache = new HashMap<>();
        Map<Long, Employer> employerCache = new HashMap<>();
        List<StudentApplicationItem> list = new ArrayList<>(applications.size());
        for (JobApplication app : applications) {
            StudentApplicationItem item = new StudentApplicationItem();
            item.setId(app.getId());
            item.setJobId(app.getJobId());
            item.setStudentId(app.getStudentId());
            item.setResumeId(app.getResumeId());
            item.setStatus(app.getStatus());
            item.setCoverLetter(app.getCoverLetter());
            item.setAppliedAt(app.getAppliedAt());
            if (app.getJobId() != null) {
                JobPosting job = jobCache.computeIfAbsent(app.getJobId(), jobPostingService::getById);
                if (job != null) {
                    item.setJobTitle(job.getTitle());
                    item.setLocation(job.getLocation());
                    item.setSalaryRange(job.getSalaryRange());
                    item.setWorkType(job.getWorkType() != null ? job.getWorkType().name() : null);
                    if (job.getEmployerId() != null) {
                        Employer employer = employerCache.computeIfAbsent(job.getEmployerId(), employerService::getById);
                        if (employer != null) {
                            item.setCompanyName(employer.getCompanyName());
                        }
                    }
                }
            }
            list.add(item);
        }
        return list;
    }

    @GetMapping("/{id}")
    public ApiResponse<JobApplication> detail(@PathVariable Long id) {
        return ApiResponse.success(jobApplicationService.getById(id));
    }
}
