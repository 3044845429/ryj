package com.ryj.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ryj.demo.common.ApiResponse;
import com.ryj.demo.dto.TeacherDashboardResponse;
import com.ryj.demo.dto.TeacherProfileApprovalDetail;
import com.ryj.demo.dto.TeacherProfileResponse;
import com.ryj.demo.dto.TeacherProfileUpdateRequest;
import com.ryj.demo.entity.ProfileUpdateRequest;
import com.ryj.demo.entity.SysUser;
import com.ryj.demo.entity.SystemNotification;
import com.ryj.demo.entity.Teacher;
import com.ryj.demo.service.ProfileUpdateRequestService;
import com.ryj.demo.service.SysUserService;
import com.ryj.demo.service.SystemNotificationService;
import com.ryj.demo.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final ProfileUpdateRequestService profileUpdateRequestService;
    private final SysUserService sysUserService;
    private final SystemNotificationService notificationService;

    @PostMapping
    public ApiResponse<Teacher> create(@Valid @RequestBody Teacher teacher) {
        teacher.setId(null);
        teacherService.save(teacher);
        return ApiResponse.success(teacher);
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @Valid @RequestBody TeacherProfileUpdateRequest request) {
        // 通过教师ID找到对应的用户ID
        Teacher teacher = teacherService.getById(id);
        if (teacher == null || teacher.getUserId() == null) {
            return ApiResponse.failure(404, "未找到对应的教师记录，无法提交资料更新申请");
        }

        SysUser user = sysUserService.getById(teacher.getUserId());
        if (user == null || user.getRole() != SysUser.Role.TEACHER) {
            return ApiResponse.failure(403, "当前账号不是教师角色，无法提交教师资料更新申请");
        }

        // 检查是否已有待审核记录
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProfileUpdateRequest> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProfileUpdateRequest>()
                        .eq(ProfileUpdateRequest::getUserId, user.getId())
                        .eq(ProfileUpdateRequest::getRole, "TEACHER")
                        .eq(ProfileUpdateRequest::getStatus, "PENDING");
        ProfileUpdateRequest existing = profileUpdateRequestService.getOne(wrapper);
        if (existing != null) {
            return ApiResponse.failure(400, "已有待审核的教师资料更新申请，请等待管理员审核后再提交新的修改");
        }

        // 将请求序列化为 JSON 存入 payload
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String payload = mapper.writeValueAsString(request);

            ProfileUpdateRequest record = new ProfileUpdateRequest();
            record.setUserId(user.getId());
            record.setRole("TEACHER");
            record.setPayload(payload);
            record.setStatus("PENDING");
            profileUpdateRequestService.save(record);

            // 给所有管理员发送系统通知
            notifyAdminsForProfileUpdate(user, "教师");

            return ApiResponse.success("资料更新申请已提交，等待管理员审核", true);
        } catch (Exception e) {
            return ApiResponse.failure(500, "保存资料更新申请失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(teacherService.removeById(id));
    }

    @GetMapping
    public ApiResponse<Page<Teacher>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) String department) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Teacher::getUserId, userId);
        }
        if (department != null && !department.isBlank()) {
            wrapper.like(Teacher::getDepartment, department);
        }
        Page<Teacher> result = teacherService.page(new Page<>(page, size), wrapper);
        return ApiResponse.success(result);
    }

    @GetMapping("/all")
    public ApiResponse<List<Map<String, Object>>> listAllTeachers() {
        List<Teacher> teachers = teacherService.list();
        List<Map<String, Object>> result = teachers.stream().map(t -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("userId", t.getUserId());
            // 教师名取用户表fullName，查不到就留空
            String name = "";
            try {
                com.ryj.demo.entity.SysUser user = teacherService.getUserIfExists(t.getUserId());
                if (user != null && user.getFullName() != null) {
                    name = user.getFullName();
                }
            } catch (Exception ignore) {}
            map.put("name", name);
            map.put("department", t.getDepartment());
            map.put("major", t.getMajor());
            map.put("email", t.getEmail());
            return map;
        }).toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<TeacherProfileResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(teacherService.getProfile(id));
    }

    /**
     * 通过用户ID获取教师信息
     */
    @GetMapping("/by-user/{userId}")
    public ApiResponse<Teacher> getByUserId(@PathVariable Long userId) {
        return ApiResponse.success(teacherService.getByUserId(userId));
    }

    /**
     * 获取教师仪表板数据
     */
    @GetMapping("/{teacherId}/dashboard")
    public ApiResponse<TeacherDashboardResponse> getDashboard(@PathVariable Long teacherId) {
        return ApiResponse.success(teacherService.getDashboardData(teacherId));
    }

    /**
     * 获取档案审核详情
     */
    @GetMapping("/{teacherId}/requests/{requestId}")
    public ApiResponse<TeacherProfileApprovalDetail> getProfileApprovalDetail(
            @PathVariable Long teacherId,
            @PathVariable Long requestId) {
        return ApiResponse.success(teacherService.getProfileApprovalDetail(teacherId, requestId));
    }

    /**
     * 审批学生档案更新请求
     */
    @PutMapping("/{teacherId}/requests/{requestId}/approve")
    public ApiResponse<Boolean> approveProfileUpdate(
            @PathVariable Long teacherId,
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> payload) {
        String reviewComment = payload != null ? payload.get("reviewComment") : null;
        return ApiResponse.success(teacherService.approveProfileUpdate(teacherId, requestId, reviewComment));
    }

    /**
     * 拒绝学生档案更新请求
     */
    @PutMapping("/{teacherId}/requests/{requestId}/reject")
    public ApiResponse<Boolean> rejectProfileUpdate(
            @PathVariable Long teacherId,
            @PathVariable Long requestId,
            @RequestBody Map<String, String> payload) {
        String reviewComment = payload.get("reviewComment");
        return ApiResponse.success(teacherService.rejectProfileUpdate(teacherId, requestId, reviewComment));
    }

    /**
     * 给所有管理员发送一条“有教师资料待审核”的系统通知
     */
    private void notifyAdminsForProfileUpdate(SysUser user, String roleLabel) {
        List<SysUser> admins = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, SysUser.Role.ADMIN));
        for (SysUser admin : admins) {
            SystemNotification notification = new SystemNotification();
            notification.setUserId(admin.getId());
            notification.setCategory(SystemNotification.Category.SYSTEM);
            notification.setTitle(roleLabel + "资料待审核");
            String name = user.getFullName() != null ? user.getFullName() : user.getUsername();
            notification.setContent("用户【" + name + "】提交了" + roleLabel + "个人资料更新申请，请及时审核。");
            notification.setReadFlag(false);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationService.save(notification);
        }
    }
}
