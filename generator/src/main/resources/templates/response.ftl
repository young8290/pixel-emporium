package com.example.${module}.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

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
public class ${Domain}Response {
<#assign ignoreColumns = ["created_by", "updated_at", "updated_by", "deleted", "deleted_at", "deleted_by"]>

<#list fieldList as field>
  <#if !ignoreColumns?seq_contains(field.name)>
    <#if field.name == 'id' || (field.name?ends_with('_id') && field.javaType == 'Long')>
    @JsonSerialize(using = ToStringSerializer.class)
    </#if>
    <#if field.javaType=='LocalTime'>
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    <#elseif field.javaType=='LocalDate'>
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    <#elseif field.javaType=='LocalDateTime'>
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    </#if>
    private ${field.javaType} ${field.nameLowerCamelCase};

  </#if>
</#list>
}
