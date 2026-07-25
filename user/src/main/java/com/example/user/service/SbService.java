package com.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;
import com.example.user.entity.Sb;
import com.example.user.response.SbResponse;

public interface SbService extends IService<Sb> {
    PageResponse<SbResponse> getSbPage(PageRequest request);
}