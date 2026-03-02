package com.ryj.demo.dto;

import com.ryj.demo.entity.SysUser;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminDashboardResponse {
    private Header header;
    private List<ModuleInfo> modules;
    private UserStatistics userStatistics;
    private ContentStatistics contentStatistics;
    private SystemStatistics systemStatistics;
    private List<UserOverview> recentUsers;
    private List<ActivityLog> recentActivities;

    @Data
    public static class Header {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalStudents;
        private Long totalEmployers;
        private Long totalTeachers;
        private Long totalJobPostings;
        private Long activeJobPostings;
        private Long totalApplications;
        private Long pendingNotifications;
    }

    @Data
    public static class ModuleInfo {
        private String key;
        private String name;
        private String description;
        private List<String> capabilities;
    }

    @Data
    public static class UserStatistics {
        private Long totalUsers;
        private Long activeUsers;
        private Long disabledUsers;
        private Map<SysUser.Role, Long> usersByRole;
        private Long newUsersLast30Days;
        private Long activeUsersLast7Days;
        // 用户活跃度趋势（按天统计最近30天）
        private List<DailyStat> dailyNewUsersLast30Days;
        // 按角色统计的用户活跃度
        private Map<SysUser.Role, Long> activeUsersByRole;
        // 最近30天每日活跃用户数（基于updatedAt）
        private List<DailyStat> dailyActiveUsersLast30Days;
    }

    @Data
    public static class ContentStatistics {
        private Long totalJobPostings;
        private Long activeJobPostings;
        private Long totalApplications;
        private Long totalResumes;
        private Long totalNotifications;
        private Long unreadNotifications;
        // 内容发布量趋势（最近30天）
        private List<DailyStat> dailyJobPostingsLast30Days;
        private List<DailyStat> dailyApplicationsLast30Days;
        private List<DailyStat> dailyResumesLast30Days;
        private List<DailyStat> dailyNotificationsLast30Days;
        // 最近7天各类内容发布量
        private Long jobPostingsLast7Days;
        private Long applicationsLast7Days;
        private Long resumesLast7Days;
        private Long notificationsLast7Days;
        // 内容发布量按类型统计
        private Map<String, Long> contentByType;
    }

    @Data
    public static class SystemStatistics {
        private Long totalInterviews;
        private Long scheduledInterviews;
        private Long completedInterviews;
        private Long totalGuidanceRecords;
        private Long totalSearchHistory;
        // 系统使用统计
        private Long searchesLast30Days;
        private List<DailyStat> dailySearchesLast30Days;
        private Long guidanceRecordsLast30Days;
        private Long interviewsLast30Days;
        private Long publicResourcesLast30Days;
        // 各类系统活动的统计
        private Map<String, Long> systemActivityByType;
    }

    @Data
    public static class UserOverview {
        private Long id;
        private String username;
        private String fullName;
        private SysUser.Role role;
        private SysUser.Status status;
        private String email;
        private String phone;
        private String createdAt;
        private String lastActiveAt;
    }

    @Data
    public static class ActivityLog {
        private Long id;
        private String action;
        private String description;
        private String userId;
        private String userName;
        private String timestamp;
        private String category;
    }
    
    /**
     * 每日统计数据
     */
    @Data
    public static class DailyStat {
        private String date; // 日期字符串，格式：yyyy-MM-dd
        private Long count;  // 数量
    }
}

