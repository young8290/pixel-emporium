package com.example.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.StockLog;
import com.example.business.mapper.StockLogMapper;
import com.example.business.request.StockLogListRequest;
import com.example.business.response.StockLogResponse;
import com.example.business.service.StockLogService;
import com.example.common.response.PageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存变动流水服务实现（BE-P2-03）。
 */
@Service
public class StockLogServiceImpl extends ServiceImpl<StockLogMapper, StockLog>
        implements StockLogService {

    @Override
    public boolean exists(String bizNo, Long productSkuId, String type) {
        if (bizNo == null || bizNo.isBlank()) {
            return false;
        }
        return this.baseMapper.exists(new LambdaQueryWrapper<StockLog>()
                .eq(StockLog::getBizNo, bizNo)
                .eq(StockLog::getProductSkuId, productSkuId)
                .eq(StockLog::getType, type));
    }

    @Override
    public void record(Long productSkuId, int changeQty, int beforeStock, int afterStock,
                       String type, String bizNo, Long operatorId, String remark) {
        StockLog log = new StockLog();
        log.setProductSkuId(productSkuId);
        log.setChangeQty(changeQty);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setType(type);
        log.setBizNo(bizNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        this.save(log);
    }

    @Override
    public PageResponse<StockLogResponse> adminList(StockLogListRequest request) {
        LambdaQueryWrapper<StockLog> qw = new LambdaQueryWrapper<>();
        qw.eq(request.getProductSkuId() != null, StockLog::getProductSkuId, request.getProductSkuId())
                .eq(request.getType() != null && !request.getType().isBlank(),
                        StockLog::getType, request.getType())
                .eq(request.getBizNo() != null && !request.getBizNo().isBlank(),
                        StockLog::getBizNo, request.getBizNo())
                .orderByDesc(StockLog::getCreatedAt);

        Page<StockLog> page = this.page(Page.of(request.getPage(), request.getSize()), qw);
        List<StockLogResponse> list = page.getRecords().stream()
                .map(o -> BeanUtil.copyProperties(o, StockLogResponse.class))
                .toList();

        PageResponse<StockLogResponse> resp = new PageResponse<>();
        resp.setTotal(page.getTotal());
        resp.setList(list);
        return resp;
    }
}
