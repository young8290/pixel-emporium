package com.example.${module}.service.impl;
<#-- 判断表是否有 user_id 字段 -->
<#assign hasUserId = false>
<#list fieldList as field>
    <#if field.name == "user_id" || field.nameLowerCamelCase == "userId">
        <#assign hasUserId = true>
        <#break>
    </#if>
</#list>

<#if hasUserId>
import com.example.common.core.UserContext;
import com.example.common.exception.CustomForbiddenException;
</#if>
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.${module}.entity.${Domain};
import com.example.${module}.mapper.${Domain}Mapper;
import com.example.${module}.request.${Domain}ListRequest;
import com.example.${module}.request.${Domain}Request;
import com.example.common.response.PageResponse;
import com.example.${module}.response.${Domain}Response;
import com.example.${module}.service.${Domain}Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.example.common.exception.CustomValidationException;
import java.util.NoSuchElementException;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class ${Domain}ServiceImpl extends ServiceImpl<${Domain}Mapper, ${Domain}> implements ${Domain}Service {
    @Override
    @Transactional
    public boolean saveOrUpdate(${Domain}Request request) {
<#if hasUserId>
        Long currentUserId = UserContext.get();
</#if>
        ${Domain} ${domain};

        if (request.getId() != null) {
            // 更新逻辑
            ${domain} = find${Domain}(request.getId());
<#if hasUserId>
            // 校验所有权：检查数据库中该资源的归属是否为当前登录用户
            if (${domain}.getUserId() == null || !${domain}.getUserId().equals(currentUserId)) {
                throw new CustomForbiddenException("无权操作此${tableNameCn!}信息");
            }
</#if>
            BeanUtil.copyProperties(request, ${domain}, "userId");
        } else {
            // 新增逻辑
            ${domain} = BeanUtil.copyProperties(request, ${Domain}.class);
<#if hasUserId>
            // 强制绑定当前用户
            ${domain}.setUserId(currentUserId);
</#if>
        }

        return this.saveOrUpdate(${domain});
    }

    @Override
    @Transactional
    public boolean delete${Domain}(Long id) {
<#if hasUserId>
        ${Domain} ${domain} = find${Domain}(id);
        // 安全校验：只能删除自己的${tableNameCn!}
        if (${domain}.getUserId() == null || !${domain}.getUserId().equals(UserContext.get())) {
            throw new CustomForbiddenException("无权删除此${tableNameCn!}信息");
        }

</#if>
        return this.removeById(id);
    }

    @Override
    public PageResponse<${Domain}Response> get${Domain}Page(${Domain}ListRequest request) {
        LambdaQueryWrapper<${Domain}> qw = buildQuery(request);
<#if hasUserId>
        // 强制限定搜索范围为当前用户 (水平越权防护)
        qw.eq(${Domain}::getUserId, UserContext.get());
</#if>

        return executePageQuery(request, qw);
    }

    @Override
    public PageResponse<${Domain}Response> getAdmin${Domain}Page(${Domain}ListRequest request) {
        LambdaQueryWrapper<${Domain}> qw = buildQuery(request);

<#if hasUserId>
        // 管理员可选：按用户ID筛选
        if (request.getUserId() != null) {
            qw.eq(${Domain}::getUserId, request.getUserId());
        }

</#if>
        return executePageQuery(request, qw);
    }

    @Override
    @Transactional
    public boolean adminAdd(${Domain}Request request) {
<#if hasUserId>
        if (request.getUserId() == null) {
            throw new CustomValidationException("管理员添加${tableNameCn!}必须指定 userId");
        }

</#if>
        ${Domain} ${domain} = BeanUtil.copyProperties(request, ${Domain}.class);
<#if hasUserId>
        // 使用请求中的 userId（管理员可以指定任意用户）
        ${domain}.setUserId(request.getUserId());
</#if>
        return this.save(${domain});
    }

    @Override
    @Transactional
    public boolean adminUpdate(${Domain}Request request) {
        ${Domain} ${domain} = find${Domain}(request.getId());

        // 管理员可以修改所有字段<#if hasUserId>，包括 userId（允许调整数据归属关系）</#if>
        BeanUtil.copyProperties(request, ${domain});
        return this.updateById(${domain});
    }

    @Override
    @Transactional
    public boolean adminDelete(Long id) {
        return this.removeById(find${Domain}(id).getId());
    }

    @Override
    @Transactional
    public boolean adminDeleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 批量删除，MyBatis-Plus 的 removeByIds 会自动处理逻辑删除
        return this.removeByIds(ids);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建基础查询对象 (处理通用过滤如关键词)
     */
    private LambdaQueryWrapper<${Domain}> buildQuery(${Domain}ListRequest request) {
        LambdaQueryWrapper<${Domain}> qw = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getKeyword())) {
            String kw = request.getKeyword();
            <#assign counter = 0>
            <#list fieldList as field>
                <#if field.searchable>
                    <#assign counter = counter + 1>
                    <#if counter == 1>
            qw.like(${Domain}::get${field.nameLowerCamelCase?cap_first}, kw);
                    <#else>
            qw.or().like(${Domain}::get${field.nameLowerCamelCase?cap_first}, kw);
                    </#if>
                </#if>
            </#list>
        }

        qw.orderByDesc(${Domain}::getCreatedAt);
        return qw;
    }

    /**
     * 执行分页查询并转换结果
     */
    private PageResponse<${Domain}Response> executePageQuery(${Domain}ListRequest request,
                                                             LambdaQueryWrapper<${Domain}> qw) {
        Page<${Domain}> page = this.page(Page.of(request.getPage(), request.getSize()), qw);

        List<${Domain}Response> list = BeanUtil.copyToList(page.getRecords(), ${Domain}Response.class);

        PageResponse<${Domain}Response> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(list);
        return response;
    }

    /**
     * 统一查找实体并抛出异常
     */
    private ${Domain} find${Domain}(Long id) {
        return this.getOptById(id)
                .orElseThrow(() -> new NoSuchElementException("${tableNameCn!}不存在"));
    }
}
