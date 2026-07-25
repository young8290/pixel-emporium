package com.example.business.controller;

import com.example.business.request.ProductSkuListRequest;
import com.example.business.response.ProductSkuResponse;
import com.example.business.service.ProductSkuService;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("product-sku")
public class ProductSkuController {
    @Autowired
    private ProductSkuService productSkuService;

    @Operation(summary = "分页查询商品SKU")
    @GetMapping("list")
    public R<PageResponse<ProductSkuResponse>> list(@Valid ProductSkuListRequest request) {
        return R.ok(productSkuService.getProductSkuPage(request));
    }

}
