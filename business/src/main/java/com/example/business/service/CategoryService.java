package com.example.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.business.entity.Category;
import com.example.business.request.CategoryListRequest;
import com.example.business.request.CategoryRequest;
import com.example.business.response.CategoryResponse;
import com.example.common.response.PageResponse;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<CategoryResponse> getEnabledTree();

    PageResponse<CategoryResponse> getAdminPage(CategoryListRequest request);

    boolean saveCategory(CategoryRequest request);

    boolean deleteCategory(Long id);
}
