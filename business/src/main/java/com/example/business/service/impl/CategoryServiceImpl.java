package com.example.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.business.entity.Category;
import com.example.business.entity.Product;
import com.example.business.mapper.CategoryMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.request.CategoryListRequest;
import com.example.business.request.CategoryRequest;
import com.example.business.response.CategoryResponse;
import com.example.business.service.CategoryService;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import com.example.common.exception.ErrorCode;
import com.example.common.response.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    private static final long ROOT_CATEGORY_ID = 0L;

    private final ProductMapper productMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper, ProductMapper productMapper) {
        this.baseMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<CategoryResponse> getEnabledTree() {
        List<Category> categories = this.list(new LambdaQueryWrapper<Category>()
                        .eq(Category::getEnabled, 1)
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId))
                .stream()
                .filter(category -> Integer.valueOf(1).equals(category.getEnabled()))
                .sorted(Comparator
                        .comparing(Category::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Category::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        Map<Long, CategoryResponse> rootsById = new LinkedHashMap<>();
        for (Category category : categories) {
            if (isRoot(category.getParentId())) {
                CategoryResponse response = toResponse(category);
                rootsById.put(response.getId(), response);
            }
        }

        for (Category category : categories) {
            if (!isRoot(category.getParentId())) {
                CategoryResponse parent = rootsById.get(category.getParentId());
                if (parent != null) {
                    parent.getChildren().add(toResponse(category));
                }
            }
        }

        return new ArrayList<>(rootsById.values());
    }

    @Override
    public PageResponse<CategoryResponse> getAdminPage(CategoryListRequest request) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            query.like(Category::getName, request.getKeyword().trim());
        }
        query.eq(request.getParentId() != null, Category::getParentId, request.getParentId())
                .eq(request.getEnabled() != null, Category::getEnabled, request.getEnabled())
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId);

        Page<Category> page = this.page(Page.of(request.getPage(), request.getSize()), query);
        PageResponse<CategoryResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(page.getRecords().stream().map(this::toResponse).toList());
        return response;
    }

    @Override
    @Transactional
    public boolean saveCategory(CategoryRequest request) {
        Long parentId = request.getParentId() == null ? ROOT_CATEGORY_ID : request.getParentId();
        validateEnabled(request.getEnabled());
        validateParent(request.getId(), parentId);

        Category category;
        if (request.getId() == null) {
            category = new Category();
        } else {
            category = findCategory(request.getId());
        }

        BeanUtil.copyProperties(request, category);
        category.setParentId(parentId);
        category.setSortOrder(request.getSortOrder() == null ? 99 : request.getSortOrder());
        return this.saveOrUpdate(category);
    }

    @Override
    @Transactional
    public boolean deleteCategory(Long id) {
        findCategory(id);

        long childCount = this.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (childCount > 0) {
            throw conflict("分类下存在子分类，无法删除");
        }

        long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw conflict("分类已被商品引用，无法删除");
        }

        return this.removeById(id);
    }

    private void validateParent(Long categoryId, Long parentId) {
        if (isRoot(parentId)) {
            return;
        }
        if (parentId.equals(categoryId)) {
            throw new CustomValidationException("分类不能将自身设为父分类");
        }

        Category parent = findCategory(parentId);
        if (!isRoot(parent.getParentId())) {
            throw new CustomValidationException("商品分类最多支持两级");
        }

        if (categoryId != null && this.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryId)) > 0) {
            throw new CustomValidationException("存在子分类的顶级分类不能移动到其他分类下");
        }
    }

    private void validateEnabled(Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new CustomValidationException("启用状态只能为0或1");
        }
    }

    private boolean isRoot(Long parentId) {
        return parentId == null || parentId == ROOT_CATEGORY_ID;
    }

    private Category findCategory(Long id) {
        Category category = this.getById(id);
        if (category == null) {
            throw new NoSuchElementException("分类不存在");
        }
        return category;
    }

    private CategoryResponse toResponse(Category category) {
        return BeanUtil.copyProperties(category, CategoryResponse.class);
    }

    private CustomBusinessException conflict(String message) {
        return new CustomBusinessException(message, ErrorCode.CONFLICT.getCode());
    }
}
