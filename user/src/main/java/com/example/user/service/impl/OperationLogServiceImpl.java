package com.example.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.response.PageResponse;
import com.example.user.entity.OperationLog;
import com.example.user.mapper.OperationLogMapper;
import com.example.user.request.OperationLogListRequest;
import com.example.user.response.OperationLogResponse;
import com.example.user.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {
    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Override
    public void record(OperationLog operationLog) {
        try {
            if (operationLog.getResult() == null || operationLog.getResult().isBlank()) {
                operationLog.setResult("SUCCESS");
            }
            save(operationLog);
        } catch (Exception exception) {
            log.warn("写入用户操作审计失败: action={}, target={}:{}, err={}",
                    operationLog.getAction(), operationLog.getTargetType(),
                    operationLog.getTargetId(), exception.getMessage());
        }
    }

    @Override
    public PageResponse<OperationLogResponse> adminList(OperationLogListRequest request) {
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<>();
        query.eq(request.getUserId() != null, OperationLog::getUserId, request.getUserId())
                .eq(hasText(request.getAction()), OperationLog::getAction, request.getAction())
                .eq(hasText(request.getTargetType()), OperationLog::getTargetType, request.getTargetType())
                .eq(hasText(request.getTargetId()), OperationLog::getTargetId, request.getTargetId())
                .orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> page = page(Page.of(request.getPage(), request.getSize()), query);
        PageResponse<OperationLogResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(page.getRecords().stream()
                .map(row -> BeanUtil.copyProperties(row, OperationLogResponse.class))
                .toList());
        return response;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
