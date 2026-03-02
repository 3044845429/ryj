package com.ryj.demo.dto;

import com.ryj.demo.entity.JobApplication;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生端「我的申请」列表项，包含职位与公司信息。
 */
@Data
public class StudentApplicationItem {
    private Long id;
    private Long jobId;
    private Long studentId;
    private Long resumeId;
    private JobApplication.Status status;
    private String coverLetter;
    private LocalDateTime appliedAt;

    private String jobTitle;
    private String companyName;
    private String location;
    private String salaryRange;
    private String workType;
}
