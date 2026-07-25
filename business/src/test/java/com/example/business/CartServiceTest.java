package com.example.business;

import com.example.business.entity.Cart;
import com.example.business.entity.Product;
import com.example.business.entity.ProductSku;
import com.example.business.mapper.CartMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.request.CartAddRequest;
import com.example.business.request.CartUpdateRequest;
import com.example.business.service.impl.CartServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CartServiceImpl.class)
class CartServiceTest {
    @MockBean
    private CartMapper cartMapper;

    @MockBean
    private ProductSkuMapper productSkuMapper;

    @MockBean
    private ProductMapper productMapper;

    @Autowired
    private CartServiceImpl service;

    @BeforeEach
    void setUp() {
        UserContext.set(11L);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void addRejectsNonPositiveQuantity() {
        CartAddRequest request = new CartAddRequest();
        request.setProductSkuId(70L);
        request.setQuantity(0);

        assertThrows(CustomValidationException.class, () -> service.add(request));
    }

    @Test
    void updateRejectsRequestWithoutAnyChange() {
        CartUpdateRequest request = new CartUpdateRequest();
        request.setId(90L);

        assertThrows(CustomValidationException.class, () -> service.update(request));
    }

    @Test
    void repeatedAddAccumulatesQuantityForTheSameUserAndSku() {
        when(productSkuMapper.selectById(70L)).thenReturn(sku(70L, 7L, "12.00", 10));
        when(productMapper.selectById(7L)).thenReturn(product(7L, 1));
        Cart existing = cart(90L, 11L, 70L, 2, true);
        when(cartMapper.selectOne(any())).thenReturn(null, existing);
        when(cartMapper.insert(any(Cart.class))).thenAnswer(invocation -> {
            invocation.<Cart>getArgument(0).setId(90L);
            return 1;
        });
        when(cartMapper.updateById(any(Cart.class))).thenReturn(1);

        assertTrue(service.add(addRequest(70L, 2)));
        assertTrue(service.add(addRequest(70L, 3)));

        ArgumentCaptor<Cart> updated = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).updateById(updated.capture());
        assertEquals(5, updated.getValue().getQuantity());
        verify(cartMapper, times(1)).insert(any(Cart.class));
    }

    @Test
    void addRejectsQuantityThatWouldExceedCurrentStock() {
        when(productSkuMapper.selectById(70L)).thenReturn(sku(70L, 7L, "12.00", 5));
        when(productMapper.selectById(7L)).thenReturn(product(7L, 1));
        when(cartMapper.selectOne(any())).thenReturn(cart(90L, 11L, 70L, 4, true));

        assertThrows(CustomValidationException.class,
                () -> service.add(addRequest(70L, 2)));
        verify(cartMapper, never()).updateById(any(Cart.class));
    }

    @Test
    void addRejectsMissingSkuAndOffSaleProduct() {
        when(productSkuMapper.selectById(70L)).thenReturn(null);
        assertThrows(CustomBusinessException.class,
                () -> service.add(addRequest(70L, 1)));

        when(productSkuMapper.selectById(70L)).thenReturn(sku(70L, 7L, "12.00", 5));
        when(productMapper.selectById(7L)).thenReturn(product(7L, 0));
        assertThrows(CustomBusinessException.class,
                () -> service.add(addRequest(70L, 1)));
        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    void updateChangesOwnedQuantityAndCheckedState() {
        when(productSkuMapper.selectById(70L)).thenReturn(sku(70L, 7L, "12.00", 10));
        when(productMapper.selectById(7L)).thenReturn(product(7L, 1));
        when(cartMapper.selectOne(any())).thenReturn(cart(90L, 11L, 70L, 2, true));
        when(cartMapper.updateById(any(Cart.class))).thenReturn(1);

        CartUpdateRequest request = new CartUpdateRequest();
        request.setId(90L);
        request.setQuantity(4);
        request.setChecked(false);

        assertTrue(service.update(request));

        ArgumentCaptor<Cart> updated = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).updateById(updated.capture());
        assertEquals(4, updated.getValue().getQuantity());
        assertEquals(Boolean.FALSE, updated.getValue().getChecked());
    }

    @Test
    void updateRejectsQuantityAboveLiveStock() {
        when(productSkuMapper.selectById(70L)).thenReturn(sku(70L, 7L, "12.00", 3));
        when(productMapper.selectById(7L)).thenReturn(product(7L, 1));
        when(cartMapper.selectOne(any())).thenReturn(cart(90L, 11L, 70L, 2, true));

        CartUpdateRequest request = new CartUpdateRequest();
        request.setId(90L);
        request.setQuantity(4);

        assertThrows(CustomValidationException.class, () -> service.update(request));
        verify(cartMapper, never()).updateById(any(Cart.class));
    }

    @Test
    void updateRejectsNonPositiveQuantityAtServiceBoundary() {
        CartUpdateRequest request = new CartUpdateRequest();
        request.setId(90L);
        request.setQuantity(0);

        assertThrows(CustomValidationException.class, () -> service.update(request));
        verify(cartMapper, never()).selectOne(any());
        verify(cartMapper, never()).updateById(any(Cart.class));
    }

    @Test
    void updateCanUncheckAnItemAfterItsSkuBecomesUnavailable() {
        when(cartMapper.selectOne(any())).thenReturn(cart(90L, 11L, 70L, 2, true));
        when(cartMapper.updateById(any(Cart.class))).thenReturn(1);
        CartUpdateRequest request = new CartUpdateRequest();
        request.setId(90L);
        request.setChecked(false);

        assertTrue(service.update(request));

        verify(productSkuMapper, never()).selectById(any());
    }

