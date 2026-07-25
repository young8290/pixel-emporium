package com.example.business.controller.admin;

import com.example.business.request.CategoryListRequest;
import com.example.business.request.CategoryRequest;
import com.example.business.response.CategoryResponse;
import com.example.business.annotation.OperationLog;
import com.example.business.service.CategoryService;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/category")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class CategoryAdminController {
    private final CategoryService categoryService;

    public CategoryAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public R<PageResponse<CategoryResponse>> list(@Valid CategoryListRequest request) {
        return R.ok(categoryService.getAdminPage(request));
    }

    @Operation(summary = "新增/修改分类")
    @PostMapping("/save")
    public R<Boolean> save(@RequestBody @Valid CategoryRequest request) {
        return R.ok(categoryService.saveCategory(request));
    }

    @Operation(summary = "删除分类")
    @OperationLog(action = "CATEGORY_DELETE", targetType = "CATEGORY", targetIdSpEL = "#id")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(categoryService.deleteCategory(id));
    }
}
