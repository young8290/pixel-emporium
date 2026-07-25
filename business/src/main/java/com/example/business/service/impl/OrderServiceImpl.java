package com.example.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.example.business.model.OrderAddressSnapshot;
import com.example.business.request.OrderCreateRequest;
import com.example.business.request.OrderAdminListRequest;
import com.example.business.request.OrderListRequest;
import com.example.business.request.OrderShipRequest;
import com.example.business.response.OrderAdminDetailResponse;
import com.example.business.response.OrderAdminResponse;
import com.example.business.response.OrderDetailResponse;
import com.example.business.response.OrderItemResponse;
import com.example.business.response.OrderResponse;
import com.example.business.service.OrderService;
import com.example.business.service.ProductSkuService;
import com.example.business.service.NotificationOutboxService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal ZERO_FREIGHT = new BigDecimal("0.00");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final ProductSkuService productSkuService;
    private final IdentifierGenerator identifierGenerator;

    @Autowired
    private NotificationOutboxService notificationOutboxService;

    @Value("${order.timeout-minutes:30}")
    private long timeoutMinutes = 30;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            CartMapper cartMapper,
                            ProductSkuMapper productSkuMapper,
                            ProductMapper productMapper,
                            ProductSkuService productSkuService,
                            IdentifierGenerator identifierGenerator) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.productSkuMapper = productSkuMapper;
        this.productMapper = productMapper;
        this.productSkuService = productSkuService;
        this.identifierGenerator = identifierGenerator;
    }

    @Override
    @Transactional
    public String create(OrderCreateRequest request) {
        Long userId = requireUserId();
        SourceSelection selection = validateSource(request);
        OrderAddressSnapshot address = loadAddress(request.getAddressId(), userId);
        List<RequestedLine> requestedLines = loadRequestedLines(selection, userId);
        List<OrderLine> lines = loadOrderLines(requestedLines);

        BigDecimal totalAmount = lines.stream()
                .map(line -> line.sku().getPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 先建单确定 orderNo，再以 orderNo 作为库存扣减的幂等业务单号（BE-P2-03）
        Order order = buildOrder(userId, request, address, totalAmount);

        for (OrderLine line : lines) {
            productSkuService.deductStock(line.sku().getId(), line.quantity(),
                    order.getOrderNo(), userId);
        }

        if (orderMapper.insert(order) != 1) {
            throw new CustomBusinessException("订单保存失败");
        }
        for (OrderLine line : lines) {
            if (orderItemMapper.insert(toOrderItem(order.getId(), line)) != 1) {
                throw new CustomBusinessException("订单项保存失败");
            }
        }
        if (selection.cartSource()) {
            List<Long> cartIds = lines.stream()
                    .map(OrderLine::cartId)
                    .sorted()
                    .toList();
            if (cartMapper.logicalDeleteBatch(userId, cartIds) != cartIds.size()) {
                throw new CustomBusinessException("已购购物车项清理失败");
            }
        }
        notificationOutboxService.appendOrderCreated(order.getId());
        return order.getOrderNo();
    }

    @Override
    public PageResponse<OrderResponse> list(OrderListRequest request) {
        Long userId = requireUserId();
        OrderListRequest query = request == null ? new OrderListRequest() : request;
        Page<Order> selected = orderMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                        .orderByDesc(Order::getCreatedAt)
                        .orderByDesc(Order::getId));
        List<Order> orders = selected.getRecords() == null ? List.of() : selected.getRecords();
        Map<Long, List<OrderItem>> itemsByOrder = loadItemsByOrder(orders.stream()
                .map(Order::getId)
                .toList());
        PageResponse<OrderResponse> response = new PageResponse<>();
        response.setTotal(selected.getTotal());
        response.setList(orders.stream()
                .map(order -> toOrderResponse(order,
                        itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList());
        return response;
    }

    @Override
    public PageResponse<OrderAdminResponse> adminList(OrderAdminListRequest request) {
        OrderAdminListRequest query = request == null ? new OrderAdminListRequest() : request;
        if (query.getPage() == null || query.getSize() == null) {
            throw new CustomValidationException("分页参数不能为空");
        }
        if (query.getCreatedAtStart() != null && query.getCreatedAtEnd() != null
                && query.getCreatedAtStart().isAfter(query.getCreatedAtEnd())) {
            throw new CustomValidationException("开始时间不能晚于结束时间");
        }
        String orderNo = StringUtils.hasText(query.getOrderNo()) ? query.getOrderNo().trim() : null;
        Page<Order> selected = orderMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Order>()
                        .like(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                        .eq(query.getUserId() != null, Order::getUserId, query.getUserId())
                        .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                        .ge(query.getCreatedAtStart() != null,
                                Order::getCreatedAt, query.getCreatedAtStart())
                        .le(query.getCreatedAtEnd() != null,
                                Order::getCreatedAt, query.getCreatedAtEnd())
                        .orderByDesc(Order::getCreatedAt)
                        .orderByDesc(Order::getId));
        List<Order> orders = selected.getRecords() == null ? List.of() : selected.getRecords();
        PageResponse<OrderAdminResponse> response = new PageResponse<>();
        response.setTotal(selected.getTotal());
        response.setList(orders.stream().map(this::toOrderAdminResponse).toList());
        return response;
    }

    @Override
    public OrderDetailResponse detail(Long id) {
        if (id == null) {
            throw new CustomValidationException("订单ID不能为空");
        }
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new NoSuchElementException("订单不存在");
        }
        requireOwned(order, requireUserId());
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        OrderDetailResponse response = new OrderDetailResponse();
        copyOrderHeader(order, response);
        response.setItems(items.stream().map(this::toOrderItemResponse).toList());
        response.setReceiverName(order.getReceiverName());
        response.setReceiverPhone(order.getReceiverPhone());
        response.setReceiverProvince(order.getReceiverProvince());
        response.setReceiverCity(order.getReceiverCity());
        response.setReceiverDistrict(order.getReceiverDistrict());
        response.setReceiverDetail(order.getReceiverDetail());
        response.setRemark(order.getRemark());
        return response;
    }

    @Override
    public OrderAdminDetailResponse adminDetail(Long id) {
        if (id == null) {
            throw new CustomValidationException("订单ID不能为空");
        }
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new NoSuchElementException("订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id)
                .orderByAsc(OrderItem::getId));
        OrderAdminDetailResponse response = new OrderAdminDetailResponse();
        copyOrderAdminHeader(order, response);
        response.setReceiverName(order.getReceiverName());
        response.setReceiverPhone(order.getReceiverPhone());
        response.setReceiverProvince(order.getReceiverProvince());
        response.setReceiverCity(order.getReceiverCity());
        response.setReceiverDistrict(order.getReceiverDistrict());
        response.setReceiverDetail(order.getReceiverDetail());
        response.setRemark(order.getRemark());
        response.setItems((items == null ? List.<OrderItem>of() : items).stream()
                .map(this::toOrderItemResponse).toList());
        return response;
    }

    @Override
    @Transactional
    public boolean cancel(Long id) {
        Order order = loadLockedOrder(id);
        requireOwned(order, requireUserId());
        cancelLocked(order);
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(Long id) {
        Order order = loadLockedOrder(id);
        requireOwned(order, requireUserId());
        requireTransfer(order, OrderStatus.COMPLETED);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        updateState(order);
        return true;
    }

    @Override
    @Transactional
    public boolean ship(Long id, OrderShipRequest request) {
        Order order = loadLockedOrder(id);
        requireTransfer(order, OrderStatus.SHIPPED);
        order.setShippingCarrier(request.getShippingCarrier().trim());
        order.setTrackingNumber(request.getTrackingNumber().trim());
        order.setShippingNote(StringUtils.hasText(request.getShippingNote())
                ? request.getShippingNote().trim() : null);
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        updateState(order);
        return true;
    }

    @Override
    @Transactional
    public boolean markPaid(Long id, LocalDateTime paidAt) {
        if (paidAt == null) {
            throw new CustomValidationException("支付时间不能为空");
        }
        Order order = loadLockedOrder(id);
        requireTransfer(order, OrderStatus.PAID);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(paidAt);
        updateState(order);
        productMapper.incrementSoldCountByOrderId(order.getId());
        return true;
    }

    @Override
    @Transactional
    public int cancelTimeoutOrders() {
        List<Long> ids = orderMapper.selectTimeoutIds(
                LocalDateTime.now().minusMinutes(timeoutMinutes), 100);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int canceled = 0;
        for (Long id : ids) {
            Order order = orderMapper.selectByIdForUpdate(id);
            if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                continue;
            }
            cancelLocked(order);
            canceled++;
        }
        return canceled;
    }

    private SourceSelection validateSource(OrderCreateRequest request) {
        if (request == null || request.getAddressId() == null || !StringUtils.hasText(request.getSource())) {
            throw new CustomValidationException("下单参数不完整");
        }
        if ("cart".equals(request.getSource())) {
            if (request.getSkuId() != null || request.getQuantity() != null) {
                throw new CustomValidationException("购物车下单不能携带立即购买参数");
            }
            List<Long> ids = request.getCartItemIds();
            if (ids != null) {
                if (ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
                    throw new CustomValidationException("购物车项ID不能为空");
                }
                Set<Long> unique = new LinkedHashSet<>(ids);
                if (unique.size() != ids.size()) {
                    throw new CustomValidationException("购物车项ID不能重复");
                }
                return new SourceSelection(true, unique.stream().sorted().toList(), null, null);
            }
            return new SourceSelection(true, null, null, null);
        }
        if ("buyNow".equals(request.getSource())) {
            if (request.getSkuId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new CustomValidationException("立即购买必须提供SKU和正整数数量");
            }
            if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
                throw new CustomValidationException("立即购买不能携带购物车项");
            }
            return new SourceSelection(false, null, request.getSkuId(), request.getQuantity());
        }
        throw new CustomValidationException("下单来源不支持");
    }

    private OrderAddressSnapshot loadAddress(Long addressId, Long userId) {
        OrderAddressSnapshot address = orderMapper.selectAddressById(addressId);
        if (address == null) {
            throw new NoSuchElementException("收货地址不存在");
        }
        if (!Objects.equals(address.getUserId(), userId)) {
            throw new CustomForbiddenException("无权使用该收货地址");
        }
        if (!StringUtils.hasText(address.getRecipient())
                || !StringUtils.hasText(address.getPhone())
                || !StringUtils.hasText(address.getProvince())
                || !StringUtils.hasText(address.getCity())
                || !StringUtils.hasText(address.getDistrict())
                || !StringUtils.hasText(address.getDetail())) {
            throw new CustomValidationException("收货地址信息不完整");
        }
        return address;
    }

    private List<RequestedLine> loadRequestedLines(SourceSelection selection, Long userId) {
        if (!selection.cartSource()) {
            return List.of(new RequestedLine(null, selection.skuId(), selection.quantity()));
        }
        List<Cart> carts;
        if (selection.cartIds() == null) {
            carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                    .eq(Cart::getUserId, userId)
                    .eq(Cart::getChecked, true)
                    .orderByAsc(Cart::getId));
        } else {
            carts = cartMapper.selectByIds(selection.cartIds());
            if (carts.stream().anyMatch(cart -> !Objects.equals(cart.getUserId(), userId))) {
                throw new CustomForbiddenException("购物车项不属于当前用户");
            }
            Set<Long> foundIds = carts.stream().map(Cart::getId).collect(Collectors.toSet());
            if (carts.size() != selection.cartIds().size()
                    || !foundIds.containsAll(selection.cartIds())) {
                throw new CustomValidationException("购物车项不存在");
            }
            if (carts.stream().anyMatch(cart -> !Boolean.TRUE.equals(cart.getChecked()))) {
                throw new CustomValidationException("购物车项未勾选");
            }
        }
        if (carts.isEmpty()) {
            throw new CustomValidationException("没有可下单的购物车项");
        }
        return carts.stream()
                .map(cart -> new RequestedLine(cart.getId(), cart.getProductSkuId(), cart.getQuantity()))
                .toList();
    }

    private List<OrderLine> loadOrderLines(List<RequestedLine> requestedLines) {
        Set<Long> skuIds = requestedLines.stream()
                .map(RequestedLine::skuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ProductSku> skuById = indexById(productSkuMapper.selectByIds(skuIds), ProductSku::getId);
        if (skuById.size() != skuIds.size()) {
            throw new NoSuchElementException("商品SKU不存在");
        }
        Set<Long> productIds = skuById.values().stream()
                .map(ProductSku::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Product> productById = indexById(productMapper.selectByIds(productIds), Product::getId);
        if (productById.size() != productIds.size()) {
            throw new NoSuchElementException("商品不存在");
        }

        List<OrderLine> lines = new ArrayList<>();
        for (RequestedLine requested : requestedLines) {
            if (requested.skuId() == null || requested.quantity() == null || requested.quantity() <= 0) {
                throw new CustomValidationException("商品数量必须大于0");
            }
            ProductSku sku = skuById.get(requested.skuId());
            if (sku == null || sku.getProductId() == null) {
                throw new NoSuchElementException("商品SKU不存在");
            }
            Product product = productById.get(sku.getProductId());
            if (product == null) {
                throw new NoSuchElementException("商品不存在");
            }
            if (!Integer.valueOf(1).equals(product.getOnSale())) {
                throw new CustomValidationException("商品已下架");
            }
            if (sku.getPrice() == null || sku.getPrice().signum() < 0) {
                throw new CustomValidationException("商品价格不可用");
            }
            lines.add(new OrderLine(requested.cartId(), requested.quantity(), sku, product));
        }
        lines.sort(Comparator.comparing(line -> line.sku().getId()));
        return lines;
    }

    private <T> Map<Long, T> indexById(Collection<T> rows, Function<T, Long> idGetter) {
        if (rows == null) {
            return Map.of();
        }
        return rows.stream().collect(Collectors.toMap(idGetter, Function.identity()));
    }

    private Order buildOrder(Long userId,
                             OrderCreateRequest request,
                             OrderAddressSnapshot address,
                             BigDecimal totalAmount) {
        Order order = new Order();
        Number generatedId = identifierGenerator.nextId(order);
        if (generatedId == null) {
            throw new CustomBusinessException("订单ID生成失败");
        }
        order.setId(generatedId.longValue());
        order.setOrderNo(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + order.getId());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setFreightAmount(ZERO_FREIGHT);
        order.setReceiverName(address.getRecipient());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverDetail(address.getDetail());
        order.setRemark(request.getRemark());
        return order;
    }

    private OrderItem toOrderItem(Long orderId, OrderLine line) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(line.product().getId());
        item.setProductSkuId(line.sku().getId());
        item.setProductTitle(line.product().getTitle());
        item.setSkuTitle(line.sku().getTitle());
        item.setImage(line.product().getImage());
        item.setPrice(line.sku().getPrice());
        item.setQuantity(line.quantity());
        return item;
    }

    private Map<Long, List<OrderItem>> loadItemsByOrder(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
                        .orderByAsc(OrderItem::getOrderId)
                        .orderByAsc(OrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        OrderResponse response = new OrderResponse();
        copyOrderHeader(order, response);
        response.setItems(items.stream().map(this::toOrderItemResponse).toList());
        return response;
    }

    private OrderAdminResponse toOrderAdminResponse(Order order) {
        OrderAdminResponse response = new OrderAdminResponse();
        copyOrderAdminHeader(order, response);
        return response;
    }

    private void copyOrderAdminHeader(Order order, OrderAdminResponse response) {
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setPayAmount(order.getPayAmount());
        response.setFreightAmount(order.getFreightAmount());
        response.setShippingCarrier(order.getShippingCarrier());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setShippingNote(order.getShippingNote());
        response.setPaidAt(order.getPaidAt());
        response.setShippedAt(order.getShippedAt());
        response.setCompletedAt(order.getCompletedAt());
        response.setCanceledAt(order.getCanceledAt());
        response.setCreatedAt(order.getCreatedAt());
    }

    private void copyOrderHeader(Order order, OrderResponse response) {
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setPayAmount(order.getPayAmount());
        response.setFreightAmount(order.getFreightAmount());
        response.setShippingCarrier(order.getShippingCarrier());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setShippingNote(order.getShippingNote());
        response.setPaidAt(order.getPaidAt());
        response.setShippedAt(order.getShippedAt());
        response.setCompletedAt(order.getCompletedAt());
        response.setCanceledAt(order.getCanceledAt());
        response.setCreatedAt(order.getCreatedAt());
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setProductSkuId(item.getProductSkuId());
        response.setProductTitle(item.getProductTitle());
        response.setSkuTitle(item.getSkuTitle());
        response.setImage(item.getImage());
        response.setPrice(item.getPrice());
        response.setQuantity(item.getQuantity());
        return response;
    }

    private Order loadLockedOrder(Long id) {
        if (id == null) {
            throw new CustomValidationException("订单ID不能为空");
        }
        Order order = orderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new NoSuchElementException("订单不存在");
        }
        return order;
    }

    private void requireOwned(Order order, Long userId) {
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new CustomForbiddenException("无权操作该订单");
        }
    }

    private void requireTransfer(Order order, OrderStatus target) {
        if (!OrderStatus.canTransfer(order.getStatus(), target)) {
            throw new CustomConflictException("当前订单状态不允许此操作");
        }
    }

    private void cancelLocked(Order order) {
        requireTransfer(order, OrderStatus.CANCELED);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        if (items == null || items.isEmpty()) {
            throw new NoSuchElementException("订单项不存在");
        }
        items.stream()
                .sorted(Comparator.comparing(OrderItem::getProductSkuId))
                .forEach(item -> productSkuService.restoreStock(
                        item.getProductSkuId(), item.getQuantity(),
                        order.getOrderNo(), order.getUserId()));
        order.setStatus(OrderStatus.CANCELED);
        order.setCanceledAt(LocalDateTime.now());
        updateState(order);
    }

    private void updateState(Order order) {
        if (orderMapper.updateById(order) != 1) {
            throw new CustomBusinessException("订单状态更新失败");
        }
    }

    private Long requireUserId() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new CustomValidationException("请先登录");
        }
        return userId;
    }

    private record SourceSelection(boolean cartSource,
                                   List<Long> cartIds,
                                   Long skuId,
                                   Integer quantity) {
    }

    private record RequestedLine(Long cartId, Long skuId, Integer quantity) {
    }

    private record OrderLine(Long cartId, Integer quantity, ProductSku sku, Product product) {
    }
}
