-- 角色表
CREATE TABLE `role`
(
    `id`          BIGINT      NOT NULL COMMENT 'ID',
    `name`        VARCHAR(50) NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(50) NOT NULL COMMENT '角色编码',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',

    -- BaseEntity 字段
    `created_at`  DATETIME     DEFAULT NULL COMMENT '创建时间',
    `created_by`  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `updated_at`  DATETIME     DEFAULT NULL COMMENT '更新时间',
    `updated_by`  BIGINT       DEFAULT NULL COMMENT '更新人ID',
    `deleted`     BIGINT       DEFAULT 0 COMMENT '逻辑删除标记',
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '删除时间',
    `deleted_by`  BIGINT       DEFAULT NULL COMMENT '删除人ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) COMMENT ='角色';

-- 权限表
CREATE TABLE `permission`
(
    `id`          BIGINT                        NOT NULL COMMENT 'ID',
    `name`        VARCHAR(50)                   NOT NULL COMMENT '权限名称',
    `category`    varchar(64)                   NULL COMMENT '权限分类(如: 用户管理)',
    `type`        varchar(16)  DEFAULT 'BUTTON' NULL COMMENT '类型(DIR:目录, MENU:菜单, BUTTON:按钮)',
    `path`        varchar(255)                  NULL COMMENT '路由路径',
    `icon`        varchar(64)                   NULL COMMENT '图标',
    `sort_order`  int          DEFAULT 99       NULL COMMENT '排序权重',
    `code`        VARCHAR(100)                  NOT NULL COMMENT '权限编码',
    `resource`    VARCHAR(100) DEFAULT NULL COMMENT '资源路径',
    `action`      VARCHAR(20)  DEFAULT NULL COMMENT '操作类型',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '权限描述',

    -- BaseEntity 字段
    `created_at`  DATETIME     DEFAULT NULL COMMENT '创建时间',
    `created_by`  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `updated_at`  DATETIME     DEFAULT NULL COMMENT '更新时间',
    `updated_by`  BIGINT       DEFAULT NULL COMMENT '更新人ID',
    `deleted`     BIGINT       DEFAULT 0 COMMENT '逻辑删除标记',
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '删除时间',
    `deleted_by`  BIGINT       DEFAULT NULL COMMENT '删除人ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) COMMENT ='权限';

-- 用户-角色关联表
CREATE TABLE `user_role`
(
    `id`         BIGINT NOT NULL COMMENT 'ID',
    `user_id`    BIGINT NOT NULL COMMENT '用户ID',
    `role_id`    BIGINT NOT NULL COMMENT '角色ID',

    -- BaseEntity 字段
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `created_by` BIGINT   DEFAULT NULL COMMENT '创建人ID',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `updated_by` BIGINT   DEFAULT NULL COMMENT '更新人ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) COMMENT ='用户-角色关联';

-- 角色-权限关联表
CREATE TABLE `role_permission`
(
    `id`            BIGINT NOT NULL COMMENT 'ID',
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',

    -- BaseEntity 字段
    `created_at`    DATETIME DEFAULT NULL COMMENT '创建时间',
    `created_by`    BIGINT   DEFAULT NULL COMMENT '创建人ID',
    `updated_at`    DATETIME DEFAULT NULL COMMENT '更新时间',
    `updated_by`    BIGINT   DEFAULT NULL COMMENT '更新人ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) COMMENT ='角色-权限关联';