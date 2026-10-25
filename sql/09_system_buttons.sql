-- ============================================================
-- 补齐系统管理子模块按钮：角色编辑/权限分配/删除、字典新增/改色/删除
-- （编号避开文档为后续阶段预留的 51-70 段）
-- ============================================================
USE medical;

INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(38, '字典新增', 17, '#', 'system:dict:add',    NULL, 3, 1, 1),
(39, '字典编辑', 17, '#', 'system:dict:edit',   NULL, 3, 2, 1),
(40, '字典删除', 17, '#', 'system:dict:delete', NULL, 3, 3, 1),
(41, '角色编辑', 15, '#', 'system:role:edit',   NULL, 3, 1, 1),
(42, '角色权限', 15, '#', 'system:role:perm',   NULL, 3, 2, 1),
(43, '角色删除', 15, '#', 'system:role:delete', NULL, 3, 3, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (38, 39, 40, 41, 42, 43)
WHERE r.role_code = 'admin';
