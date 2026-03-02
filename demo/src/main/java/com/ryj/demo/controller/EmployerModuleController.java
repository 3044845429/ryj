package com.ryj.demo.controller;

import com.ryj.demo.common.ApiResponse;
import com.ryj.demo.dto.EmployerApplicationStatusRequest;
import com.ryj.demo.dto.EmployerDashboardResponse;
import com.ryj.demo.dto.EmployerDashboardResponse.ApplicationOverview;
import com.ryj.demo.dto.EmployerDashboardResponse.CompanyProfile;
import com.ryj.demo.dto.EmployerDashboardResponse.Header;
import com.ryj.demo.dto.EmployerDashboardResponse.InterviewOverview;
import com.ryj.demo.dto.EmployerDashboardResponse.JobOverview;
import com.ryj.demo.dto.EmployerDashboardResponse.ModuleInfo;
import com.ryj.demo.dto.EmployerInterviewRequest;
import com.ryj.demo.dto.EmployerJobDetailResponse;
import com.ryj.demo.dto.EmployerJobRequest;
import com.ryj.demo.dto.EmployerProfileRequest;
import com.ryj.demo.dto.EmployerTalentResponse;
import com.ryj.demo.dto.EmployerTalentResponse.Summary;
import com.ryj.demo.dto.EmployerTalentResponse.TalentCandidate;
import com.ryj.demo.entity.Employer;
import com.ryj.demo.entity.EmploymentIntention;
import com.ryj.demo.entity.EmploymentIntentionCity;
import com.ryj.demo.entity.Interview;
import com.ryj.demo.entity.JobApplication;
import com.ryj.demo.entity.JobPosting;
import com.ryj.demo.entity.JobRequirement;
import com.ryj.demo.entity.ProfileUpdateRequest;
import com.ryj.demo.entity.Resume;
import com.ryj.demo.entity.SysUser;
import com.ryj.demo.entity.SystemNotification;
import com.ryj.demo.entity.StudentProfile;
import com.ryj.demo.service.EmployerService;
import com.ryj.demo.service.EmploymentIntentionCityService;
import com.ryj.demo.service.EmploymentIntentionService;
import com.ryj.demo.service.InterviewService;
import com.ryj.demo.service.JobApplicationService;
import com.ryj.demo.service.JobPostingService;
import com.ryj.demo.service.JobRequirementService;
import com.ryj.demo.service.ProfileUpdateRequestService;
import com.ryj.demo.service.ResumeService;
import com.ryj.demo.service.StudentProfileService;
import com.ryj.demo.service.SysUserService;
import com.ryj.demo.service.SystemNotificationService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer-module")
@RequiredArgsConstructor
public class EmployerModuleController {

    private final SysUserService sysUserService;
    private final EmployerService employerService;
    private final JobPostingService jobPostingService;
    private final JobRequirementService jobRequirementService;
    private final JobApplicationService jobApplicationService;
    private final ResumeService resumeService;
    private final InterviewService interviewService;
    private final StudentProfileService studentProfileService;
    private final EmploymentIntentionService employmentIntentionService;
    private final EmploymentIntentionCityService employmentIntentionCityService;
    private final SystemNotificationService notificationService;
    private final ProfileUpdateRequestService profileUpdateRequestService;

