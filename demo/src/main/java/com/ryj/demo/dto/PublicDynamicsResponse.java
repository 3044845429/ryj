package com.ryj.demo.dto;

import com.ryj.demo.dto.PublicOverviewResponse.NotificationItem;
import lombok.Data;

import java.util.List;

/**
 * 分页返回网上发布的资讯动态（供资讯动态页浏览）
 */
@Data
public class PublicDynamicsResponse {
    private List<NotificationItem> records;
    private long total;
    private long current;
    private long size;
    private long pages;
}
