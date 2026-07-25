package com.example.business;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.example.business.entity.Cart;
import com.example.business.entity.Order;
import com.example.business.entity.OrderItem;
import com.example.business.entity.Product;
import com.example.business.entity.ProductSku;
import com.example.business.enums.OrderStatus;
import com.example.business.mapper.CartMapper;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.request.OrderCreateRequest;
import com.example.business.response.OrderDetailResponse;
import com.example.business.service.OrderService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("mysql-integration")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "alibaba.cloud.oss.access-key=test-access-key",
        "alibaba.cloud.oss.secret-key=test-secret-key",
        "order.timeout-scan-interval-ms=3600000"
})
class OrderCreateTest {
    @Autowired private OrderService orderService;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;
    @Autowired private CartMapper cartMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private ProductSkuMapper productSkuMapper;
    @Autowired private IdentifierGenerator identifierGenerator;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<Long> userIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> addressIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> cartIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> skuIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> productIds = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void createSharedAddressTableForBusinessTestDatabase() {
        jdbcTemplate.execute("""
                create table if not exists `address`
                (
                    `id` bigint not null,
                    `user_id` bigint not null,
                    `recipient` varchar(50) not null,
                    `phone` varchar(20) not null,
                    `province` varchar(50) not null,
                    `city` varchar(50) not null,
                    `district` varchar(50) not null,
                    `detail` varchar(255) not null,
                    `deleted` bigint not null default 0,
                    primary key (`id`)
                )
                """);
    }

    @AfterEach
    void cleanOnlyRowsCreatedByThisTest() {
        UserContext.remove();
        for (Long userId : userIds) {
            List<Long> orderIds = jdbcTemplate.queryForList(
                    "select id from `order` where user_id = ?", Long.class, userId);
            for (Long orderId : orderIds) {
                jdbcTemplate.update("delete from order_item where order_id = ?", orderId);
                jdbcTemplate.update("delete from `order` where id = ?", orderId);
            }
        }
        for (Long id : cartIds) {
            jdbcTemplate.update("delete from cart where id = ?", id);
        }
        for (Long id : skuIds) {
            jdbcTemplate.update("delete from product_sku where id = ?", id);
        }
        for (Long id : productIds) {
            jdbcTemplate.update("delete from product where id = ?", id);
        }
        for (Long id : addressIds) {
            jdbcTemplate.update("delete from address where id = ?", id);
        }
        userIds.clear();
        addressIds.clear();
        cartIds.clear();
        skuIds.clear();
        productIds.clear();
    }

