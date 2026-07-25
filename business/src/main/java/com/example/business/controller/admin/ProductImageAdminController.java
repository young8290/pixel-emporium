package com.example.business.controller.admin;

import com.example.business.annotation.OperationLog;
import com.example.business.entity.ProductImage;
import com.example.business.response.ProductImageResponse;
import com.example.business.service.AsyncProductImageStyleDispatcher;
import com.example.business.service.ProductImageService;
import com.example.business.service.ProductImageStyleDispatcher;
import com.example.business.service.ProductImageStyleService;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.exception.CustomBusinessException;
import com.example.common.response.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("admin/product-image")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class ProductImageAdminController {
    private final ProductImageService imageService;
    private final ProductImageStyleService styleService;
    private final ProductImageStyleDispatcher dispatcher;

    @Autowired
    public ProductImageAdminController(ProductImageService imageService,
                                       ProductImageStyleService styleService,
                                       ProductImageStyleDispatcher dispatcher) {
        this.imageService = imageService;
        this.styleService = styleService;
        this.dispatcher = dispatcher;
    }

    public ProductImageAdminController(ProductImageService imageService,
                                       ProductImageStyleService styleService) {
        this(imageService, styleService, (imageId, presetKey) -> {
        });
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(action = "PRODUCT_IMAGE_UPLOAD", targetType = "PRODUCT", targetIdSpEL = "#productId")
    public R<ProductImageResponse> upload(@RequestParam Long productId,
                                          @RequestParam(required = false) Long skuId,
                                          @RequestParam(required = false) Integer sort,
                                          @RequestParam("file") MultipartFile file) {
        return R.ok(ProductImageResponse.from(imageService.upload(productId, skuId, sort, file)));
    }

    @GetMapping("/list")
    public R<List<ProductImageResponse>> list(@RequestParam Long productId,
                                              @RequestParam(required = false) Long skuId) {
        return R.ok(imageService.list(productId, skuId));
    }

    @DeleteMapping("/{id}")
    @OperationLog(action = "PRODUCT_IMAGE_DELETE", targetType = "PRODUCT_IMAGE", targetIdSpEL = "#id")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(imageService.delete(id));
    }

    @PostMapping("/{id}/set-main")
    @OperationLog(action = "PRODUCT_IMAGE_SET_MAIN", targetType = "PRODUCT_IMAGE", targetIdSpEL = "#id")
    public R<ProductImageResponse> setMain(@PathVariable Long id) {
        return R.ok(imageService.setMain(id));
    }

    @PostMapping("/{id}/stylize")
    @OperationLog(action = "PRODUCT_IMAGE_STYLIZE", targetType = "PRODUCT_IMAGE", targetIdSpEL = "#id")
    public R<ProductImageResponse> stylize(@PathVariable Long id,
                                           @RequestBody @Valid StylizeRequest request) {
        ProductImage pending = styleService.prepare(id, request.getPresetKey());
        try {
            dispatcher.dispatch(id, request.getPresetKey());
        } catch (TaskRejectedException exception) {
            styleService.markFailed(id, request.getPresetKey(), "任务队列已满");
            throw new CustomBusinessException("转换任务队列已满，请稍后重试");
        }
        return R.ok(ProductImageResponse.from(pending));
    }

    @GetMapping("/{id}/style-status")
    public R<ProductImageResponse> styleStatus(@PathVariable Long id) {
        return R.ok(imageService.status(id));
    }

    @Getter
    @Setter
    public static class StylizeRequest {
        @NotBlank(message = "风格预设不能为空")
        private String presetKey;
    }
}
