package com.ryj.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("profile_update_request")
public class ProfileUpdateRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 仅允许 TEACHER / EMPLOYER，与 sys_user.role 对应子集 */
    private String role;

    /** 序列化后的资料变更内容（JSON 字符串） */
    private String payload;

    /** 审核状态：PENDING / APPROVED / REJECTED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private Long reviewerId;

    private String reviewComment;
}

