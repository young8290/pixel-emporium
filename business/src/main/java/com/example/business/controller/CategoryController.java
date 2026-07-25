package com.example.business.controller;

import com.example.business.response.CategoryResponse;
import com.example.business.service.CategoryService;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("category")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "公开分类树")
    @GetMapping("/tree")
    public R<List<CategoryResponse>> tree() {
        return R.ok(categoryService.getEnabledTree());
    }
}
