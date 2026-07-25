package com.example.${module}.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.${module}.entity.${Domain};
import com.example.${module}.request.${Domain}ListRequest;
import com.example.common.response.PageResponse;
import com.example.${module}.response.${Domain}Response;
import com.example.${module}.request.${Domain}Request;

import java.util.List;

public interface ${Domain}Service extends IService<${Domain}> {
    /**
     * 保存或更新${tableNameCn!}
     * 会强制关联当前登录用户
     */
    boolean saveOrUpdate(${Domain}Request request);

    /**
     * 删除${tableNameCn!}
     * 包含所有权检查
     */
    boolean delete${Domain}(Long id);

    /**
     * 分页查询当前用户的${tableNameCn!}
     */
    PageResponse<${Domain}Response> get${Domain}Page(${Domain}ListRequest request);

    /**
     * 【管理员】分页查询全量${tableNameCn!}
     */
    PageResponse<${Domain}Response> getAdmin${Domain}Page(${Domain}ListRequest request);

    /**
     * 【管理员】添加${tableNameCn!}
     * 如果该实体表有关联的 userId 字段，管理员任意可以指定该字段的值
     */
    boolean adminAdd(${Domain}Request request);

    /**
     * 【管理员】更新${tableNameCn!}信息 (跨用户强制操作)
     */
    boolean adminUpdate(${Domain}Request request);

    /**
     * 【管理员】逻辑删除${tableNameCn!} (无需所有权检查)
     */
    boolean adminDelete(Long id);

    /**
     * 【管理员】批量逻辑删除${tableNameCn!} (无需所有权检查)
     */
    boolean adminDeleteBatch(List<Long> ids);
}