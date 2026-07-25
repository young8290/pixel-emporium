-- 插入默认角色
INSERT INTO `role` (`id`, `name`, `code`, `description`, `created_at`)
VALUES (1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限', NOW()),
       (2, '管理员', 'ADMIN', '拥有大部分管理权限', NOW()),
       (3, '普通用户', 'USER', '基本用户权限', NOW());

-- 给用户分配角色
--   1. 给用户 178 分配超级管理员角色
--   2. 给用户 256 分配普通用户角色
INSERT INTO `user_role` (`id`, `user_id`, `role_id`, `created_at`)
VALUES (1, 178, 1, NOW()),
       (2, 256, 3, NOW());

-- 插入示例权限
INSERT INTO `permission` (id, name, category, type, path, icon, sort_order, code, resource, action, description,
                          created_at, created_by)
VALUES (1, '新增用户', '用户管理', 'BUTTON', null, 'user', 1, 'user:add', 'user', 'add', '', NOW(), null),
       (2, '修改用户', '用户管理', 'BUTTON', null, 'user', 2, 'user:update', 'user', 'update', '', NOW(), null),
       (3, '用户列表', '用户管理', 'MENU', '/admin/user', 'user', 3, 'user:list', 'user', 'list', '', NOW(), null),
       (4, '删除用户', '用户管理', 'BUTTON', null, 'user', 4, 'user:delete', 'user', 'delete', '', NOW(), null);

-- 为角色分配示例权限
--   1. 给角色 2 分配所有示例权限
--   2. 给角色 3 分配“用户列表”权限
INSERT INTO `role_permission` (`id`, `role_id`, `permission_id`, `created_at`)
VALUES (1, 2, 1, NOW()),
       (2, 2, 2, NOW()),
       (3, 2, 3, NOW()),
       (4, 2, 4, NOW()),
       (5, 3, 3, NOW());