package com.example.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.Order;
import com.example.business.entity.OrderItem;
import com.example.business.entity.Product;
import com.example.business.entity.ProductReview;
import com.example.business.enums.OrderStatus;
import com.example.business.enums.ProductReviewStatus;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductReviewMapper;
import com.example.business.model.ProductReviewAggregate;
import com.example.business.request.ProductReviewCreateRequest;
import com.example.business.request.ProductReviewAdminListRequest;
import com.example.business.request.ProductReviewModerateRequest;
import com.example.business.request.ProductReviewPublicListRequest;
import com.example.business.request.ProductReviewUpdateRequest;
import com.example.business.response.ProductReviewResponse;
import com.example.business.response.ProductReviewAdminResponse;
import com.example.business.response.ProductReviewSummaryResponse;
import com.example.business.response.OrderReviewEligibilityResponse;
import com.example.business.service.ProductReviewService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductReviewServiceImpl implements ProductReviewService {
    private final ProductReviewMapper reviewMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public ProductReviewServiceImpl(ProductReviewMapper reviewMapper,
                                    ProductMapper productMapper,
                                    OrderMapper orderMapper,
                                    OrderItemMapper orderItemMapper) {
        this.reviewMapper = reviewMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public PageResponse<ProductReviewResponse> publicList(
            Long productId, ProductReviewPublicListRequest request) {
        PublicListOptions options = validatePublicList(productId, request);
        requireOnSaleProduct(productId);

        LambdaQueryWrapper<ProductReview> query = new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getProductId, productId)
                .eq(ProductReview::getStatus, ProductReviewStatus.PUBLISHED)
                .eq(options.rating() != null, ProductReview::getRating, options.rating());
        applyPublicSort(query, options.sort());

        Page<ProductReview> page = reviewMapper.selectPage(
                Page.of(options.page(), options.size()), query);
        Map<Long, OrderItem> itemsById = loadOrderItems(page.getRecords());

        PageResponse<ProductReviewResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(page.getRecords().stream()
                .map(review -> toPublicResponse(review, itemsById.get(review.getOrderItemId())))
                .toList());
        return response;
    }

    @Override
    public ProductReviewSummaryResponse summary(Long productId) {
        if (productId == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
        requireOnSaleProduct(productId);
        ProductReviewAggregate aggregate = reviewMapper.selectAggregate(productId);

        ProductReviewSummaryResponse response = new ProductReviewSummaryResponse();
        response.setProductId(productId);
        response.setAverageRating(aggregate == null || aggregate.averageRating() == null
                ? BigDecimal.ZERO.setScale(2)
                : aggregate.averageRating().setScale(2, RoundingMode.HALF_UP));
        response.setReviewCount(aggregate == null || aggregate.count() == null
                ? 0L : aggregate.count());
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        distribution.put(5, aggregateValue(aggregate == null ? null : aggregate.star5()));
        distribution.put(4, aggregateValue(aggregate == null ? null : aggregate.star4()));
        distribution.put(3, aggregateValue(aggregate == null ? null : aggregate.star3()));
        distribution.put(2, aggregateValue(aggregate == null ? null : aggregate.star2()));
        distribution.put(1, aggregateValue(aggregate == null ? null : aggregate.star1()));
        response.setDistribution(distribution);
        return response;
    }

    @Override
    public OrderReviewEligibilityResponse orderEligibility(Long orderId) {
        Long userId = requireUserId();
        if (orderId == null) {
            throw new CustomValidationException("订单ID不能为空");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new NoSuchElementException("订单不存在");
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
        Map<Long, ProductReview> reviewsByItemId = orderItems.isEmpty()
                ? Map.of()
                : reviewMapper.selectList(new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getOrderId, orderId)
                        .eq(ProductReview::getUserId, userId)
                        .eq(ProductReview::getDeleted, 0L))
                .stream()
                .filter(review -> review.getOrderItemId() != null)
                .collect(Collectors.toMap(ProductReview::getOrderItemId, Function.identity(),
                        (left, ignored) -> left));

        boolean completed = order.getStatus() == OrderStatus.COMPLETED;
        OrderReviewEligibilityResponse response = new OrderReviewEligibilityResponse();
        response.setOrderId(order.getId());
        response.setOrderStatus(order.getStatus());
        response.setItems(orderItems.stream()
                .map(item -> toEligibilityItem(item, reviewsByItemId.get(item.getId()), completed))
                .toList());
        return response;
    }

    @Override
    public PageResponse<ProductReviewAdminResponse> adminList(ProductReviewAdminListRequest request) {
        ProductReviewAdminListRequest queryRequest = request == null
                ? new ProductReviewAdminListRequest() : request;
        validateAdminList(queryRequest);
        String keyword = StringUtils.hasText(queryRequest.getKeyword())
                ? queryRequest.getKeyword().trim() : null;
        LambdaQueryWrapper<ProductReview> query = new LambdaQueryWrapper<ProductReview>()
                .eq(queryRequest.getProductId() != null,
                        ProductReview::getProductId, queryRequest.getProductId())
                .eq(queryRequest.getUserId() != null,
                        ProductReview::getUserId, queryRequest.getUserId())
                .eq(queryRequest.getRating() != null,
                        ProductReview::getRating, queryRequest.getRating())
                .eq(queryRequest.getStatus() != null,
                        ProductReview::getStatus, queryRequest.getStatus())
                .like(StringUtils.hasText(keyword), ProductReview::getContent, keyword)
                .ge(queryRequest.getCreatedAtStart() != null,
                        ProductReview::getCreatedAt, queryRequest.getCreatedAtStart())
                .le(queryRequest.getCreatedAtEnd() != null,
                        ProductReview::getCreatedAt, queryRequest.getCreatedAtEnd())
                .orderByDesc(ProductReview::getCreatedAt)
                .orderByDesc(ProductReview::getId);
        Page<ProductReview> page = reviewMapper.selectPage(
                Page.of(queryRequest.getPage(), queryRequest.getSize()), query);
        Map<Long, OrderItem> itemsById = loadOrderItems(page.getRecords());

        PageResponse<ProductReviewAdminResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(page.getRecords().stream()
                .map(review -> toAdminResponse(review, itemsById.get(review.getOrderItemId())))
                .toList());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean hide(Long id, ProductReviewModerateRequest request) {
        Long adminId = requireUserId();
        String reason = validateModerationReason(request);
        ProductReview review = activeReview(id);
        if (review.getStatus() != ProductReviewStatus.PUBLISHED) {
            throw new CustomConflictException("评价已隐藏");
        }
        lockProduct(review.getProductId());
        if (reviewMapper.hidePublished(id, adminId, reason) != 1) {
            throw new CustomConflictException("评价状态已变化，请刷新后重试");
        }
        refreshAggregate(review.getProductId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restore(Long id) {
        Long adminId = requireUserId();
        ProductReview review = activeReview(id);
        if (review.getStatus() != ProductReviewStatus.HIDDEN) {
            throw new CustomConflictException("评价未隐藏");
        }
        lockProduct(review.getProductId());
        if (reviewMapper.restoreHidden(id, adminId) != 1) {
            throw new CustomConflictException("评价状态已变化，请刷新后重试");
        }
        refreshAggregate(review.getProductId());
        return true;
    }

    private OrderReviewEligibilityResponse.Item toEligibilityItem(
            OrderItem item, ProductReview review, boolean completed) {
        OrderReviewEligibilityResponse.Item response = new OrderReviewEligibilityResponse.Item();
        response.setOrderItemId(item.getId());
        response.setProductId(item.getProductId());
        response.setProductTitle(item.getProductTitle());
        response.setSkuTitle(item.getSkuTitle());
        if (review == null) {
            response.setEligible(completed);
            return response;
        }
        response.setReviewId(review.getId());
        response.setReviewStatus(review.getStatus());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setModerationReason(review.getModerationReason());
        response.setEligible(false);
        response.setEditable(review.getStatus() == ProductReviewStatus.PUBLISHED);
        response.setDeletable(review.getStatus() == ProductReviewStatus.PUBLISHED
                || review.getStatus() == ProductReviewStatus.HIDDEN);
        return response;
    }

    private void validateAdminList(ProductReviewAdminListRequest request) {
        if (request.getPage() == null || request.getSize() == null
                || request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 100) {
            throw new CustomValidationException("分页参数不合法");
        }
        if (request.getRating() != null
                && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new CustomValidationException("评分筛选应为1到5星");
        }
        if (request.getCreatedAtStart() != null && request.getCreatedAtEnd() != null
                && request.getCreatedAtStart().isAfter(request.getCreatedAtEnd())) {
            throw new CustomValidationException("开始时间不能晚于结束时间");
        }
    }

    private String validateModerationReason(ProductReviewModerateRequest request) {
        String reason = request == null || request.getReason() == null
                ? "" : request.getReason().trim();
        if (reason.length() < 2 || reason.length() > 200) {
            throw new CustomValidationException("隐藏原因长度应为2到200个字符");
        }
        return reason;
    }

    private ProductReview activeReview(Long id) {
        if (id == null) {
            throw new CustomValidationException("评价ID不能为空");
        }
        ProductReview review = reviewMapper.selectById(id);
        if (review == null) {
            throw new NoSuchElementException("评价不存在");
        }
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductReview create(ProductReviewCreateRequest request) {
        Long userId = requireUserId();
        ReviewContent content = validate(request == null ? null : request.getRating(),
                request == null ? null : request.getContent());
        if (request.getOrderItemId() == null) {
            throw new CustomValidationException("订单项不能为空");
        }

        OrderItem item = orderItemMapper.selectById(request.getOrderItemId());
        if (item == null) {
            throw new NoSuchElementException("订单项不存在");
        }
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new NoSuchElementException("订单项不存在");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new CustomConflictException("订单完成后才能评价");
        }
        lockProduct(item.getProductId());

        ProductReview review = new ProductReview();
        review.setProductId(item.getProductId());
        review.setOrderId(order.getId());
        review.setOrderItemId(item.getId());
        review.setUserId(userId);
        review.setRating(content.rating());
        review.setContent(content.content());
        review.setStatus(ProductReviewStatus.PUBLISHED);
        review.setCreatedBy(userId);
        try {
            if (reviewMapper.insert(review) != 1) {
                throw new CustomBusinessException("评价保存失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new CustomConflictException("该订单项已经评价");
        }
        refreshAggregate(item.getProductId());
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductReview update(Long id, ProductReviewUpdateRequest request) {
        Long userId = requireUserId();
        if (id == null) {
            throw new CustomValidationException("评价ID不能为空");
        }
        ReviewContent content = validate(request == null ? null : request.getRating(),
                request == null ? null : request.getContent());
        ProductReview review = ownedReview(id, userId);
        if (review.getStatus() != ProductReviewStatus.PUBLISHED) {
            throw new CustomConflictException("已隐藏评价不能修改");
        }
        lockProduct(review.getProductId());
        if (reviewMapper.updatePublishedByOwner(id, userId,
                content.rating(), content.content()) != 1) {
            mutationConflict(id, userId);
        }
        review.setRating(content.rating());
        review.setContent(content.content());
        refreshAggregate(review.getProductId());
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        Long userId = requireUserId();
        if (id == null) {
            throw new CustomValidationException("评价ID不能为空");
        }
        ProductReview review = ownedReview(id, userId);
        lockProduct(review.getProductId());
        if (reviewMapper.softDeleteByOwner(id, userId) != 1) {
            mutationConflict(id, userId);
        }
        refreshAggregate(review.getProductId());
        return true;
    }

    private Long requireUserId() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new CustomUnauthorizedException("请先登录");
        }
        return userId;
    }

    private PublicListOptions validatePublicList(
            Long productId, ProductReviewPublicListRequest request) {
        if (productId == null) {
            throw new CustomValidationException("商品ID不能为空");
        }
        if (request == null || request.getPage() == null || request.getSize() == null) {
            throw new CustomValidationException("分页参数不能为空");
        }
        if (request.getPage() < 1 || request.getSize() < 1 || request.getSize() > 50) {
            throw new CustomValidationException("评价分页参数不合法");
        }
        Integer rating = request.getRating();
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new CustomValidationException("评分筛选应为1到5星");
        }
        String sort = request.getSort() == null || request.getSort().isBlank()
                ? "newest" : request.getSort();
        if (!List.of("newest", "highest", "lowest").contains(sort)) {
            throw new CustomValidationException("不支持的评价排序方式: " + sort);
        }
        return new PublicListOptions(request.getPage(), request.getSize(), sort, rating);
    }

    private void requireOnSaleProduct(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getOnSale, 1));
        if (product == null) {
            throw new NoSuchElementException("商品不存在");
        }
    }

    private void applyPublicSort(LambdaQueryWrapper<ProductReview> query, String sort) {
        switch (sort) {
            case "highest" -> query.orderByDesc(ProductReview::getRating)
                    .orderByDesc(ProductReview::getCreatedAt)
                    .orderByDesc(ProductReview::getId);
            case "lowest" -> query.orderByAsc(ProductReview::getRating)
                    .orderByDesc(ProductReview::getCreatedAt)
                    .orderByDesc(ProductReview::getId);
            default -> query.orderByDesc(ProductReview::getCreatedAt)
                    .orderByDesc(ProductReview::getId);
        }
    }

    private Map<Long, OrderItem> loadOrderItems(List<ProductReview> reviews) {
        List<Long> itemIds = reviews.stream()
                .map(ProductReview::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return orderItemMapper.selectByIds(itemIds).stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity(),
                        (left, ignored) -> left));
    }

    private ProductReviewResponse toPublicResponse(ProductReview review, OrderItem item) {
        ProductReviewResponse response = new ProductReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProductId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setReviewerLabel("已购用户");
        response.setVerifiedPurchase(true);
        response.setSkuTitle(item == null ? null : item.getSkuTitle());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }

    private ProductReviewAdminResponse toAdminResponse(ProductReview review, OrderItem item) {
        ProductReviewAdminResponse response = new ProductReviewAdminResponse();
        response.setId(review.getId());
        response.setProductId(review.getProductId());
        response.setUserId(review.getUserId());
        response.setOrderId(review.getOrderId());
        response.setOrderItemId(review.getOrderItemId());
        response.setProductTitle(item == null ? null : item.getProductTitle());
        response.setSkuTitle(item == null ? null : item.getSkuTitle());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setStatus(review.getStatus());
        response.setModerationReason(review.getModerationReason());
        response.setModeratedAt(review.getModeratedAt());
        response.setModeratedBy(review.getModeratedBy());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }

    private long aggregateValue(Long value) {
        return value == null ? 0L : value;
    }

    private ReviewContent validate(Integer rating, String rawContent) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new CustomValidationException("评分应为1到5星");
        }
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.length() < 5 || content.length() > 1000) {
            throw new CustomValidationException("评价内容长度应为5到1000个字符");
        }
        return new ReviewContent(rating, content);
    }

    private ProductReview ownedReview(Long id, Long userId) {
        ProductReview review = reviewMapper.selectOwnedActiveById(id, userId);
        if (review == null) {
            throw new NoSuchElementException("评价不存在");
        }
        return review;
    }

    private void lockProduct(Long productId) {
        if (productId == null || productMapper.selectByIdForUpdate(productId) == null) {
            throw new NoSuchElementException("商品不存在");
        }
    }

    private void refreshAggregate(Long productId) {
        ProductReviewAggregate aggregate = reviewMapper.selectAggregate(productId);
        long count = aggregate == null || aggregate.count() == null ? 0L : aggregate.count();
        BigDecimal average = aggregate == null || aggregate.averageRating() == null
                ? BigDecimal.ZERO : aggregate.averageRating();
        average = average.setScale(2, RoundingMode.HALF_UP);
        if (productMapper.updateReviewAggregate(productId, average, count) != 1) {
            throw new CustomBusinessException("商品评分更新失败");
        }
    }

    private void mutationConflict(Long id, Long userId) {
        if (reviewMapper.selectOwnedActiveById(id, userId) == null) {
            throw new NoSuchElementException("评价不存在");
        }
        throw new CustomConflictException("评价状态已变化，请刷新后重试");
    }

    private record ReviewContent(Integer rating, String content) {
    }

    private record PublicListOptions(int page, int size, String sort, Integer rating) {
    }
}
