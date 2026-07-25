package com.example.business.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;

    private Long parentId = 0L;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder = 99;

    @Size(max = 255, message = "图标文件名不能超过255个字符")
    private String icon;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled = 1;
}