    @Test
    void flywayV8AndCartOrderPersistServerSnapshotsAndExactStock() {
        assertEquals(2, jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                 where table_schema = database()
                   and table_name in ('order', 'order_item')
                """, Integer.class));
        TestUser user = createUserAddress();
        Catalog catalog = createCatalog(5, "12.00");
        Cart cart = createCart(user.userId(), catalog.skuId(), 2);

        String orderNo = orderService.create(cartRequest(user.addressId(), List.of(cart.getId())));

        Order saved = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, saved.getId()));
        assertEquals(OrderStatus.PENDING_PAYMENT, saved.getStatus());
        assertEquals(new BigDecimal("24.00"), saved.getTotalAmount());
        assertEquals(new BigDecimal("24.00"), saved.getPayAmount());
        assertEquals(new BigDecimal("0.00"), saved.getFreightAmount());
        assertEquals(3, productSkuMapper.selectById(catalog.skuId()).getStock());
        assertEquals(1, items.size());
        assertEquals("测试商品", items.getFirst().getProductTitle());
        assertEquals(new BigDecimal("12.00"), items.getFirst().getPrice());
        assertNull(cartMapper.selectById(cart.getId()));

        Product product = productMapper.selectById(catalog.productId());
        product.setTitle("改名后商品");
        productMapper.updateById(product);
        ProductSku sku = productSkuMapper.selectById(catalog.skuId());
        sku.setPrice(new BigDecimal("99.00"));
        productSkuMapper.updateById(sku);
        OrderDetailResponse detail = orderService.detail(saved.getId());
        assertEquals("测试商品", detail.getItems().getFirst().getProductTitle());
        assertEquals(new BigDecimal("12.00"), detail.getItems().getFirst().getPrice());
    }

    @Test
    void secondSkuFailureRollsBackFirstDeductionOrdersItemsAndCartDeletion() {
        TestUser user = createUserAddress();
        Catalog available = createCatalog(3, "10.00");
        Catalog empty = createCatalog(0, "20.00");
        Cart first = createCart(user.userId(), available.skuId(), 2);
        Cart second = createCart(user.userId(), empty.skuId(), 1);

        assertThrows(CustomBusinessException.class, () -> orderService.create(
                cartRequest(user.addressId(), List.of(first.getId(), second.getId()))));

        assertEquals(3, productSkuMapper.selectById(available.skuId()).getStock());
        assertEquals(0, productSkuMapper.selectById(empty.skuId()).getStock());
        assertEquals(0L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, user.userId())));
        assertTrue(cartMapper.selectById(first.getId()) != null);
        assertTrue(cartMapper.selectById(second.getId()) != null);
    }

    @Test
    void cancelRestoresOnceAndConfirmOnlyAcceptsShipped() {
        TestUser user = createUserAddress();
        Catalog catalog = createCatalog(5, "12.00");
        String cancelNo = orderService.create(buyNowRequest(user.addressId(), catalog.skuId(), 2));
        Order cancelOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, cancelNo));

        assertTrue(orderService.cancel(cancelOrder.getId()));
        assertEquals(5, productSkuMapper.selectById(catalog.skuId()).getStock());
        assertThrows(CustomConflictException.class, () -> orderService.cancel(cancelOrder.getId()));
        assertEquals(5, productSkuMapper.selectById(catalog.skuId()).getStock());

        String confirmNo = orderService.create(buyNowRequest(user.addressId(), catalog.skuId(), 1));
        Order confirmOrder = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, confirmNo));
        assertThrows(CustomConflictException.class, () -> orderService.confirm(confirmOrder.getId()));
        confirmOrder.setStatus(OrderStatus.SHIPPED);
        confirmOrder.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(confirmOrder);

        assertTrue(orderService.confirm(confirmOrder.getId()));
        assertEquals(OrderStatus.COMPLETED,
                orderMapper.selectById(confirmOrder.getId()).getStatus());
    }

    @Test
    void foreignUserCannotReadOrCancelOrder() {
        TestUser owner = createUserAddress();
        Catalog catalog = createCatalog(3, "8.00");
        String orderNo = orderService.create(buyNowRequest(owner.addressId(), catalog.skuId(), 1));
        Order saved = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        TestUser other = createUserAddress();
        UserContext.set(other.userId());

        assertThrows(CustomForbiddenException.class, () -> orderService.detail(saved.getId()));
        assertThrows(CustomForbiddenException.class, () -> orderService.cancel(saved.getId()));
    }

    @Test
    void concurrentBuyNowNeverOversellsScarceSku() throws Exception {
        TestUser user = createUserAddress();
        Catalog catalog = createCatalog(5, "1.00");
        ExecutorService executor = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tasks.add(executor.submit(() -> {
                UserContext.set(user.userId());
                try {
                    start.await();
                    orderService.create(buyNowRequest(user.addressId(), catalog.skuId(), 1));
                    successes.incrementAndGet();
                } catch (CustomBusinessException ignored) {
                    // Expected after the five available units are reserved.
                } catch (Throwable error) {
                    unexpected.add(error);
                } finally {
                    UserContext.remove();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> task : tasks) {
            task.get();
        }
        executor.shutdown();

        assertTrue(unexpected.isEmpty(), () -> "unexpected failures: " + unexpected);
        assertEquals(5, successes.get());
        assertEquals(0, productSkuMapper.selectById(catalog.skuId()).getStock());
        assertEquals(5L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, user.userId())));
    }

    private TestUser createUserAddress() {
        Long userId = nextId();
        Long addressId = nextId();
        userIds.add(userId);
        addressIds.add(addressId);
        jdbcTemplate.update("""
                insert into address
                    (id, user_id, recipient, phone, province, city, district, detail, deleted)
                values (?, ?, '张三', '13800138000', '广东省', '深圳市', '南山区', '科技园1号', 0)
                """, addressId, userId);
        UserContext.set(userId);
        return new TestUser(userId, addressId);
    }

    private Catalog createCatalog(int stock, String price) {
        Product product = new Product();
        product.setId(nextId());
        product.setTitle("测试商品");
        product.setDescription("测试商品");
        product.setImage("snapshot.png");
        product.setOnSale(1);
        product.setPrice(new BigDecimal(price));
        productMapper.insert(product);
        productIds.add(product.getId());

        ProductSku sku = new ProductSku();
        sku.setId(nextId());
        sku.setProductId(product.getId());
        sku.setTitle("默认规格");
        sku.setDescription("默认规格");
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        productSkuMapper.insert(sku);
        skuIds.add(sku.getId());
        return new Catalog(product.getId(), sku.getId());
    }

    private Cart createCart(Long userId, Long skuId, int quantity) {
        Cart cart = new Cart();
        cart.setId(nextId());
        cart.setUserId(userId);
        cart.setProductSkuId(skuId);
        cart.setQuantity(quantity);
        cart.setChecked(true);
        cartMapper.insert(cart);
        cartIds.add(cart.getId());
        return cart;
    }

    private OrderCreateRequest cartRequest(Long addressId, List<Long> ids) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(addressId);
        request.setSource("cart");
        request.setCartItemIds(ids);
        return request;
    }

    private OrderCreateRequest buyNowRequest(Long addressId, Long skuId, int quantity) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(addressId);
        request.setSource("buyNow");
        request.setSkuId(skuId);
        request.setQuantity(quantity);
        return request;
    }

    private Long nextId() {
        return identifierGenerator.nextId(this).longValue();
    }

    private record TestUser(Long userId, Long addressId) {
    }

    private record Catalog(Long productId, Long skuId) {
    }
}
