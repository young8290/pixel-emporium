package com.example.business;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.Category;
import com.example.business.mapper.CategoryMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.request.CategoryListRequest;
import com.example.business.response.CategoryResponse;
import com.example.business.service.impl.CategoryServiceImpl;
import com.example.common.exception.CustomBusinessException;
import com.example.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ProductMapper productMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryMapper, productMapper);
    }

    @Test
    void getEnabledTreeBuildsSortedTwoLevelTree() {
        Category fruit = category(1L, "水果", 0L, 20, 1);
        Category apple = category(2L, "苹果", 1L, 20, 1);
        Category citrus = category(3L, "柑橘", 1L, 10, 1);
        Category disabled = category(4L, "停用分类", 0L, 5, 0);
        when(categoryMapper.selectList(any()))
                .thenReturn(List.of(apple, disabled, fruit, citrus));

        List<CategoryResponse> tree = categoryService.getEnabledTree();

        assertEquals(1, tree.size());
        assertEquals("水果", tree.getFirst().getName());
        assertEquals(List.of("柑橘", "苹果"), tree.getFirst().getChildren().stream()
                .map(CategoryResponse::getName)
                .toList());
    }

    @Test
    void getAdminPageReturnsFlatPage() {
        Category fruit = category(1L, "水果", 0L, 20, 1);
        when(categoryMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> {
                    Page<Category> page = invocation.getArgument(0);
                    page.setRecords(List.of(fruit));
                    page.setTotal(1);
                    return page;
                });
        CategoryListRequest request = new CategoryListRequest();
        request.setKeyword("果");

        PageResponse<CategoryResponse> result = categoryService.getAdminPage(request);

        assertEquals(1L, result.getTotal());
        assertEquals("水果", result.getList().getFirst().getName());
    }

    @Test
    void deleteCategoryRejectsCategoryWithChildren() {
        Category fruit = category(1L, "水果", 0L, 20, 1);
        when(categoryMapper.selectById(1L)).thenReturn(fruit);
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        CustomBusinessException exception = assertThrows(
                CustomBusinessException.class,
                () -> categoryService.deleteCategory(1L)
        );

        assertEquals(409, exception.getCode());
        verifyNoInteractions(productMapper);
    }

    @Test
    void deleteCategoryRejectsCategoryReferencedByProduct() {
        Category fruit = category(1L, "水果", 0L, 20, 1);
        when(categoryMapper.selectById(1L)).thenReturn(fruit);
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(productMapper.selectCount(any())).thenReturn(1L);

        CustomBusinessException exception = assertThrows(
                CustomBusinessException.class,
                () -> categoryService.deleteCategory(1L)
        );

        assertEquals(409, exception.getCode());
    }

    private Category category(Long id, String name, Long parentId, Integer sortOrder, Integer enabled) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setSortOrder(sortOrder);
        category.setEnabled(enabled);
        return category;
    }
}
