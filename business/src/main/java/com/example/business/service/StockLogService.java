package com.example.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.business.entity.StockLog;
import com.example.business.request.StockLogListRequest;
import com.example.business.response.StockLogResponse;
import com.example.common.response.PageResponse;

/**
 * 库存变动流水服务（BE-P2-03）。
 */
public interface StockLogService extends IService<StockLog> {

    /**
     * 幂等判断：给定业务单号 + SKU + 类型的流水是否已存在。
     * 用于下单扣减防重（同一订单不重复扣减同一 SKU）。
     *
     * @return true 表示已存在（应跳过本次变动）
     */
    boolean exists(String bizNo, Long productSkuId, String type);

    /**
     * 记录一条库存变动流水（在调用方事务内执行，保证 before/after 与实际一致）。
     */
    void record(Long productSkuId, int changeQty, int beforeStock, int afterStock,
                String type, String bizNo, Long operatorId, String remark);

    /**
     * 管理端分页查询库存流水（只读）。
     */
    PageResponse<StockLogResponse> adminList(StockLogListRequest request);
}
