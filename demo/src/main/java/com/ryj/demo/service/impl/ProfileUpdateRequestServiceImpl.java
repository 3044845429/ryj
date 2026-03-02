package com.ryj.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ryj.demo.entity.ProfileUpdateRequest;
import com.ryj.demo.mapper.ProfileUpdateRequestMapper;
import com.ryj.demo.service.ProfileUpdateRequestService;
import org.springframework.stereotype.Service;

@Service
public class ProfileUpdateRequestServiceImpl extends ServiceImpl<ProfileUpdateRequestMapper, ProfileUpdateRequest>
        implements ProfileUpdateRequestService {
}

