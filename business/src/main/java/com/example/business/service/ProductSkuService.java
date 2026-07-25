package com.example.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.business.entity.ProductSku;
import com.example.business.request.ProductSkuListRequest;
import com.example.common.response.PageResponse;
import com.example.business.response.ProductSkuResponse;
import com.example.business.request.ProductSkuRequest;
import com.example.business.request.ProductSkuGenerateRequest;
import com.example.business.response.ProductSkuGenerateResponse;

import java.util.List;

public interface ProductSkuService extends IService<ProductSku> {
    /**
     * 原子扣减库存，库存不足或 SKU 不存在时抛出业务异常。
     */
    void deductStock(Long id, Integer qty);

    /**
     * 原子回滚库存，SKU 不存在时抛出业务异常。
     */
    void restoreStock(Long id, Integer qty);

    /**
     * 带流水与幂等的原子扣减（BE-P2-03）。
     * 同一 {@code bizNo}+SKU+DEDUCT 的流水已存在时视为重复请求，跳过扣减（幂等）。
     * 在同一事务内原子扣减并写入 before/after 流水。
     *
     * @param bizNo      业务单号（订单号），用于幂等
     * @param operatorId 操作人ID（可空）
     */
    void deductStock(Long id, Integer qty, String bizNo, Long operatorId);

    /**
     * 带流水的原子回滚（BE-P2-03）。同一事务内回滚并写 RESTORE 流水。
     */
    void restoreStock(Long id, Integer qty, String bizNo, Long operatorId);

    /**
     * 分页查询当前用户的商品SKU
     */
    PageResponse<ProductSkuResponse> getProductSkuPage(ProductSkuListRequest request);

    /**
     * 【管理员】分页查询全量商品SKU
     */
    PageResponse<ProductSkuResponse> getAdminProductSkuPage(ProductSkuListRequest request);

    /**
     * 【管理员】添加商品SKU
     * 如果该实体表有关联的 userId 字段，管理员任意可以指定该字段的值
     */
    boolean adminAdd(ProductSkuRequest request);

    /**
     * 【管理员】更新商品SKU信息 (跨用户强制操作)
     */
    boolean adminUpdate(ProductSkuRequest request);

    /**
     * 管理员按商品归属删除 SKU。
     */
    boolean deleteSku(Long productId, Long id);

    /**
     * 【管理员】批量逻辑删除商品SKU (无需所有权检查)
     */
    boolean adminDeleteBatch(List<Long> ids);

    /**
     * 管理员按商品归属批量删除 SKU。
     */
    boolean deleteSkuBatch(Long productId, List<Long> ids);

    ProductSkuGenerateResponse generate(ProductSkuGenerateRequest request);

    boolean saveSku(ProductSkuRequest request);
}
