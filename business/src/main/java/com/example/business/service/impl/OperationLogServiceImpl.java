package com.example.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.OperationLog;
import com.example.business.mapper.OperationLogMapper;
import com.example.business.request.OperationLogListRequest;
import com.example.business.response.OperationLogResponse;
import com.example.business.service.OperationLogService;
import com.example.common.response.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作审计日志服务实现（BE-P2-01）。
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    /**
     * 异步写入审计日志：审计失败绝不影响主业务流程。
     */
    @Async
    @Override
    public void record(OperationLog operationLog) {
        try {
            if (operationLog.getResult() == null || operationLog.getResult().isBlank()) {
                operationLog.setResult("SUCCESS");
            }
            this.save(operationLog);
        } catch (Exception e) {
            log.warn("写入操作审计日志失败: action={}, target={}:{}, err={}",
                    operationLog.getAction(), operationLog.getTargetType(),
                    operationLog.getTargetId(), e.getMessage());
        }
    }

    @Override
    public PageResponse<OperationLogResponse> adminList(OperationLogListRequest request) {
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<>();
        qw.eq(request.getUserId() != null, OperationLog::getUserId, request.getUserId())
                .eq(request.getAction() != null && !request.getAction().isBlank(),
                        OperationLog::getAction, request.getAction())
                .eq(request.getTargetType() != null && !request.getTargetType().isBlank(),
                        OperationLog::getTargetType, request.getTargetType())
                .eq(request.getTargetId() != null && !request.getTargetId().isBlank(),
                        OperationLog::getTargetId, request.getTargetId())
                .ge(request.getCreatedAtStart() != null,
                        OperationLog::getCreatedAt, request.getCreatedAtStart())
                .le(request.getCreatedAtEnd() != null,
                        OperationLog::getCreatedAt, request.getCreatedAtEnd())
                .orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> page = this.page(Page.of(request.getPage(), request.getSize()), qw);
        List<OperationLogResponse> records = page.getRecords().stream()
                .map(o -> BeanUtil.copyProperties(o, OperationLogResponse.class))
                .toList();

        PageResponse<OperationLogResponse> resp = new PageResponse<>();
        resp.setTotal(page.getTotal());
        resp.setList(records);
        return resp;
    }
}
