package com.example.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.business.entity.OperationLog;
import com.example.business.request.OperationLogListRequest;
import com.example.business.response.OperationLogResponse;
import com.example.common.response.PageResponse;

/**
 * 操作审计日志服务（BE-P2-01）。
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 记录一条审计日志（异步写入，失败不影响主流程）。
     */
    void record(OperationLog log);

    /**
     * 管理端分页查询操作日志（只读）。
     */
    PageResponse<OperationLogResponse> adminList(OperationLogListRequest request);
}
