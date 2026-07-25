package com.example.${module}.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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

@Setter
@Getter
public class ${Domain} extends BaseEntity {
<#assign baseColumns = ["id", "created_at", "created_by", "updated_at", "updated_by", "deleted", "deleted_at", "deleted_by"]>
<#-- 检查字段名是否是 MySQL 保留关键字（常见关键字列表） -->
<#assign mysqlReservedKeywords = ["index", "order", "group", "select", "table", "key", "value", "type", "status", "level", "rank", "user", "date", "time", "timestamp", "desc", "asc", "limit", "offset", "from", "where", "join", "inner", "left", "right", "outer", "on", "as", "case", "when", "then", "else", "end", "if", "while", "return", "call", "declare", "handler", "condition", "signal", "get", "open", "close", "fetch", "lock", "unlock", "execute", "prepare", "set", "show", "describe", "explain", "use", "load", "create", "drop", "alter", "rename", "truncate", "insert", "update", "delete", "replace", "merge", "do", "values", "with", "into", "partition", "columns", "rows", "row", "format", "constraint", "references", "match", "full", "null", "default", "enum", "set", "json", "primary", "unique", "fulltext", "using", "algorithm", "none", "temporary", "memory", "range", "list", "in", "engine", "comment", "union"]>
    <#list fieldList as field>
    <#if !baseColumns?seq_contains(field.name)>
    /**
     * ${field.comment?split('｜')?first}
     */
      <#assign isReservedKeyword = false>
      <#if mysqlReservedKeywords?seq_contains(field.name?lower_case)>
          <#assign isReservedKeyword = true>
      </#if>
      <#if isReservedKeyword>
      <#-- 如果是保留关键字，添加带反引号的 @TableField 注解；否则使用 MyBatis-Plus 自动生成的注解 -->
      <#-- https://baomidou.com/reference/question/#3x版本中数据库关键字处理方法 -->
    @TableField("`${field.name}`")
      </#if>
    private ${field.javaType} ${field.nameLowerCamelCase};

    </#if>
    </#list>
}