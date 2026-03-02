package com.ryj.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ryj.demo.common.ApiResponse;
import com.ryj.demo.dto.AdminDashboardResponse;
import com.ryj.demo.dto.AdminNotificationRequest;
import com.ryj.demo.dto.AdminUserRequest;
import com.ryj.demo.entity.ProfileUpdateRequest;
import com.ryj.demo.entity.SysUser;
import com.ryj.demo.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 获取管理员仪表板数据
     */
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard(@RequestParam Long userId) {
        return ApiResponse.success(adminService.getDashboardData(userId));
    }

    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/users")
    public ApiResponse<Page<SysUser>> listUsers(
            @RequestParam Long adminUserId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) SysUser.Role role,
            @RequestParam(required = false) SysUser.Status status,
            @RequestParam(required = false) String keyword) {
        adminService.requireAdminRole(adminUserId);
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (role != null) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getFullName, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        Page<SysUser> result = adminService.page(new Page<>(page, size), 
                wrapper.orderByDesc(SysUser::getCreatedAt));
        return ApiResponse.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{id}")
    public ApiResponse<SysUser> getUserDetail(@RequestParam Long adminUserId, @PathVariable Long id) {
        adminService.requireAdminRole(adminUserId);
        return ApiResponse.success(adminService.getById(id));
    }

    /**
     * 创建用户
     */
    @PostMapping("/users")
    public ApiResponse<SysUser> createUser(@RequestParam Long adminUserId,
                                           @Valid @RequestBody AdminUserRequest request) {
        return ApiResponse.success(adminService.createOrUpdateUser(request, adminUserId));
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    public ApiResponse<SysUser> updateUser(@RequestParam Long adminUserId,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AdminUserRequest request) {
        adminService.requireAdminRole(adminUserId);
        // 设置用户ID以便更新
        SysUser existing = adminService.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        request.setUsername(existing.getUsername()); // 保持用户名不变
        return ApiResponse.success(adminService.createOrUpdateUser(request, adminUserId));
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/users/{id}/status")
    public ApiResponse<Boolean> updateUserStatus(@RequestParam Long adminUserId,
                                                 @PathVariable Long id,
                                                 @RequestParam String status) {
        adminService.requireAdminRole(adminUserId);
        try {
            SysUser.Status statusEnum = SysUser.Status.valueOf(status.toUpperCase());
            return ApiResponse.success(adminService.updateUserStatus(id, statusEnum));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的状态类型: " + status);
        }
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    public ApiResponse<Boolean> updateUserRole(@RequestParam Long adminUserId,
                                              @PathVariable Long id,
                                              @RequestParam String role) {
        adminService.requireAdminRole(adminUserId);
        try {
            SysUser.Role roleEnum = SysUser.Role.valueOf(role.toUpperCase());
            return ApiResponse.success(adminService.updateUserRole(id, roleEnum));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的角色类型: " + role);
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public ApiResponse<Boolean> deleteUser(@RequestParam Long adminUserId, @PathVariable Long id) {
        adminService.requireAdminRole(adminUserId);
        // 防止删除自己
        if (id.equals(adminUserId)) {
            throw new IllegalArgumentException("不能删除自己的账户");
        }
        return ApiResponse.success(adminService.deleteUser(id));
    }

    /**
     * 发送系统通知
     */
    @PostMapping("/notifications")
    public ApiResponse<Boolean> sendNotification(@RequestParam Long adminUserId,
                                                 @Valid @RequestBody AdminNotificationRequest request) {
        adminService.requireAdminRole(adminUserId);
        return ApiResponse.success(adminService.sendSystemNotification(adminUserId, request));
    }

    /**
     * 分页获取教师/企业资料更新申请列表
     */
    @GetMapping("/profile-requests")
    public ApiResponse<Page<ProfileUpdateRequest>> listProfileUpdateRequests(
            @RequestParam Long adminUserId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.getProfileUpdateRequests(adminUserId, role, status, page, size));
    }

    /**
     * 审核通过资料更新申请
     */
    @PutMapping("/profile-requests/{id}/approve")
    public ApiResponse<Boolean> approveProfileUpdateRequest(@RequestParam Long adminUserId,
                                                            @PathVariable Long id,
                                                            @RequestBody(required = false) java.util.Map<String, String> payload) {
        String reviewComment = payload != null ? payload.get("reviewComment") : null;
        return ApiResponse.success(adminService.approveProfileUpdateRequest(adminUserId, id, reviewComment));
    }

    /**
     * 驳回资料更新申请
     */
    @PutMapping("/profile-requests/{id}/reject")
    public ApiResponse<Boolean> rejectProfileUpdateRequest(@RequestParam Long adminUserId,
                                                           @PathVariable Long id,
                                                           @RequestBody java.util.Map<String, String> payload) {
        String reviewComment = payload != null ? payload.get("reviewComment") : null;
        return ApiResponse.success(adminService.rejectProfileUpdateRequest(adminUserId, id, reviewComment));
    }
}

