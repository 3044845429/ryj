package com.ryj.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ryj.demo.dto.AdminDashboardResponse;
import com.ryj.demo.dto.AdminNotificationRequest;
import com.ryj.demo.dto.AdminUserRequest;
import com.ryj.demo.entity.ProfileUpdateRequest;
import com.ryj.demo.entity.SysUser;

public interface AdminService extends IService<SysUser> {
    /**
     * 获取管理员仪表板数据
     */
    AdminDashboardResponse getDashboardData(Long adminUserId);
    
    /**
     * 创建或更新用户（管理员操作）
     */
    SysUser createOrUpdateUser(AdminUserRequest request, Long userId);
    
    /**
     * 更新用户状态
     */
    boolean updateUserStatus(Long userId, SysUser.Status status);
    
    /**
     * 更新用户角色
     */
    boolean updateUserRole(Long userId, SysUser.Role role);
    
    /**
     * 发送系统通知（可批量发送）
     * @param adminUserId 发送通知的管理员ID，管理员自己不会收到该通知
     */
    boolean sendSystemNotification(Long adminUserId, AdminNotificationRequest request);
    
    /**
     * 验证管理员权限
     */
    void requireAdminRole(Long userId);
    
    /**
     * 删除用户（级联删除相关数据）
     */
    boolean deleteUser(Long userId);

    /**
     * 分页获取教师/企业资料更新申请
     */
    Page<ProfileUpdateRequest> getProfileUpdateRequests(Long adminUserId,
                                                        String role,
                                                        String status,
                                                        long page,
                                                        long size);

    /**
     * 审批资料更新申请：通过
     */
    boolean approveProfileUpdateRequest(Long adminUserId, Long requestId, String reviewComment);

    /**
     * 审批资料更新申请：驳回
     */
    boolean rejectProfileUpdateRequest(Long adminUserId, Long requestId, String reviewComment);
}

