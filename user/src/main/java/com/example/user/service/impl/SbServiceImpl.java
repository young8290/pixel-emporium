package com.example.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;
import com.example.user.entity.Sb;
import com.example.user.mapper.SbMapper;
import com.example.user.response.SbResponse;
import com.example.user.service.SbService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SbServiceImpl
        extends ServiceImpl<SbMapper, Sb>
        implements SbService {

    @Override
    public PageResponse<SbResponse> getSbPage(PageRequest request) {
        LambdaQueryWrapper<Sb> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper
                .eq(Sb::getUserId, UserContext.get())
                .orderByDesc(Sb::getId);

        Page<Sb> page = this.page(
                Page.of(request.getPage(), request.getSize()),
                queryWrapper
        );

        List<SbResponse> list =
                BeanUtil.copyToList(page.getRecords(), SbResponse.class);

        PageResponse<SbResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(list);

        return response;
    }
}