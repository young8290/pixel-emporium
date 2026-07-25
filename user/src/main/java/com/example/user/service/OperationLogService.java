package com.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.response.PageResponse;
import com.example.user.entity.OperationLog;
import com.example.user.request.OperationLogListRequest;
import com.example.user.response.OperationLogResponse;

public interface OperationLogService extends IService<OperationLog> {
    void record(OperationLog operationLog);

    PageResponse<OperationLogResponse> adminList(OperationLogListRequest request);
}
