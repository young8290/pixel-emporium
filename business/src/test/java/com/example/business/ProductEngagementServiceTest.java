package com.example.business;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.BrowseHistory;
import com.example.business.entity.Product;
import com.example.business.entity.ProductFavorite;
import com.example.business.mapper.BrowseHistoryMapper;
import com.example.business.mapper.ProductFavoriteMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.response.BrowseHistoryResponse;
import com.example.business.response.ProductResponse;
import com.example.business.service.impl.BrowseHistoryServiceImpl;
import com.example.business.service.impl.ProductFavoriteServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.request.PageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEngagementServiceTest {
    @Mock
    private ProductFavoriteMapper favoriteMapper;
    @Mock
    private BrowseHistoryMapper historyMapper;
    @Mock
    private ProductMapper productMapper;

    private ProductFavoriteServiceImpl favoriteService;
    private BrowseHistoryServiceImpl historyService;

    @BeforeEach
    void setUp() {
        UserContext.set(11L);
        favoriteService = new ProductFavoriteServiceImpl(favoriteMapper, productMapper);
        historyService = new BrowseHistoryServiceImpl(historyMapper, productMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void repeatedFavoriteIsIdempotentAndOwnedByCurrentUser() {
        when(productMapper.selectById(7L)).thenReturn(product(7L, 1));
        when(favoriteMapper.existsActive(11L, 7L)).thenReturn(false, true);
        when(favoriteMapper.insert(any(ProductFavorite.class))).thenReturn(1);

        assertTrue(favoriteService.add(7L));
        assertTrue(favoriteService.add(7L));

        ArgumentCaptor<ProductFavorite> inserted = ArgumentCaptor.forClass(ProductFavorite.class);
        verify(favoriteMapper).insert(inserted.capture());
        assertEquals(11L, inserted.getValue().getUserId());
        assertEquals(7L, inserted.getValue().getProductId());
    }

    @Test
    void favoriteStatusAndRemovalUseCurrentUserScope() {
        when(favoriteMapper.existsActive(11L, 7L)).thenReturn(true);
        when(favoriteMapper.logicalDeleteActive(11L, 7L)).thenReturn(0);

        assertTrue(favoriteService.isFavorite(7L));
        assertTrue(favoriteService.remove(7L));

        verify(favoriteMapper).existsActive(11L, 7L);
        verify(favoriteMapper).logicalDeleteActive(11L, 7L);
    }

    @Test
    void favoriteListUsesCurrentUserAndPreservesPageMetadata() {
        ProductResponse product = new ProductResponse();
        product.setId(7L);
        Page<ProductResponse> result = Page.of(2, 5);
        result.setRecords(List.of(product));
        result.setTotal(6);
        when(favoriteMapper.selectProductPage(any(), eq(11L))).thenReturn(result);

        PageRequest request = new PageRequest();
        request.setPage(2);
        request.setSize(5);
        var page = favoriteService.list(request);

        assertEquals(6L, page.getTotal());
        assertEquals(List.of(7L), page.getList().stream().map(ProductResponse::getId).toList());
        verify(favoriteMapper).selectProductPage(any(), eq(11L));
    }

    @Test
    void engagementRejectsProductsThatAreNotPubliclyAvailable() {
        when(productMapper.selectById(7L)).thenReturn(product(7L, 0));

        assertThrows(CustomBusinessException.class, () -> favoriteService.add(7L));
        assertThrows(CustomBusinessException.class, () -> historyService.record(7L));

        verify(favoriteMapper, never()).insert(any(ProductFavorite.class));
        verify(historyMapper, never()).insert(any(BrowseHistory.class));
    }

    @Test
    void repeatedBrowseIncrementsOneOwnedHistoryRow() {
        when(productMapper.selectById(7L)).thenReturn(product(7L, 1));
        BrowseHistory existing = new BrowseHistory();
        existing.setId(91L);
        existing.setUserId(11L);
        existing.setProductId(7L);
        when(historyMapper.selectActive(11L, 7L)).thenReturn(null, existing);
        when(historyMapper.insert(any(BrowseHistory.class))).thenReturn(1);
        when(historyMapper.incrementActive(11L, 7L)).thenReturn(1);

        assertTrue(historyService.record(7L));
        assertTrue(historyService.record(7L));

        ArgumentCaptor<BrowseHistory> inserted = ArgumentCaptor.forClass(BrowseHistory.class);
        verify(historyMapper).insert(inserted.capture());
        assertEquals(11L, inserted.getValue().getUserId());
        assertEquals(7L, inserted.getValue().getProductId());
        assertEquals(1, inserted.getValue().getViewCount());
        verify(historyMapper).incrementActive(11L, 7L);
    }

    @Test
    void historyListDeleteAndClearAreUserScoped() {
        BrowseHistoryResponse item = new BrowseHistoryResponse();
        item.setId(7L);
        item.setLastViewedAt(LocalDateTime.of(2026, 7, 24, 20, 0));
        Page<BrowseHistoryResponse> result = Page.of(1, 10);
        result.setRecords(List.of(item));
        result.setTotal(1);
        when(historyMapper.selectProductPage(any(), eq(11L))).thenReturn(result);
        when(historyMapper.logicalDeleteActive(11L, 7L)).thenReturn(1);
        when(historyMapper.logicalDeleteAll(11L)).thenReturn(1);

        PageRequest request = new PageRequest();
        request.setSize(10);
        assertEquals(1L, historyService.list(request).getTotal());
        assertTrue(historyService.remove(7L));
        assertTrue(historyService.clear());

        verify(historyMapper).selectProductPage(any(), eq(11L));
        verify(historyMapper).logicalDeleteActive(11L, 7L);
        verify(historyMapper).logicalDeleteAll(11L);
    }

    private Product product(Long id, int onSale) {
        Product product = new Product();
        product.setId(id);
        product.setOnSale(onSale);
        return product;
    }
}
