package com.ryj.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ryj.demo.common.ApiResponse;
import com.ryj.demo.dto.TeacherGuidanceRequest;
import com.ryj.demo.entity.Teacher;
import com.ryj.demo.entity.TeacherGuidance;
import com.ryj.demo.entity.SystemNotification;
import com.ryj.demo.service.TeacherGuidanceService;
import com.ryj.demo.service.TeacherService;
import com.ryj.demo.service.SystemNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/teacher-guidance")
@RequiredArgsConstructor
public class TeacherGuidanceController {

    private final TeacherGuidanceService teacherGuidanceService;
    private final TeacherService teacherService;
    private final SystemNotificationService notificationService;

    /**
     * 创建指导记录
     */
    @PostMapping
    public ApiResponse<TeacherGuidance> create(@Valid @RequestBody TeacherGuidanceRequest request) {
        TeacherGuidance guidance = new TeacherGuidance();
        guidance.setTeacherId(request.getTeacherId());
        guidance.setStudentId(request.getStudentId());
        guidance.setNote(request.getNote());
        teacherGuidanceService.save(guidance);
        if (guidance.getStudentId() != null) {
            notifyStudentNewGuidance(guidance.getStudentId());
        }
        if (guidance.getTeacherId() != null) {
            notifyTeacherNewGuidance(guidance.getTeacherId(), guidance.getStudentId());
        }
        return ApiResponse.success(guidance);
    }

    /**
     * 分页查询指导记录
     */
    @GetMapping
    public ApiResponse<Page<TeacherGuidance>> list(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long teacherId,
                                                   @RequestParam(required = false) Long studentId) {
        LambdaQueryWrapper<TeacherGuidance> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(TeacherGuidance::getTeacherId, teacherId);
        }
        if (studentId != null) {
            wrapper.eq(TeacherGuidance::getStudentId, studentId);
        }
        wrapper.orderByDesc(TeacherGuidance::getCreatedAt);
        Page<TeacherGuidance> result = teacherGuidanceService.page(new Page<>(page, size), wrapper);
        return ApiResponse.success(result);
    }

    /**
     * 更新指导记录
     */
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody TeacherGuidanceRequest request) {
        TeacherGuidance guidance = teacherGuidanceService.getById(id);
        if (guidance == null) {
            return ApiResponse.failure(404, "指导记录不存在");
        }
        if (request.getNote() != null) {
            guidance.setNote(request.getNote());
        }
        return ApiResponse.success(teacherGuidanceService.updateById(guidance));
    }

    /**
     * 删除指导记录
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(teacherGuidanceService.removeById(id));
    }

    /**
     * 批量添加指导记录
     */
    @PostMapping("/batch")
    public ApiResponse<Boolean> createBatch(@Valid @RequestBody List<TeacherGuidanceRequest> requests) {
        List<TeacherGuidance> guidances = requests.stream().map(request -> {
            TeacherGuidance guidance = new TeacherGuidance();
            guidance.setTeacherId(request.getTeacherId());
            guidance.setStudentId(request.getStudentId());
            guidance.setNote(request.getNote());
            return guidance;
        }).toList();
        boolean saved = teacherGuidanceService.saveBatch(guidances);
        if (saved) {
            for (TeacherGuidance g : guidances) {
                if (g.getStudentId() != null) {
                    notifyStudentNewGuidance(g.getStudentId());
                }
                if (g.getTeacherId() != null) {
                    notifyTeacherNewGuidance(g.getTeacherId(), g.getStudentId());
                }
            }
        }
        return ApiResponse.success(saved);
    }

    private void notifyStudentNewGuidance(Long studentUserId) {
        SystemNotification n = new SystemNotification();
        n.setUserId(studentUserId);
        n.setCategory(SystemNotification.Category.GUIDANCE);
        n.setTitle("就业指导更新");
        n.setContent("教师为您添加了新的就业指导记录，请及时查看。");
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);
    }

    private void notifyTeacherNewGuidance(Long teacherId, Long studentUserId) {
        if (teacherId == null) {
            return;
        }
        Teacher teacher = teacherService.getById(teacherId);
        if (teacher == null || teacher.getUserId() == null) {
            return;
        }

        SystemNotification n = new SystemNotification();
        n.setUserId(teacher.getUserId());
        n.setCategory(SystemNotification.Category.GUIDANCE);
        n.setTitle("新的学生就业指导记录");
        n.setContent("有学生与您建立或更新了就业指导记录，请及时关注学生的就业进展。");
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationService.save(n);
    }
}
