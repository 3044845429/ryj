package com.ryj.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ryj.demo.common.ApiResponse;
import com.ryj.demo.dto.JobPostingRequest;
import com.ryj.demo.entity.JobPosting;
import com.ryj.demo.service.JobPostingService;
import com.ryj.demo.service.JobRequirementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final JobRequirementService jobRequirementService;

    @PostMapping
    @Transactional
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody JobPostingRequest request) {
        JobPosting posting = new JobPosting();
        applyRequest(posting, request);
        jobPostingService.save(posting);
        jobRequirementService.replaceRequirements(posting.getId(), request.getRequirements());
        return detail(posting.getId());
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody JobPostingRequest request) {
        JobPosting posting = jobPostingService.getById(id);
        if (posting == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        applyRequest(posting, request);
        jobPostingService.updateById(posting);
        jobRequirementService.replaceRequirements(id, request.getRequirements());
        return detail(id);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        JobPosting posting = jobPostingService.getById(id);
        if (posting == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("posting", posting);
        result.put("requirements", jobRequirementService.findByJobId(id));
        return ApiResponse.success(result);
    }

    @GetMapping
    public ApiResponse<Page<JobPosting>> list(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Long employerId,
                                              @RequestParam(required = false) JobPosting.WorkType workType,
                                              @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<JobPosting> wrapper = new LambdaQueryWrapper<>();
        if (employerId != null) {
            wrapper.eq(JobPosting::getEmployerId, employerId);
        } else {
            // 学生端/公开列表：只展示招聘中的职位，按发布时间倒序
            wrapper.eq(JobPosting::getStatus, JobPosting.Status.OPEN);
        }
        if (workType != null) {
            wrapper.eq(JobPosting::getWorkType, workType);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(JobPosting::getTitle, keyword);
        }
        wrapper.orderByDesc(JobPosting::getPublishedDate);
        Page<JobPosting> result = jobPostingService.page(new Page<>(page, size), wrapper);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        jobRequirementService.replaceRequirements(id, null);
        return ApiResponse.success(jobPostingService.removeById(id));
    }

    private void applyRequest(JobPosting posting, JobPostingRequest request) {
        posting.setEmployerId(request.getEmployerId());
        posting.setTitle(request.getTitle());
        posting.setDescription(request.getDescription());
        posting.setSalaryRange(request.getSalaryRange());
        posting.setLocation(request.getLocation());
        posting.setWorkType(request.getWorkType());
        posting.setStatus(request.getStatus());
        posting.setPublishedDate(request.getPublishedDate());
        posting.setClosingDate(request.getClosingDate());
    }
}
