-- ============================================================
-- 医疗后台管理系统 · 患者管理模块（阶段2）
-- 患者档案 + 就诊卡扩展列 + 标签；菜单与角色权限
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 患者表 biz_patient（接口文档 §3.1）
-- 身份证号课程级明文存储，展示统一脱敏；patient_no/card_no 先插后回补
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_patient (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_no      VARCHAR(30)   DEFAULT NULL COMMENT '患者编号 P%05d',
  name            VARCHAR(50)   NOT NULL COMMENT '姓名',
  gender          TINYINT       DEFAULT 1 COMMENT '性别 0女1男',
  birth_date      DATE          DEFAULT NULL COMMENT '出生日期',
  id_card         VARCHAR(20)   DEFAULT NULL COMMENT '身份证号（加密，展示脱敏）',
  phone           VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
  address         VARCHAR(255)  DEFAULT NULL COMMENT '住址',
  blood_type      VARCHAR(10)   DEFAULT NULL COMMENT '血型',
  allergy_history VARCHAR(500)  DEFAULT NULL COMMENT '过敏史',
  medical_history TEXT          DEFAULT NULL COMMENT '既往病史',
  family_history  TEXT          DEFAULT NULL COMMENT '家族病史',
  card_no         VARCHAR(30)   DEFAULT NULL COMMENT '就诊卡号 C%05d',
  card_status     TINYINT       DEFAULT 1 COMMENT '就诊卡状态 0挂失1正常',
  card_balance    DECIMAL(10,2) DEFAULT 0.00 COMMENT '卡余额',
  tags            VARCHAR(255)  DEFAULT NULL COMMENT '标签（逗号分隔，FIND_IN_SET 精确匹配）',
  create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
  update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_patient_no (patient_no),
  KEY idx_name (name),
  KEY idx_phone (phone)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者表';

-- ------------------------------------------------------------
-- 菜单：患者档案（parent_id=2 患者管理）+ 按钮四件套
-- 权限码按 system 模块同族风格
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(18, '患者档案', 2, 'archive', 'patient:list', 'Notebook', 2, 1, 1),
(19, '患者新增', 18, '#', 'patient:add',    NULL, 3, 1, 1),
(20, '患者编辑', 18, '#', 'patient:edit',   NULL, 3, 2, 1),
(21, '患者删除', 18, '#', 'patient:delete', NULL, 3, 3, 1),
(22, '就诊卡操作', 18, '#', 'patient:card',  NULL, 3, 4, 1);

-- ------------------------------------------------------------
-- 角色分配（按文档角色矩阵惯例：dean 只读，cashier/pharmacist 无）
-- admin 全量
-- ------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (18, 19, 20, 21, 22);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (18, 19, 20, 21, 22)
WHERE r.role_code IN ('doctor', 'nurse');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 18 FROM sys_role r WHERE r.role_code = 'dean';

-- ------------------------------------------------------------
-- 示例患者（供联调）
-- ------------------------------------------------------------
INSERT INTO biz_patient (id, name, gender, birth_date, id_card, phone, address, blood_type, allergy_history, tags) VALUES
(1, '张三', 1, '1990-05-01', '440300199005011234', '13800000101', '深圳市南山区科技园 12 号', 'A', '青霉素过敏', '慢病'),
(2, '李四', 0, '1996-11-20', '440300199611202345', '13800000102', '深圳市福田区华强北 3 号', 'B', NULL, 'VIP'),
(3, '王五', 1, '1958-03-15', '440300195803153456', '13800000103', '深圳市罗湖区东门路 8 号', 'O', '磺胺类过敏', '高风险,慢病');
