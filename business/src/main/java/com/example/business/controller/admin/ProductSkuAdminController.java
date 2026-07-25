package com.example.business.controller.admin;

import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.R;
import com.example.business.request.ProductSkuListRequest;
import com.example.business.request.ProductSkuRequest;
import com.example.common.response.PageResponse;
import com.example.business.response.ProductSkuResponse;
import com.example.business.request.ProductSkuGenerateRequest;
import com.example.business.response.ProductSkuGenerateResponse;
import com.example.business.service.ProductSkuService;
import com.example.business.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/product-sku")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class ProductSkuAdminController {
    private final ProductSkuService productSkuService;

    public ProductSkuAdminController(ProductSkuService productSkuService) {
        this.productSkuService = productSkuService;
    }

    @Operation(summary = "商品SKU列表")
    @GetMapping("/list")
    public R<PageResponse<ProductSkuResponse>> list(@Valid ProductSkuListRequest request) {
        return R.ok(productSkuService.getAdminProductSkuPage(request));
    }

    @Operation(summary = "新增/修改商品SKU")
    @PostMapping("/save")
    @OperationLog(action = "SKU_SAVE", targetType = "SKU", targetIdSpEL = "#request.id")
    public R<Boolean> save(@RequestBody @Valid ProductSkuRequest request) {
        if (request.getId() == null) {
            return R.ok(productSkuService.adminAdd(request));
        } else {
            return R.ok(productSkuService.adminUpdate(request));
        }
    }

    @Operation(summary = "批量生成商品SKU")
    @PostMapping("/generate")
    @OperationLog(action = "SKU_GENERATE", targetType = "PRODUCT", targetIdSpEL = "#request.productId")
    public R<ProductSkuGenerateResponse> generate(@RequestBody @Valid ProductSkuGenerateRequest request) {
        return R.ok(productSkuService.generate(request));
    }

    @Operation(summary = "删除商品SKU")
    @OperationLog(action = "SKU_DELETE", targetType = "SKU", targetIdSpEL = "#id")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id, @RequestParam Long productId) {
        return R.ok(productSkuService.deleteSku(productId, id));
    }

    @Operation(summary = "批量删除商品SKU")
    @DeleteMapping
    @OperationLog(action = "SKU_BATCH_DELETE", targetType = "PRODUCT", targetIdSpEL = "#productId")
    public R<Boolean> deleteBatch(@RequestParam Long productId, @RequestBody List<Long> ids) {
        return R.ok(productSkuService.deleteSkuBatch(productId, ids));
    }
}