    @GetMapping("/overview")
    public ApiResponse<EmployerDashboardResponse> overview(@RequestParam Long userId) {
        SysUser employerUser = requireEmployerUser(userId);
        Optional<Employer> employerOpt = employerService.findByUserId(userId);

        EmployerDashboardResponse response = new EmployerDashboardResponse();
        response.setModules(buildModules());

        if (employerOpt.isEmpty()) {
            Header header = new Header();
            header.setCompanyName(Optional.ofNullable(employerUser.getFullName()).orElse(employerUser.getUsername()));
            header.setTotalJobCount(0);
            header.setOpenJobCount(0);
            header.setActiveApplicationCount(0);
            header.setScheduledInterviewCount(0);
            header.setTalentPoolSize(0);
            response.setHeader(header);
            response.setCompanyProfile(null);
            response.setJobs(Collections.emptyList());
            response.setApplications(Collections.emptyList());
            response.setInterviews(Collections.emptyList());
            return ApiResponse.success(response);
        }

        Employer employer = employerOpt.get();
        List<JobPosting> postings = jobPostingService.lambdaQuery()
                .eq(JobPosting::getEmployerId, employer.getId())
                .orderByDesc(JobPosting::getPublishedDate)
                .list();

        List<Long> jobIds = postings.stream()
                .map(JobPosting::getId)
                .collect(Collectors.toList());

        List<JobApplication> applications = jobIds.isEmpty() ? List.of() : jobApplicationService.lambdaQuery()
                .in(JobApplication::getJobId, jobIds)
                .orderByDesc(JobApplication::getAppliedAt)
                .list();

        List<Interview> interviews = jobIds.isEmpty() ? List.of() : interviewService.lambdaQuery()
                .in(Interview::getJobId, jobIds)
                .orderByDesc(Interview::getScheduledTime)
                .list();

        Map<Long, JobPosting> jobMap = postings.stream()
                .collect(Collectors.toMap(JobPosting::getId, Function.identity()));

        Map<Long, JobApplication> applicationMap = new HashMap<>();
        for (JobApplication application : applications) {
            applicationMap.put(application.getId(), application);
        }

        Set<Long> additionalApplicationIds = interviews.stream()
                .map(Interview::getApplicationId)
                .filter(Objects::nonNull)
                .filter(id -> !applicationMap.containsKey(id))
                .collect(Collectors.toSet());
        if (!additionalApplicationIds.isEmpty()) {
            List<JobApplication> extraApplications = jobApplicationService.listByIds(additionalApplicationIds);
            for (JobApplication extra : extraApplications) {
                applicationMap.put(extra.getId(), extra);
            }
            applications = mergeApplications(applications, extraApplications);
        }

        Set<Long> resumeIds = applicationMap.values().stream()
                .map(JobApplication::getResumeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Resume> resumes = resumeIds.isEmpty() ? List.of() : resumeService.listByIds(resumeIds);
        Map<Long, Resume> resumeMap = resumes.stream()
                .collect(Collectors.toMap(Resume::getId, Function.identity()));

        Set<Long> studentIds = new HashSet<>();
        for (JobApplication application : applicationMap.values()) {
            if (application.getStudentId() != null) {
                studentIds.add(application.getStudentId());
            }
        }
        for (Resume resume : resumes) {
            if (resume.getStudentId() != null) {
                studentIds.add(resume.getStudentId());
            }
        }

        Map<Long, SysUser> studentMap = studentIds.isEmpty() ? Collections.emptyMap() : sysUserService.listByIds(studentIds)
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        Map<Long, Long> applicationCountMap = applications.stream()
                .collect(Collectors.groupingBy(JobApplication::getJobId, Collectors.counting()));

        long openJobCount = postings.stream()
                .filter(job -> job.getStatus() == JobPosting.Status.OPEN)
                .count();
        long activeApplicationCount = applications.stream()
                .filter(app -> app.getStatus() != JobApplication.Status.REJECTED
                        && app.getStatus() != JobApplication.Status.OFFERED)
                .count();
        long scheduledInterviewCount = interviews.stream()
                .filter(interview -> interview.getStatus() == Interview.Status.SCHEDULED)
                .count();

        Header header = new Header();
        header.setCompanyName(employer.getCompanyName());
        header.setTotalJobCount(postings.size());
        header.setOpenJobCount((int) openJobCount);
        header.setActiveApplicationCount((int) activeApplicationCount);
        header.setScheduledInterviewCount((int) scheduledInterviewCount);
        header.setTalentPoolSize(studentIds.size());
        response.setHeader(header);

        response.setCompanyProfile(toCompanyProfile(employer));

        List<JobOverview> jobOverviews = postings.stream()
                .map(job -> toJobOverview(job, applicationCountMap.getOrDefault(job.getId(), 0L)))
                .collect(Collectors.toList());
        response.setJobs(jobOverviews);

        List<ApplicationOverview> applicationOverviews = applications.stream()
                .sorted(Comparator.comparing(JobApplication::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(app -> toApplicationOverview(app, jobMap, studentMap))
                .collect(Collectors.toList());
        response.setApplications(applicationOverviews);

        List<InterviewOverview> interviewOverviews = interviews.stream()
                .sorted(Comparator.comparing(Interview::getScheduledTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(interview -> toInterviewOverview(interview, jobMap, applicationMap, resumeMap, studentMap))
                .collect(Collectors.toList());
        response.setInterviews(interviewOverviews);

        return ApiResponse.success(response);
    }

    @GetMapping("/profile")
    public ApiResponse<Employer> profile(@RequestParam Long userId) {
        requireEmployerUser(userId);
        return ApiResponse.success(employerService.findByUserId(userId).orElse(null));
    }

    @PostMapping("/profile")
    public ApiResponse<Employer> saveProfile(@RequestParam Long userId,
                                             @Valid @RequestBody EmployerProfileRequest request) {
        SysUser user = requireEmployerUser(userId);

        // 检查是否已有待审核记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProfileUpdateRequest> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProfileUpdateRequest>()
                        .eq(ProfileUpdateRequest::getUserId, user.getId())
                        .eq(ProfileUpdateRequest::getRole, "EMPLOYER")
                        .eq(ProfileUpdateRequest::getStatus, "PENDING");
        ProfileUpdateRequest existing = profileUpdateRequestService.getOne(wrapper);
        if (existing != null) {
            return ApiResponse.failure(400, "已有待审核的企业资料更新申请，请等待管理员审核后再提交新的修改");
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String payload = mapper.writeValueAsString(request);

            ProfileUpdateRequest record = new ProfileUpdateRequest();
            record.setUserId(user.getId());
            record.setRole("EMPLOYER");
            record.setPayload(payload);
            record.setStatus("PENDING");
            profileUpdateRequestService.save(record);

            // 给所有管理员发送系统通知
            notifyAdminsForProfileUpdate(user, "企业");

            // 返回当前已生效的企业资料（未修改）
            Employer employer = employerService.findByUserId(userId).orElse(null);
            return ApiResponse.success("资料更新申请已提交，等待管理员审核", employer);
        } catch (Exception e) {
            return ApiResponse.failure(500, "保存资料更新申请失败：" + e.getMessage(), null);
        }
    }

    @GetMapping("/jobs")
    public ApiResponse<List<EmployerJobDetailResponse>> jobs(@RequestParam Long userId) {
        Employer employer = requireEmployerProfile(userId);
        List<JobPosting> postings = jobPostingService.lambdaQuery()
                .eq(JobPosting::getEmployerId, employer.getId())
                .orderByDesc(JobPosting::getPublishedDate)
                .list();
        List<Long> jobIds = postings.stream().map(JobPosting::getId).collect(Collectors.toList());
        Map<Long, Long> applicationCountMap = jobIds.isEmpty() ? Collections.emptyMap()
                : jobApplicationService.lambdaQuery()
                .in(JobApplication::getJobId, jobIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(JobApplication::getJobId, Collectors.counting()));
        List<EmployerJobDetailResponse> results = new ArrayList<>();
        for (JobPosting posting : postings) {
            List<String> requirements = jobRequirementService.findByJobId(posting.getId()).stream()
                    .map(JobRequirement::getRequirement)
                    .collect(Collectors.toList());
            long count = applicationCountMap.getOrDefault(posting.getId(), 0L);
            results.add(toJobDetail(posting, requirements, count));
        }
        return ApiResponse.success(results);
    }

    @PostMapping("/jobs")
    public ApiResponse<EmployerJobDetailResponse> createJob(@RequestParam Long userId,
                                                            @Valid @RequestBody EmployerJobRequest request) {
        Employer employer = requireEmployerProfile(userId);
        JobPosting posting = new JobPosting();
        posting.setEmployerId(employer.getId());
        applyJobRequest(posting, request);
        posting.setPublishedDate(LocalDateTime.now());
        if (posting.getStatus() == null) {
            posting.setStatus(JobPosting.Status.OPEN);
        }
        jobPostingService.save(posting);
        List<String> requirements = normalizeRequirements(request.getRequirements());
        jobRequirementService.replaceRequirements(posting.getId(), requirements);
        return ApiResponse.success(toJobDetail(posting, requirements, 0));
    }

    @PutMapping("/jobs/{id}")
    public ApiResponse<EmployerJobDetailResponse> updateJob(@RequestParam Long userId,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody EmployerJobRequest request) {
        Employer employer = requireEmployerProfile(userId);
        JobPosting posting = jobPostingService.getById(id);
        if (posting == null || !Objects.equals(posting.getEmployerId(), employer.getId())) {
            throw new IllegalArgumentException("未找到所属企业的岗位信息");
        }
        applyJobRequest(posting, request);
        if (posting.getStatus() == null) {
            posting.setStatus(JobPosting.Status.OPEN);
        }
        jobPostingService.updateById(posting);
        List<String> requirements = normalizeRequirements(request.getRequirements());
        jobRequirementService.replaceRequirements(posting.getId(), requirements);
        long applicationCount = jobApplicationService.lambdaQuery()
                .eq(JobApplication::getJobId, posting.getId())
                .count();
        return ApiResponse.success(toJobDetail(posting, requirements, applicationCount));
    }

    @DeleteMapping("/jobs/{id}")
    public ApiResponse<Boolean> deleteJob(@RequestParam Long userId, @PathVariable Long id) {
        Employer employer = requireEmployerProfile(userId);
        JobPosting posting = jobPostingService.getById(id);
        if (posting == null || !Objects.equals(posting.getEmployerId(), employer.getId())) {
            throw new IllegalArgumentException("未找到所属企业的岗位信息");
        }
        jobRequirementService.replaceRequirements(id, null);
        boolean removed = jobPostingService.removeById(id);
        return ApiResponse.success(removed);
    }

    @GetMapping("/applications")
    public ApiResponse<List<ApplicationOverview>> applications(@RequestParam Long userId,
                                                               @RequestParam(required = false) JobApplication.Status status) {
        Employer employer = requireEmployerProfile(userId);
        List<JobPosting> postings = jobPostingService.lambdaQuery()
                .eq(JobPosting::getEmployerId, employer.getId())
                .list();
        List<Long> jobIds = postings.stream().map(JobPosting::getId).collect(Collectors.toList());
        if (jobIds.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }
        List<JobApplication> applications = jobApplicationService.lambdaQuery()
                .in(JobApplication::getJobId, jobIds)
                .orderByDesc(JobApplication::getAppliedAt)
                .list();
        if (status != null) {
            applications = applications.stream()
                    .filter(app -> app.getStatus() == status)
                    .collect(Collectors.toList());
        }
        Map<Long, JobPosting> jobMap = postings.stream()
                .collect(Collectors.toMap(JobPosting::getId, Function.identity()));
        Set<Long> studentIds = applications.stream()
                .map(JobApplication::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> studentMap = studentIds.isEmpty() ? Collections.emptyMap()
                : sysUserService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<ApplicationOverview> results = applications.stream()
                .map(app -> toApplicationOverview(app, jobMap, studentMap))
                .collect(Collectors.toList());
        return ApiResponse.success(results);
    }

    @GetMapping("/talent")
    public ApiResponse<EmployerTalentResponse> talent(@RequestParam Long userId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) JobApplication.Status status,
                                                      @RequestParam(required = false) Interview.Status interviewStatus) {
        Employer employer = requireEmployerProfile(userId);
        List<JobPosting> postings = jobPostingService.lambdaQuery()
                .eq(JobPosting::getEmployerId, employer.getId())
                .list();
        if (postings.isEmpty()) {
            return ApiResponse.success(emptyTalentResponse());
        }

        List<Long> jobIds = postings.stream()
                .map(JobPosting::getId)
                .collect(Collectors.toList());
        List<JobApplication> applications = jobApplicationService.lambdaQuery()
                .in(JobApplication::getJobId, jobIds)
                .orderByDesc(JobApplication::getAppliedAt)
                .list();
        if (applications.isEmpty()) {
            return ApiResponse.success(emptyTalentResponse());
        }

        Map<Long, JobPosting> jobMap = postings.stream()
                .collect(Collectors.toMap(JobPosting::getId, Function.identity()));

        Map<Long, JobApplication> applicationMap = applications.stream()
                .collect(Collectors.toMap(JobApplication::getId, Function.identity()));

        Map<Long, List<JobApplication>> applicationsByStudent = new HashMap<>();
        for (JobApplication application : applications) {
            if (application.getStudentId() == null) {
                continue;
            }
            applicationsByStudent
                    .computeIfAbsent(application.getStudentId(), key -> new ArrayList<>())
                    .add(application);
        }

        if (applicationsByStudent.isEmpty()) {
            return ApiResponse.success(emptyTalentResponse());
        }

        List<Long> studentIds = new ArrayList<>(applicationsByStudent.keySet());
        Map<Long, SysUser> studentMap = sysUserService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, StudentProfile> profileMap = studentProfileService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(StudentProfile::getId, Function.identity()));

        List<EmploymentIntention> intentions = employmentIntentionService.lambdaQuery()
                .in(EmploymentIntention::getStudentId, studentIds)
                .list();
        Map<Long, EmploymentIntention> intentionMap = intentions.stream()
                .collect(Collectors.toMap(EmploymentIntention::getStudentId, Function.identity()));
        Map<Long, List<String>> intentionCities = new HashMap<>();
        for (EmploymentIntention intention : intentions) {
            List<String> cities = employmentIntentionCityService.findByIntentionId(intention.getId()).stream()
                    .map(EmploymentIntentionCity::getCity)
                    .filter(Objects::nonNull)
                    .filter(city -> !city.isBlank())
                    .collect(Collectors.toList());
            intentionCities.put(intention.getStudentId(), cities);
        }

        List<Interview> interviews = interviewService.lambdaQuery()
                .in(Interview::getJobId, jobIds)
                .list();
        Map<Long, List<Interview>> interviewsByStudent = new HashMap<>();
        for (Interview interview : interviews) {
            JobApplication application = interview.getApplicationId() == null
                    ? null
                    : applicationMap.get(interview.getApplicationId());
            if (application == null || application.getStudentId() == null) {
                continue;
            }
            interviewsByStudent
                    .computeIfAbsent(application.getStudentId(), key -> new ArrayList<>())
                    .add(interview);
        }

        List<TalentCandidate> candidates = new ArrayList<>();
        for (Map.Entry<Long, List<JobApplication>> entry : applicationsByStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<JobApplication> studentApplications = entry.getValue();
            studentApplications.sort(Comparator.comparing(JobApplication::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())));
            JobApplication latest = studentApplications.get(0);

            TalentCandidate candidate = new TalentCandidate();
            candidate.setStudentId(studentId);
            SysUser student = studentMap.get(studentId);
            if (student != null) {
                candidate.setCandidateName(Optional.ofNullable(student.getFullName()).orElse(student.getUsername()));
                candidate.setEmail(student.getEmail());
                candidate.setPhone(student.getPhone());
            } else {
                candidate.setCandidateName("候选人");
            }

            StudentProfile profile = profileMap.get(studentId);
            if (profile != null) {
                candidate.setMajor(profile.getMajor());
                candidate.setGraduationYear(profile.getGraduationYear());
            }

            EmploymentIntention intention = intentionMap.get(studentId);
            if (intention != null) {
                candidate.setExpectedPosition(intention.getExpectedPosition());
                candidate.setExpectedWorkType(intention.getWorkType());
                List<String> cities = intentionCities.getOrDefault(studentId, Collections.emptyList());
                candidate.setIntentionCities(cities.isEmpty() ? Collections.emptyList() : new ArrayList<>(cities));
            }

            candidate.setLatestStatus(latest.getStatus());
            candidate.setLastAppliedAt(latest.getAppliedAt());
            candidate.setLatestResumeId(latest.getResumeId());
            JobPosting job = jobMap.get(latest.getJobId());
            candidate.setLatestJobTitle(job != null ? job.getTitle() : "岗位已删除");
            candidate.setApplicationCount(studentApplications.size());

            List<Interview> studentInterviews = interviewsByStudent.getOrDefault(studentId, Collections.emptyList());
            if (!studentInterviews.isEmpty()) {
                studentInterviews.sort(Comparator.comparing(Interview::getScheduledTime, Comparator.nullsLast(Comparator.reverseOrder())));
                candidate.setLatestInterviewStatus(studentInterviews.get(0).getStatus());
            }
            candidate.setInterviewCount(studentInterviews.size());

            candidates.add(candidate);
        }

        int totalCandidates = candidates.size();
        String keywordFilter = keyword == null ? null : keyword.trim().toLowerCase();
        List<TalentCandidate> filteredCandidates = candidates.stream()
                .filter(candidate -> matchesKeyword(candidate, keywordFilter))
                .filter(candidate -> status == null || candidate.getLatestStatus() == status)
                .filter(candidate -> interviewStatus == null || candidate.getLatestInterviewStatus() == interviewStatus)
                .sorted(Comparator.comparing(TalentCandidate::getLastAppliedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(candidate -> Optional.ofNullable(candidate.getCandidateName()).orElse("")))
                .collect(Collectors.toList());

        Summary summary = new Summary();
        summary.setTotalCandidates(totalCandidates);
        summary.setFilteredCandidates(filteredCandidates.size());
        summary.setPendingReviewCount((int) filteredCandidates.stream()
                .filter(candidate -> candidate.getLatestStatus() == JobApplication.Status.SUBMITTED
                        || candidate.getLatestStatus() == JobApplication.Status.REVIEWING)
                .count());
        summary.setInterviewingCount((int) filteredCandidates.stream()
                .filter(candidate -> candidate.getLatestInterviewStatus() == Interview.Status.SCHEDULED)
                .count());
        summary.setOfferCount((int) filteredCandidates.stream()
                .filter(candidate -> candidate.getLatestStatus() == JobApplication.Status.OFFERED)
                .count());

        EmployerTalentResponse response = new EmployerTalentResponse();
        response.setSummary(summary);
        response.setCandidates(filteredCandidates);
        return ApiResponse.success(response);
    }

    @PutMapping("/applications/{id}/status")
    public ApiResponse<Boolean> updateApplicationStatus(@RequestParam Long userId,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody EmployerApplicationStatusRequest request) {
        Employer employer = requireEmployerProfile(userId);
        JobApplication application = jobApplicationService.getById(id);
        if (application == null) {
            throw new IllegalArgumentException("申请记录不存在");
        }
        JobPosting posting = jobPostingService.getById(application.getJobId());
        if (posting == null || !Objects.equals(posting.getEmployerId(), employer.getId())) {
            throw new IllegalArgumentException("无权更新其他企业的申请");
        }
        application.setStatus(request.getStatus());
        boolean updated = jobApplicationService.updateById(application);
        if (updated && application.getStudentId() != null) {
            notifyStudentApplicationStatus(application.getStudentId(), request.getStatus());
        }
        return ApiResponse.success(updated);
    }

    @GetMapping("/interviews")
    public ApiResponse<List<InterviewOverview>> interviews(@RequestParam Long userId) {
        Employer employer = requireEmployerProfile(userId);
        List<JobPosting> postings = jobPostingService.lambdaQuery()
                .eq(JobPosting::getEmployerId, employer.getId())
                .list();
        List<Long> jobIds = postings.stream().map(JobPosting::getId).collect(Collectors.toList());
        if (jobIds.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }
        List<Interview> interviews = interviewService.lambdaQuery()
                .in(Interview::getJobId, jobIds)
                .orderByDesc(Interview::getScheduledTime)
                .list();
        Map<Long, JobPosting> jobMap = postings.stream()
                .collect(Collectors.toMap(JobPosting::getId, Function.identity()));
        Map<Long, JobApplication> applicationMap = jobApplicationService.lambdaQuery()
                .in(JobApplication::getJobId, jobIds)
                .list()
                .stream()
                .collect(Collectors.toMap(JobApplication::getId, Function.identity()));
        Set<Long> resumeIds = applicationMap.values().stream()
                .map(JobApplication::getResumeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Resume> resumeMap = resumeIds.isEmpty() ? Collections.emptyMap()
                : resumeService.listByIds(resumeIds).stream()
                .collect(Collectors.toMap(Resume::getId, Function.identity()));
        Set<Long> studentIds = applicationMap.values().stream()
                .map(JobApplication::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> studentMap = studentIds.isEmpty() ? Collections.emptyMap()
                : sysUserService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<InterviewOverview> results = interviews.stream()
                .map(interview -> toInterviewOverview(interview, jobMap, applicationMap, resumeMap, studentMap))
                .collect(Collectors.toList());
        return ApiResponse.success(results);
    }

    @PostMapping("/interviews")
    public ApiResponse<InterviewOverview> createInterview(@RequestParam Long userId,
                                                          @Valid @RequestBody EmployerInterviewRequest request) {
        Employer employer = requireEmployerProfile(userId);
        JobPosting posting = jobPostingService.getById(request.getJobId());
        if (posting == null || !Objects.equals(posting.getEmployerId(), employer.getId())) {
            throw new IllegalArgumentException("未找到所属企业的岗位信息");
        }
        JobApplication application = jobApplicationService.getById(request.getApplicationId());
        if (application == null || !Objects.equals(application.getJobId(), posting.getId())) {
            throw new IllegalArgumentException("申请与岗位信息不匹配");
        }
        Interview interview = new Interview();
        applyInterview(interview, request);
        interviewService.save(interview);
        notifyStudentInterview(application.getStudentId(), true, null);
        return ApiResponse.success(buildInterviewResponse(interview, posting, application));
    }

    @PutMapping("/interviews/{id}")
    public ApiResponse<InterviewOverview> updateInterview(@RequestParam Long userId,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody EmployerInterviewRequest request) {
        Employer employer = requireEmployerProfile(userId);
        Interview interview = interviewService.getById(id);
        if (interview == null) {
            throw new IllegalArgumentException("面试记录不存在");
        }
        JobPosting posting = jobPostingService.getById(interview.getJobId());
        if (posting == null || !Objects.equals(posting.getEmployerId(), employer.getId())) {
            throw new IllegalArgumentException("无权更新其他企业的面试安排");
        }
        JobApplication application = jobApplicationService.getById(interview.getApplicationId());
        if (application == null) {
            throw new IllegalArgumentException("关联申请不存在");
        }
        applyInterview(interview, request);
        interviewService.updateById(interview);
        if (application.getStudentId() != null) {
            notifyStudentInterview(application.getStudentId(), false, interview);
        }
        return ApiResponse.success(buildInterviewResponse(interview, posting, application));
    }

    @DeleteMapping("/interviews/{id}")
    public ApiResponse<Boolean> deleteInterview(@RequestParam Long userId, @PathVariable Long id) {
        Employer employer = requireEmployerProfile(userId);
        Interview interview = interviewService.getById(id);
        if (interview == null) {
            return ApiResponse.success(false);
        }
        JobPosting posting = jobPostingService.getById(interview.getJobId());
        if (posting == null || !Objects.equals(posting.getEmployerId(), employer.getId())) {
            throw new IllegalArgumentException("无权删除其他企业的面试安排");
        }
        boolean removed = interviewService.removeById(id);
        return ApiResponse.success(removed);
    }

    private EmployerTalentResponse emptyTalentResponse() {
        EmployerTalentResponse response = new EmployerTalentResponse();
        Summary summary = new Summary();
        summary.setTotalCandidates(0);
        summary.setFilteredCandidates(0);
        summary.setPendingReviewCount(0);
        summary.setInterviewingCount(0);
        summary.setOfferCount(0);
        response.setSummary(summary);
        response.setCandidates(Collections.emptyList());
        return response;
    }

    private boolean matchesKeyword(TalentCandidate candidate, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        String name = candidate.getCandidateName();
        if (name != null && name.toLowerCase().contains(keyword)) {
            return true;
        }
        String major = candidate.getMajor();
        if (major != null && major.toLowerCase().contains(keyword)) {
            return true;
        }
        String position = candidate.getExpectedPosition();
        if (position != null && position.toLowerCase().contains(keyword)) {
            return true;
        }
        String jobTitle = candidate.getLatestJobTitle();
        if (jobTitle != null && jobTitle.toLowerCase().contains(keyword)) {
            return true;
        }
        if (candidate.getIntentionCities() != null) {
            for (String city : candidate.getIntentionCities()) {
                if (city != null && city.toLowerCase().contains(keyword)) {
                    return true;
                }
            }
        }
        String email = candidate.getEmail();
        if (email != null && email.toLowerCase().contains(keyword)) {
            return true;
        }
        String phone = candidate.getPhone();
        return phone != null && phone.toLowerCase().contains(keyword);
    }

    private SysUser requireEmployerUser(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getRole() != SysUser.Role.EMPLOYER) {
            throw new IllegalArgumentException("仅企业用户可以访问该模块");
        }
        return user;
    }

    private Employer requireEmployerProfile(Long userId) {
        return employerService.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("请先完善企业信息"));
    }

    private CompanyProfile toCompanyProfile(Employer employer) {
        CompanyProfile profile = new CompanyProfile();
        profile.setId(employer.getId());
        profile.setCompanyName(employer.getCompanyName());
        profile.setContactPerson(employer.getContactPerson());
        profile.setContactEmail(employer.getContactEmail());
        profile.setContactPhone(employer.getContactPhone());
        profile.setDescription(employer.getDescription());
        profile.setWebsite(employer.getWebsite());
        return profile;
    }

    private JobOverview toJobOverview(JobPosting job, long applicationCount) {
        JobOverview overview = new JobOverview();
        overview.setId(job.getId());
        overview.setTitle(job.getTitle());
        overview.setStatus(job.getStatus());
        overview.setWorkType(job.getWorkType());
        overview.setLocation(job.getLocation());
        overview.setSalaryRange(job.getSalaryRange());
        overview.setPublishedDate(job.getPublishedDate());
        overview.setClosingDate(job.getClosingDate());
        overview.setApplicationCount(applicationCount);
        return overview;
    }

    private ApplicationOverview toApplicationOverview(JobApplication application,
                                                      Map<Long, JobPosting> jobMap,
                                                      Map<Long, SysUser> studentMap) {
        ApplicationOverview overview = new ApplicationOverview();
        overview.setId(application.getId());
        overview.setJobId(application.getJobId());
        JobPosting job = jobMap.get(application.getJobId());
        overview.setJobTitle(job != null ? job.getTitle() : "岗位已删除");
        overview.setStudentId(application.getStudentId());
        SysUser student = application.getStudentId() == null ? null : studentMap.get(application.getStudentId());
        overview.setCandidateName(student != null ? Optional.ofNullable(student.getFullName()).orElse(student.getUsername()) : "匿名候选人");
        overview.setStatus(application.getStatus());
        overview.setAppliedAt(application.getAppliedAt());
        overview.setResumeId(application.getResumeId());
        return overview;
    }

    private InterviewOverview toInterviewOverview(Interview interview,
                                                  Map<Long, JobPosting> jobMap,
                                                  Map<Long, JobApplication> applicationMap,
                                                  Map<Long, Resume> resumeMap,
                                                  Map<Long, SysUser> studentMap) {
        InterviewOverview overview = new InterviewOverview();
        overview.setId(interview.getId());
        overview.setJobId(interview.getJobId());
        JobPosting job = jobMap.get(interview.getJobId());
        overview.setJobTitle(job != null ? job.getTitle() : "岗位已删除");
        overview.setApplicationId(interview.getApplicationId());
        JobApplication application = interview.getApplicationId() == null ? null : applicationMap.get(interview.getApplicationId());
        Long studentId = application != null ? application.getStudentId() : null;
        if (studentId == null && application != null) {
            Resume resume = application.getResumeId() == null ? null : resumeMap.get(application.getResumeId());
            if (resume != null) {
                studentId = resume.getStudentId();
            }
        }
        SysUser student = studentId == null ? null : studentMap.get(studentId);
        overview.setCandidateName(student != null ? Optional.ofNullable(student.getFullName()).orElse(student.getUsername()) : "候选人");
        overview.setStatus(interview.getStatus());
        overview.setScheduledTime(interview.getScheduledTime());
        overview.setLocation(interview.getLocation());
        overview.setMeetingLink(interview.getMeetingLink());
        overview.setFeedback(interview.getFeedback());
        return overview;
    }

    private EmployerJobDetailResponse toJobDetail(JobPosting posting, List<String> requirements, long applicationCount) {
        EmployerJobDetailResponse detail = new EmployerJobDetailResponse();
        detail.setJob(posting);
        detail.setRequirements(requirements);
        detail.setApplicationCount(applicationCount);
        return detail;
    }

    private List<ModuleInfo> buildModules() {
        List<ModuleInfo> modules = new ArrayList<>();
        modules.add(module("overview", "企业总览", "掌握招聘动态与运营指标", List.of("岗位统计", "人才转化")));
        modules.add(module("jobs", "职位管理", "发布与管理招聘岗位", List.of("岗位发布", "要求配置", "状态控制")));
        modules.add(module("applications", "简历管理", "筛选候选人并推进流程", List.of("状态更新", "候选人概览", "进度追踪")));
        modules.add(module("interviews", "面试管理", "安排与记录面试过程", List.of("面试排期", "会议信息", "结果反馈")));
        modules.add(module("talent", "人才库", "沉淀优质候选人资源", List.of("候选人画像", "人才分层", "邀约记录")));
        return modules;
    }

    private ModuleInfo module(String key, String name, String description, List<String> capabilities) {
        ModuleInfo module = new ModuleInfo();
        module.setKey(key);
        module.setName(name);
        module.setDescription(description);
        module.setCapabilities(capabilities);
        return module;
    }

    private void applyJobRequest(JobPosting posting, EmployerJobRequest request) {
        posting.setTitle(request.getTitle().trim());
        posting.setDescription(trimToNull(request.getDescription()));
        posting.setSalaryRange(trimToNull(request.getSalaryRange()));
        posting.setLocation(trimToNull(request.getLocation()));
        posting.setWorkType(request.getWorkType());
        posting.setStatus(request.getStatus());
        posting.setClosingDate(request.getClosingDate());
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

    private void notifyStudentInterview(Long studentUserId, boolean isCreate, Interview interview) {
        if (studentUserId == null) return;
        SystemNotification n = new SystemNotification();
        n.setUserId(studentUserId);
        n.setCategory(SystemNotification.Category.INTERVIEW);
        n.setTitle(isCreate ? "面试安排通知" : "面试安排已更新");
        n.setContent(isCreate ? "企业已为您安排面试，请及时查看时间与地点。" : "您的面试安排已更新，请查看最新时间与地点。");
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);
    }

    private void applyInterview(Interview interview, EmployerInterviewRequest request) {
        interview.setJobId(request.getJobId());
        interview.setApplicationId(request.getApplicationId());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setLocation(trimToNull(request.getLocation()));
        interview.setMeetingLink(trimToNull(request.getMeetingLink()));
        interview.setStatus(Optional.ofNullable(request.getStatus()).orElse(Interview.Status.SCHEDULED));
        interview.setFeedback(trimToNull(request.getFeedback()));
    }

    private InterviewOverview buildInterviewResponse(Interview interview,
                                                      JobPosting posting,
                                                      JobApplication application) {
        Map<Long, JobPosting> jobMap = Map.of(posting.getId(), posting);
        Map<Long, JobApplication> applicationMap = Map.of(application.getId(), application);
        Resume resume = application.getResumeId() == null ? null : resumeService.getById(application.getResumeId());
        Map<Long, Resume> resumeMap = resume == null ? Collections.emptyMap() : Map.of(resume.getId(), resume);
        SysUser candidate = application.getStudentId() == null ? null : sysUserService.getById(application.getStudentId());
        Map<Long, SysUser> studentMap = candidate == null ? Collections.emptyMap() : Map.of(candidate.getId(), candidate);
        return toInterviewOverview(interview, jobMap, applicationMap, resumeMap, studentMap);
    }

    private List<String> normalizeRequirements(List<String> requirements) {
        if (requirements == null) {
            return Collections.emptyList();
        }
        return requirements.stream()
                .map(item -> item == null ? null : item.trim())
                .filter(item -> item != null && !item.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 给所有管理员发送一条“有企业资料待审核”的系统通知
     */
    private void notifyAdminsForProfileUpdate(SysUser user, String roleLabel) {
        List<SysUser> admins = sysUserService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, SysUser.Role.ADMIN));
        for (SysUser admin : admins) {
            SystemNotification notification = new SystemNotification();
            notification.setUserId(admin.getId());
            notification.setCategory(SystemNotification.Category.SYSTEM);
            notification.setTitle(roleLabel + "资料待审核");
            String name = user.getFullName() != null ? user.getFullName() : user.getUsername();
            notification.setContent("用户【" + name + "】提交了" + roleLabel + "个人资料更新申请，请及时审核。");
            notification.setReadFlag(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationService.save(notification);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<JobApplication> mergeApplications(List<JobApplication> existing, List<JobApplication> additional) {
        if (additional.isEmpty()) {
            return existing;
        }
        Map<Long, JobApplication> merged = new HashMap<>();
        for (JobApplication app : existing) {
            merged.put(app.getId(), app);
        }
        for (JobApplication app : additional) {
            merged.put(app.getId(), app);
        }
        return new ArrayList<>(merged.values());
    }
}
