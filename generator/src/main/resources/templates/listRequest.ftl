package com.example.${module}.request;

import com.example.common.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ${Domain}ListRequest extends PageRequest {
<#-- 判断表是否有 user_id 字段 -->
<#assign hasUserId = false>
<#list fieldList as field>
    <#if field.name == "user_id" || field.nameLowerCamelCase == "userId">
        <#assign hasUserId = true>
        <#break>
    </#if>
</#list>
<#if hasUserId>
    @Schema(description = "用户ID (仅管理员端有效)")
    private Long userId;

</#if>
    @Schema(description = "搜索关键词", example = "\"\"")
    private String keyword;
}
