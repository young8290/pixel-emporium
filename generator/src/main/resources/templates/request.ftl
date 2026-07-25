package com.example.${module}.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;
<#list typeSet as type>
    <#if type == 'LocalDateTime'>
import java.time.LocalDateTime;
    </#if>
    <#if type == 'LocalDate'>
import java.time.LocalDate;
    </#if>
    <#if type == 'LocalTime'>
import java.time.LocalTime;
    </#if>
    <#if type == 'BigDecimal'>
import java.math.BigDecimal;
    </#if>
</#list>

@Getter
@Setter
public class ${Domain}Request {
<#list fieldList as field>
  <#if field.nameLowerCamelCase != 'createdAt' && field.nameLowerCamelCase != 'createdBy' && field.nameLowerCamelCase != 'updatedAt' && field.nameLowerCamelCase != 'updatedBy' && field.nameLowerCamelCase != 'deleted' && field.nameLowerCamelCase != 'deletedAt' && field.nameLowerCamelCase != 'deletedBy'>
    <#if field.name == 'email'>
    @Email(message = "邮箱格式不正确")
    </#if>
    <#if !field.nullAble && field.name != 'id'>
        <#if field.javaType == 'String'>
    @NotBlank(message = "${field.nameCn}不能为空")
            <#else>
    @NotNull(message = "${field.nameCn}不能为空")
        </#if>
        <#if field.length gt 0>
    @Size(max = ${field.length})
        </#if>
    </#if>
    <#if field.javaType=='LocalTime'>
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    <#elseif field.javaType=='LocalDate'>
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    <#elseif field.javaType=='LocalDateTime'>
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    </#if>
    <#if field.nameLowerCamelCase=='idCard'>
    @Size(min = 18, max = 18, message = "【${field.nameCn}】必须是18位")
    </#if>
    <#if field.nameLowerCamelCase == 'mobile'>
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "【${field.nameCn}】格式不正确")
    </#if>
    private ${field.javaType} ${field.nameLowerCamelCase};

  </#if>
</#list>
}
