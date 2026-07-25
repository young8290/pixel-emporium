package com.example.business.controller.admin;

import com.example.business.request.ProductAttributeRequest;
import com.example.business.request.ProductAttributeValueRequest;
import com.example.business.response.ProductAttributeResponse;
import com.example.business.service.ProductAttributeService;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("admin/product-attribute")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class ProductAttributeAdminController {
    private final ProductAttributeService service;

    public ProductAttributeAdminController(ProductAttributeService service) {
        this.service = service;
    }

    @Operation(summary = "商品规格树")
    @GetMapping("/tree")
    public R<List<ProductAttributeResponse>> tree(@RequestParam Long productId) {
        return R.ok(service.listTree(productId));
    }

    @Operation(summary = "保存商品规格")
    @PostMapping("/save")
    public R<Boolean> save(@RequestBody @Valid ProductAttributeRequest request) {
        return R.ok(service.saveAttribute(request));
    }

    @Operation(summary = "删除商品规格")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id, @RequestParam Long productId) {
        return R.ok(service.deleteAttribute(productId, id));
    }

    @Operation(summary = "保存商品规格值")
    @PostMapping("/value/save")
    public R<Boolean> saveValue(@RequestBody @Valid ProductAttributeValueRequest request) {
        return R.ok(service.saveValue(request));
    }

    @Operation(summary = "删除商品规格值")
    @DeleteMapping("/value/{id}")
    public R<Boolean> deleteValue(@PathVariable Long id, @RequestParam Long productId) {
        return R.ok(service.deleteValue(productId, id));
    }
}