    @Test
    void updateAndDeleteRejectCartItemOwnedByAnotherUser() {
        when(cartMapper.selectOne(any())).thenReturn(null);

        CartUpdateRequest request = new CartUpdateRequest();
        request.setId(90L);
        request.setChecked(false);

        assertThrows(CustomValidationException.class, () -> service.update(request));
        assertThrows(CustomValidationException.class, () -> service.delete(90L));
        verify(cartMapper, never()).updateById(any(Cart.class));
        verify(cartMapper, never()).logicalDeleteOwned(any(Long.class), any(Long.class));
    }

    @Test
    void deleteRemovesOwnedItemWithUserScopedMapperCall() {
        when(cartMapper.selectOne(any())).thenReturn(cart(90L, 11L, 70L, 2, true));
        when(cartMapper.logicalDeleteOwned(eq(11L), eq(90L))).thenReturn(1);

        assertTrue(service.delete(90L));

        verify(cartMapper).logicalDeleteOwned(11L, 90L);
    }

    @Test
    void getCartMarksInvalidRowsAndExcludesThemFromCheckedTotals() {
        when(cartMapper.selectList(any())).thenReturn(List.of(
                cart(90L, 11L, 70L, 2, true),
                cart(91L, 11L, 71L, 3, true),
                cart(92L, 11L, 72L, 4, true),
                cart(93L, 11L, 73L, 1, false),
                cart(94L, 11L, 74L, 1, true)));
        when(productSkuMapper.selectByIds(any())).thenReturn(List.of(
                sku(70L, 7L, "12.00", 5),
                sku(71L, 8L, "20.00", 10),
                sku(72L, 9L, "9.00", 2),
                sku(73L, 10L, "5.00", 5)));
        when(productMapper.selectByIds(any())).thenReturn(List.of(
                product(7L, 1),
                product(8L, 0),
                product(9L, 1),
                product(10L, 1)));

        var view = service.getCart();

        assertEquals(5, view.getItems().size());
        assertEquals(new BigDecimal("24.00"), view.getCheckedAmount());
        assertEquals(2, view.getCheckedQuantity());
        assertEquals(Boolean.FALSE, view.getItems().get(0).getInvalid());
        assertEquals(Boolean.TRUE, view.getItems().get(1).getInvalid());
        assertEquals(Boolean.TRUE, view.getItems().get(2).getInvalid());
        assertEquals(Boolean.FALSE, view.getItems().get(3).getInvalid());
        assertEquals(Boolean.TRUE, view.getItems().get(4).getInvalid());
        assertEquals("商品 7", view.getItems().get(0).getProductTitle());
        assertEquals("product-7.png", view.getItems().get(0).getProductImage());
        assertEquals("SKU 70", view.getItems().get(0).getSkuTitle());
        assertEquals(new BigDecimal("12.00"), view.getItems().get(0).getPrice());
        assertEquals(5, view.getItems().get(0).getStock());
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchDeleteDeduplicatesIdsAndDeletesOnlyOwnedRows() {
        when(cartMapper.selectList(any())).thenReturn(List.of(
                cart(90L, 11L, 70L, 2, true),
                cart(91L, 11L, 71L, 1, true)));
        when(cartMapper.logicalDeleteBatch(eq(11L), any())).thenReturn(2);

        assertTrue(service.deleteBatch(List.of(90L, 91L, 90L)));

        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);
        verify(cartMapper).logicalDeleteBatch(eq(11L), ids.capture());
        assertEquals(List.of(90L, 91L), ids.getValue());
    }

    @Test
    void batchDeleteRejectsWholeRequestWhenAnyItemIsNotOwned() {
        when(cartMapper.selectList(any())).thenReturn(List.of(
                cart(90L, 11L, 70L, 2, true)));

        assertThrows(CustomValidationException.class,
                () -> service.deleteBatch(List.of(90L, 99L)));
        verify(cartMapper, never()).logicalDeleteBatch(any(Long.class), any());
    }

    @Test
    void batchDeleteRejectsEmptyInput() {
        assertThrows(CustomValidationException.class,
                () -> service.deleteBatch(List.of()));
    }

    @Test
    void clearIsUserScopedAndIdempotentForEmptyCart() {
        when(cartMapper.logicalDeleteAll(11L)).thenReturn(0);

        assertTrue(service.clear());

        verify(cartMapper).logicalDeleteAll(11L);
    }

    private CartAddRequest addRequest(Long productSkuId, int quantity) {
        CartAddRequest request = new CartAddRequest();
        request.setProductSkuId(productSkuId);
        request.setQuantity(quantity);
        return request;
    }

    private ProductSku sku(Long id, Long productId, String price, int stock) {
        ProductSku sku = new ProductSku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setTitle("SKU " + id);
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        return sku;
    }

    private Product product(Long id, int onSale) {
        Product product = new Product();
        product.setId(id);
        product.setTitle("商品 " + id);
        product.setImage("product-" + id + ".png");
        product.setOnSale(onSale);
        return product;
    }

    private Cart cart(Long id, Long userId, Long productSkuId, int quantity, boolean checked) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(userId);
        cart.setProductSkuId(productSkuId);
        cart.setQuantity(quantity);
        cart.setChecked(checked);
        return cart;
    }
}
