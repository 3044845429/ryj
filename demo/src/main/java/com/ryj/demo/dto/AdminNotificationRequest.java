package com.ryj.demo.dto;

import com.ryj.demo.entity.SystemNotification;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminNotificationRequest {
    private Long userId; // 如果为null，则发送给所有用户
    
    @NotBlank(message = "通知标题不能为空")
    private String title;
    
    @NotBlank(message = "通知内容不能为空")
    private String content;
    
    private SystemNotification.Category category = SystemNotification.Category.SYSTEM;
    
    private Boolean readFlag = false;
}

