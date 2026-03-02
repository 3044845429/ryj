package com.ryj.demo.controller;

import com.ryj.demo.common.ApiResponse;
import com.ryj.demo.dto.PublicDynamicsResponse;
import com.ryj.demo.dto.PublicOverviewResponse;
import com.ryj.demo.dto.PublicOverviewResponse.HighlightModule;
import com.ryj.demo.dto.PublicOverviewResponse.Hero;
import com.ryj.demo.dto.PublicOverviewResponse.JobSummary;
import com.ryj.demo.dto.PublicOverviewResponse.NotificationItem;
import com.ryj.demo.dto.PublicOverviewResponse.QuickLink;
import com.ryj.demo.dto.PublicOverviewResponse.ResourceSummary;
import com.ryj.demo.dto.PublicOverviewResponse.Statistics;
import com.ryj.demo.dto.PublicSearchResponse;
import com.ryj.demo.dto.PublicSearchResult;
import com.ryj.demo.entity.Employer;
import com.ryj.demo.entity.JobPosting;
import com.ryj.demo.entity.PublicResource;
import com.ryj.demo.entity.PublicSearchHistory;
import com.ryj.demo.entity.SystemNotification;
import com.ryj.demo.service.EmployerService;
import com.ryj.demo.service.JobPostingService;
import com.ryj.demo.service.PublicResourceService;
import com.ryj.demo.service.PublicSearchHistoryService;
import com.ryj.demo.service.StudentProfileService;
import com.ryj.demo.service.SystemNotificationService;
import com.ryj.demo.service.TeacherService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicModuleController {

    private final JobPostingService jobPostingService;
    private final EmployerService employerService;
    private final StudentProfileService studentProfileService;
    private final TeacherService teacherService;
    private final SystemNotificationService notificationService;
    private final PublicSearchHistoryService searchHistoryService;
    private final PublicResourceService resourceService;

    @Value("${app.public-module.storage-path:uploads}")
    private String storageDirectory;

    @GetMapping("/overview")
    public ApiResponse<PublicOverviewResponse> overview() {
        PublicOverviewResponse response = new PublicOverviewResponse();

        Hero hero = new Hero();
        hero.setTitle("高校就业服务枢纽");
        hero.setSubtitle("面向管理员、学生、教师与企业的统一就业门户");
        hero.setDescription("集成全局搜索、即时通知与资料共享功能，帮助各角色快速联通就业生态。");
        hero.setBadges(List.of("智能推荐", "实时同步", "一站式服务"));
        response.setHero(hero);

        Statistics statistics = new Statistics();
        statistics.setTotalJobs(jobPostingService.lambdaQuery()
                .eq(JobPosting::getStatus, JobPosting.Status.OPEN)
                .count());
        statistics.setActiveEmployers(employerService.count());
        statistics.setRegisteredStudents(studentProfileService.count());
        statistics.setServiceTeachers(teacherService.count());
        response.setStatistics(statistics);

        List<String> focusAreas = List.of(
                "全局搜索实时整合招聘、通知、企业信息",
                "统一消息中心覆盖站内信与系统公告",
                "多角色共享的资料库支持上传与预览"
        );
        response.setFocusAreas(focusAreas);

        List<HighlightModule> modules = new ArrayList<>();

        HighlightModule searchModule = new HighlightModule();
        searchModule.setName("全局搜索");
        searchModule.setDescription("结合智能提示与高级筛选，快速定位目标信息");
        searchModule.setAccentColor("#2563eb");
        searchModule.setFeatures(List.of(
                "跨角色统一搜索入口",
                "高级筛选支持角色、地点与类型",
                "自动记录搜索历史，便于快速回访"
        ));
        modules.add(searchModule);

        HighlightModule notificationModule = new HighlightModule();
        notificationModule.setName("消息通知中心");
        notificationModule.setDescription("多渠道同步提醒重要动态与待办");
        notificationModule.setAccentColor("#9333ea");
        notificationModule.setFeatures(List.of(
                "系统公告与面试邀请统一查看",
                "支持未读快速筛查",
                "按角色推送个性化提示"
        ));
        modules.add(notificationModule);

        HighlightModule resourceModule = new HighlightModule();
        resourceModule.setName("资料共享");
        resourceModule.setDescription("高质量模板与政策解读随时下载");
        resourceModule.setAccentColor("#0ea5e9");
        resourceModule.setFeatures(List.of(
                "简历、协议等模板统一存放",
                "企业及院校资料集中共享",
                "支持批量上传与安全下载"
        ));
        modules.add(resourceModule);
        response.setModules(modules);

        List<QuickLink> quickLinks = new ArrayList<>();
        quickLinks.add(buildQuickLink("智能职位搜索", "探索最新开放的岗位需求", "/jobs"));
        quickLinks.add(buildQuickLink("通知中心", "查看未读提醒与面试安排", "/notifications"));
        quickLinks.add(buildQuickLink("资料下载", "下载简历模板与政策文件", "/resources"));
        quickLinks.add(buildQuickLink("就业数据驾驶舱", "掌握实时就业动态", "/dashboard"));
        response.setQuickLinks(quickLinks);

        List<JobSummary> jobSummaries = jobPostingService.lambdaQuery()
                .eq(JobPosting::getStatus, JobPosting.Status.OPEN)
                .orderByDesc(JobPosting::getPublishedDate)
                .last("limit 6")
                .list()
                .stream()
                .map(this::toJobSummary)
                .collect(Collectors.toList());
        response.setTrendingJobs(jobSummaries);

        List<NotificationItem> notificationItems = notificationService.lambdaQuery()
                .orderByDesc(SystemNotification::getCreatedAt)
                .last("limit 6")
                .list()
                .stream()
                .map(this::toNotificationItem)
                .collect(Collectors.toList());
        response.setNotifications(notificationItems);

        List<ResourceSummary> resourceSummaries = resourceService.lambdaQuery()
                .orderByDesc(PublicResource::getCreatedAt)
                .last("limit 6")
                .list()
                .stream()
                .map(this::toResourceSummary)
                .collect(Collectors.toList());
        response.setResources(resourceSummaries);

        return ApiResponse.success(response);
    }

    /**
     * 资讯动态列表（网上发布的各类动态讯息，供所有人浏览，无需登录）
     */
    @GetMapping("/dynamics")
    public ApiResponse<PublicDynamicsResponse> dynamics(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            try {
                SystemNotification.Category cat = SystemNotification.Category.valueOf(category.trim().toUpperCase());
                wrapper.eq(SystemNotification::getCategory, cat);
            } catch (IllegalArgumentException ignored) {
                // 忽略无效分类
            }
        }
        wrapper.orderByDesc(SystemNotification::getCreatedAt);
        Page<SystemNotification> p = notificationService.page(new Page<>(page, size), wrapper);
        PublicDynamicsResponse response = new PublicDynamicsResponse();
        response.setRecords(p.getRecords().stream().map(this::toNotificationItem).collect(Collectors.toList()));
        response.setTotal(p.getTotal());
        response.setCurrent(p.getCurrent());
        response.setSize(p.getSize());
        response.setPages(p.getPages());
        return ApiResponse.success(response);
    }

    @GetMapping("/search")
    public ApiResponse<PublicSearchResponse> search(@RequestParam String keyword,
                                                    @RequestParam(required = false) String role,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String location,
                                                    @RequestParam(required = false) Long userId,
                                                    HttpServletRequest request) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(trimmedKeyword)) {
            return ApiResponse.failure(400, "搜索关键字不能为空");
        }

        Set<String> categories = new HashSet<>();
        if (StringUtils.hasText(category)) {
            for (String item : category.split(",")) {
                if (StringUtils.hasText(item)) {
                    categories.add(item.trim().toLowerCase());
                }
            }
        }

        List<PublicSearchResult> results = new ArrayList<>();
        Map<String, Long> breakdown = new HashMap<>();

        if (categories.isEmpty() || categories.contains("job")) {
            List<JobPosting> jobPostings = jobPostingService.lambdaQuery()
                    .and(wrapper -> wrapper.like(JobPosting::getTitle, trimmedKeyword)
                            .or(w -> w.like(JobPosting::getDescription, trimmedKeyword)))
                    .eq(JobPosting::getStatus, JobPosting.Status.OPEN)
                    .like(StringUtils.hasText(location), JobPosting::getLocation, location)
                    .orderByDesc(JobPosting::getPublishedDate)
                    .last("limit 10")
                    .list();
            for (JobPosting jobPosting : jobPostings) {
                results.add(buildJobResult(jobPosting));
            }
            breakdown.put("job", (long) jobPostings.size());
        }

        if (categories.isEmpty() || categories.contains("employer") || categories.contains("company")) {
            List<Employer> employers = employerService.lambdaQuery()
                    .and(wrapper -> wrapper.like(Employer::getCompanyName, trimmedKeyword)
                            .or(w -> w.like(Employer::getDescription, trimmedKeyword)))
                    .orderByAsc(Employer::getCompanyName)
                    .last("limit 6")
                    .list();
            for (Employer employer : employers) {
                results.add(buildEmployerResult(employer));
            }
            breakdown.put("employer", (long) employers.size());
        }

        if (categories.isEmpty() || categories.contains("notification") || categories.contains("message")) {
            List<SystemNotification> notifications = notificationService.lambdaQuery()
                    .and(wrapper -> wrapper.like(SystemNotification::getTitle, trimmedKeyword)
                            .or(w -> w.like(SystemNotification::getContent, trimmedKeyword)))
                    .orderByDesc(SystemNotification::getCreatedAt)
                    .last("limit 6")
                    .list();
            for (SystemNotification notification : notifications) {
                results.add(buildNotificationResult(notification));
            }
            breakdown.put("notification", (long) notifications.size());
        }

        if (categories.isEmpty() || categories.contains("resource") || categories.contains("file")) {
            List<PublicResource> resources = resourceService.lambdaQuery()
                    .and(wrapper -> wrapper.like(PublicResource::getFileName, trimmedKeyword)
                            .or(w -> w.like(PublicResource::getDescription, trimmedKeyword)))
                    .orderByDesc(PublicResource::getCreatedAt)
                    .last("limit 6")
                    .list();
            for (PublicResource resource : resources) {
                results.add(buildResourceResult(resource));
            }
            breakdown.put("resource", (long) resources.size());
        }

        results.sort((a, b) -> {
            LocalDateTime t1 = a.getTimestamp();
            LocalDateTime t2 = b.getTimestamp();
            if (t1 == null && t2 == null) {
                return 0;
            }
            if (t1 == null) {
                return 1;
            }
            if (t2 == null) {
                return -1;
            }
            return t2.compareTo(t1);
        });

        if (userId != null) {
            PublicSearchHistory history = new PublicSearchHistory();
            history.setUserId(userId);
            history.setKeyword(trimmedKeyword);
            history.setRoleFilter(role);
            history.setCategoryFilter(category);
            history.setLocationFilter(location);
            history.setAdvancedOptions(request.getQueryString());
            history.setCreatedAt(LocalDateTime.now());
            searchHistoryService.save(history);
        }

        PublicSearchResponse response = new PublicSearchResponse();
        response.setResults(results);
        response.setTotal(results.size());
        response.setBreakdown(breakdown);
        response.setSuggestions(buildSuggestions(trimmedKeyword));

        return ApiResponse.success(response);
    }

    @GetMapping("/search/history")
    public ApiResponse<List<PublicSearchHistory>> history(@RequestParam Long userId) {
        List<PublicSearchHistory> historyList = searchHistoryService.lambdaQuery()
                .eq(PublicSearchHistory::getUserId, userId)
                .orderByDesc(PublicSearchHistory::getCreatedAt)
                .last("limit 20")
                .list();
        return ApiResponse.success(historyList);
    }

    @DeleteMapping("/search/history/{id}")
    public ApiResponse<Boolean> removeHistory(@PathVariable Long id, @RequestParam Long userId) {
        return ApiResponse.success(searchHistoryService.lambdaUpdate()
                .eq(PublicSearchHistory::getId, id)
                .eq(PublicSearchHistory::getUserId, userId)
                .remove());
    }

    @DeleteMapping("/search/history")
    public ApiResponse<Boolean> clearHistory(@RequestParam Long userId) {
        return ApiResponse.success(searchHistoryService.lambdaUpdate()
                .eq(PublicSearchHistory::getUserId, userId)
                .remove());
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResourceSummary> uploadFile(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) Long uploaderId,
                                                   @RequestParam(required = false) String description) throws IOException {
        if (file.isEmpty()) {
            return ApiResponse.failure(400, "请选择需要上传的文件");
        }

        Path basePath = Paths.get(storageDirectory).toAbsolutePath();
        Files.createDirectories(basePath);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;
        Path target = basePath.resolve(storedName);
        file.transferTo(target);

        PublicResource resource = new PublicResource();
        resource.setUploaderId(uploaderId);
        resource.setFileName(originalFilename);
        resource.setFileType(file.getContentType());
        resource.setFileSize(file.getSize());
        resource.setStoragePath(target.toString());
        resource.setDescription(description);
        resource.setCreatedAt(LocalDateTime.now());
        resourceService.save(resource);

        resource.setDownloadUrl("/api/public/files/" + resource.getId() + "/download");
        resourceService.updateById(resource);

        return ApiResponse.success(toResourceSummary(resource));
    }

    @GetMapping("/files")
    public ApiResponse<List<ResourceSummary>> listFiles() {
        List<ResourceSummary> resources = resourceService.lambdaQuery()
                .orderByDesc(PublicResource::getCreatedAt)
                .list()
                .stream()
                .map(this::toResourceSummary)
                .collect(Collectors.toList());
        return ApiResponse.success(resources);
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {
        PublicResource resource = resourceService.getById(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        Path path = Paths.get(resource.getStoragePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource fileResource = new FileSystemResource(path);
        String encodedFileName = URLEncoder.encode(resource.getFileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFileName)
                .body(fileResource);
    }

    private QuickLink buildQuickLink(String label, String description, String target) {
        QuickLink quickLink = new QuickLink();
        quickLink.setLabel(label);
        quickLink.setDescription(description);
        quickLink.setTarget(target);
        return quickLink;
    }

    private JobSummary toJobSummary(JobPosting jobPosting) {
        JobSummary summary = new JobSummary();
        summary.setId(jobPosting.getId());
        summary.setTitle(jobPosting.getTitle());
        summary.setLocation(jobPosting.getLocation());
        summary.setSalaryRange(jobPosting.getSalaryRange());
        summary.setWorkType(jobPosting.getWorkType() != null ? jobPosting.getWorkType().name() : null);
        summary.setPublishedDate(jobPosting.getPublishedDate());
        return summary;
    }

    private NotificationItem toNotificationItem(SystemNotification notification) {
        NotificationItem item = new NotificationItem();
        item.setId(notification.getId());
        item.setTitle(notification.getTitle());
        item.setContent(notification.getContent());
        item.setCategory(notification.getCategory() != null ? notification.getCategory().name() : null);
        item.setCreatedAt(notification.getCreatedAt());
        return item;
    }

    private ResourceSummary toResourceSummary(PublicResource resource) {
        ResourceSummary summary = new ResourceSummary();
        summary.setId(resource.getId());
        summary.setFileName(resource.getFileName());
        summary.setDescription(resource.getDescription());
        summary.setFileType(resource.getFileType());
        summary.setFileSize(resource.getFileSize());
        summary.setCreatedAt(resource.getCreatedAt());
        summary.setDownloadUrl(resource.getDownloadUrl());
        return summary;
    }

    private PublicSearchResult buildJobResult(JobPosting jobPosting) {
        PublicSearchResult result = new PublicSearchResult();
        result.setType("job");
        result.setTitle(jobPosting.getTitle());
        result.setSubtitle(jobPosting.getLocation());
        result.setDescription(jobPosting.getDescription());
        result.setLink("/jobs/" + jobPosting.getId());
        result.setTimestamp(jobPosting.getPublishedDate());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("salaryRange", jobPosting.getSalaryRange());
        metadata.put("workType", jobPosting.getWorkType() != null ? jobPosting.getWorkType().name() : null);
        result.setMetadata(metadata);
        return result;
    }

    private PublicSearchResult buildEmployerResult(Employer employer) {
        PublicSearchResult result = new PublicSearchResult();
        result.setType("employer");
        result.setTitle(employer.getCompanyName());
        result.setSubtitle(employer.getContactPerson());
        result.setDescription(employer.getDescription());
        result.setLink("/employers/" + employer.getId());
        result.setTimestamp(LocalDateTime.now());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contactEmail", employer.getContactEmail());
        metadata.put("contactPhone", employer.getContactPhone());
        metadata.put("website", employer.getWebsite());
        result.setMetadata(metadata);
        return result;
    }

    private PublicSearchResult buildNotificationResult(SystemNotification notification) {
        PublicSearchResult result = new PublicSearchResult();
        result.setType("notification");
        result.setTitle(notification.getTitle());
        result.setSubtitle(notification.getCategory() != null ? notification.getCategory().name() : "系统通知");
        result.setDescription(notification.getContent());
        result.setLink("/notifications/" + notification.getId());
        result.setTimestamp(notification.getCreatedAt());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("readFlag", notification.getReadFlag());
        result.setMetadata(metadata);
        return result;
    }

    private PublicSearchResult buildResourceResult(PublicResource resource) {
        PublicSearchResult result = new PublicSearchResult();
        result.setType("resource");
        result.setTitle(resource.getFileName());
        result.setSubtitle("共享资料");
        result.setDescription(resource.getDescription());
        result.setLink(resource.getDownloadUrl());
        result.setTimestamp(resource.getCreatedAt());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileSize", resource.getFileSize());
        metadata.put("fileType", resource.getFileType());
        result.setMetadata(metadata);
        return result;
    }

    private List<String> buildSuggestions(String keyword) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("尝试使用“" + keyword + "” + 城市名称进行区域搜索");
        suggestions.add("开启高级筛选可按角色查看专属内容");
        suggestions.add("下载资料模板，快速准备面试与入职材料");
        return suggestions;
    }
}
