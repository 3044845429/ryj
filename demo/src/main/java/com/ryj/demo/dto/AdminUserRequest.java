package com.ryj.demo.dto;

import com.ryj.demo.entity.SysUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    private String password; // 可选，如果为空则不更新密码
    
    @NotBlank(message = "姓名不能为空")
    private String fullName;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    
    @NotBlank(message = "角色不能为空")
    private String role; // STUDENT, TEACHER, EMPLOYER, ADMIN
    
    private String status; // ACTIVE, DISABLED
}

