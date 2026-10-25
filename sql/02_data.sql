-- ============================================================
-- 医疗后台管理系统 · 种子数据初始化
-- 账号：admin / doc01 / nurse01 / cashier01 / pharmacist01 / dean01
-- 密码统一为：123456（BCrypt）
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 角色（六角色矩阵，见需求文档 §1.3 / 接口文档阶段3 权限说明）
-- ------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_code, remark, status) VALUES
(1, '超级管理员', 'admin',     '系统内置角色，拥有全部权限', 1),
(2, '医生',       'doctor',    '门诊接诊、开处方/检查单',     1),
(3, '护士',       'nurse',     '住院护理、检验执行',          1),
(4, '收费员',     'cashier',   '门诊收费、退费、日结',        1),
(5, '药剂师',     'pharmacist','药房发药、退药、库存',        1),
(6, '院长',       'dean',      '统计报表只读',                1);

-- ------------------------------------------------------------
-- 科室（病区复用科室ID：科室即病区，见接口文档 §6.2）
-- ------------------------------------------------------------
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort, status) VALUES
(1, 0, '内科',   'NK', 1, 1, 1),
(2, 0, '外科',   'WK', 1, 2, 1),
(3, 0, '儿科',   'EK', 1, 3, 1),
(4, 0, '妇产科', 'FK', 1, 4, 1),
(5, 0, '急诊科', 'JK', 1, 5, 1),
(6, 0, '医技科', 'YJ', 3, 6, 1),
(7, 0, '药剂科', 'YJK', 3, 7, 1),
(8, 0, '财务科', 'CWK', 4, 8, 1),
(9, 0, '行政办', 'XZB', 4, 9, 1);

-- ------------------------------------------------------------
-- 菜单：13 个顶级（id 与接口文档阶段编号对齐）+ 系统管理子菜单
-- menu_type: 1目录 2菜单
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(1,  '系统管理', 0, '/system',       NULL, 'Setting',  1, 1,  1),
(2,  '患者管理', 0, '/patient',      NULL, 'User',     1, 2,  1),
(3,  '挂号预约', 0, '/registration', NULL, 'Calendar', 1, 3,  1),
(4,  '门诊管理', 0, '/clinic',       NULL, 'Monitor',  1, 4,  1),
(5,  '药房管理', 0, '/pharmacy',     NULL, 'Box',      1, 5,  1),
(6,  '收费财务', 0, '/finance',      NULL, 'Money',    1, 6,  1),
(7,  '统计报表', 0, '/report',       NULL, 'TrendCharts', 1, 7, 1),
(8,  '数据总览', 0, '/overview',     NULL, 'Odometer', 1, 8,  1),
(9,  '住院管理', 0, '/inpatient',    NULL, 'Bed',      1, 9,  1),
(10, '检查检验', 0, '/exam',         NULL, 'Document', 1, 10, 1),
(11, '公共卫生', 0, '/health',       NULL, 'FirstAidKit', 1, 11, 1),
(12, '消息中心', 0, '/message',      NULL, 'Bell',     1, 12, 1),
(13, '数据安全', 0, '/audit',        NULL, 'Lock',     1, 13, 1),
-- 系统管理子菜单（后续阶段业务子菜单自 51 起编号，见接口文档 §6.2⑧）
(14, '用户管理', 1, 'user', 'system:user:list', 'UserFilled', 2, 1, 1),
(15, '角色管理', 1, 'role', 'system:role:list', 'Avatar',     2, 2, 1),
(16, '科室管理', 1, 'dept', 'system:dept:list', 'OfficeBuilding', 2, 3, 1),
(17, '字典管理', 1, 'dict', 'system:dict:list', 'Collection', 2, 4, 1);

-- ------------------------------------------------------------
-- 角色-菜单：admin 全量；其余角色业务菜单由后续阶段按矩阵分配
-- ------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- ------------------------------------------------------------
-- 用户（密码统一 123456）
-- ------------------------------------------------------------
INSERT INTO sys_user (id, username, password, real_name, gender, phone, email, dept_id, role_id, status) VALUES
(1, 'admin',       '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '系统管理员', 1, '13800000001', 'admin@hospital.com',    9, 1, 1),
(2, 'doc01',       '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '张三医生',   1, '13800000002', 'doc01@hospital.com',    1, 2, 1),
(3, 'nurse01',     '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '李四护士',   0, '13800000003', 'nurse01@hospital.com',  1, 3, 1),
(4, 'cashier01',   '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '王五收费员', 0, '13800000004', 'cashier01@hospital.com',8, 4, 1),
(5, 'pharmacist01','$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '赵六药剂师', 1, '13800000005', 'pharm01@hospital.com',  7, 5, 1),
(6, 'dean01',      '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '孙院长',     1, '13800000006', 'dean01@hospital.com',   9, 6, 1);

-- ------------------------------------------------------------
-- 字典：患者标签（三色分级，接口文档 §2.1 备注）
-- ------------------------------------------------------------
INSERT INTO sys_dict_type (id, dict_name, dict_type, remark, status) VALUES
(1, '患者标签', 'patient_tag', 'biz_patient.tags 存标签文本，筛选走 FIND_IN_SET', 1);

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, color, sort, status) VALUES
(1, 'patient_tag', '高风险', '高风险', 'red',    1, 1),
(2, 'patient_tag', '慢病',   '慢病',   'yellow', 2, 1),
(3, 'patient_tag', 'VIP',    'VIP',    'green',  3, 1);
