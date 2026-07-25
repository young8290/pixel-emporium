package com.example.business.controller.admin;

import com.example.business.annotation.OperationLog;
import com.example.business.request.ProductListRequest;
import com.example.business.request.ProductRequest;
import com.example.business.response.ProductResponse;
import com.example.business.service.ProductService;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/product")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class ProductAdminController {
    private final ProductService productService;

    public ProductAdminController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/list")
    public R<PageResponse<ProductResponse>> list(@Valid ProductListRequest request) {
        return R.ok(productService.getProductPage(request));
    }

    @PostMapping("/save")
    @OperationLog(action = "PRODUCT_SAVE", targetType = "PRODUCT", targetIdSpEL = "#request.id")
    public R<Boolean> save(@RequestBody @Valid ProductRequest request) {
        return R.ok(productService.saveOrUpdateProduct(request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(action = "PRODUCT_DELETE", targetType = "PRODUCT", targetIdSpEL = "#id")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(productService.deleteProduct(id));
    }
}
