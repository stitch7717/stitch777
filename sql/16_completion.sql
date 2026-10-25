-- ============================================================
-- 医疗后台管理系统 · 功能补齐（随访/发票/采购/妇幼保健/短信/自定义报表）
-- ============================================================
USE medical;

-- 随访表（文档 §3.1）
CREATE TABLE IF NOT EXISTS biz_follow_up (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id  BIGINT       NOT NULL COMMENT '患者ID',
  plan_date   DATE         DEFAULT NULL COMMENT '计划随访日期',
  follow_type TINYINT      DEFAULT 1 COMMENT '1电话2上门3短信',
  content     TEXT         DEFAULT NULL COMMENT '随访内容',
  result      VARCHAR(500) DEFAULT NULL COMMENT '随访结果',
  operator_id BIGINT       DEFAULT NULL COMMENT '随访人',
  status      TINYINT      DEFAULT 0 COMMENT '0待随访1已完成',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_patient (patient_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '随访表';

-- 发票表（文档 §9.1）
CREATE TABLE IF NOT EXISTS biz_invoice (
  id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  invoice_no   VARCHAR(50)   DEFAULT NULL COMMENT '发票号',
  charge_id    BIGINT        NOT NULL COMMENT '收费单ID',
  invoice_type TINYINT       DEFAULT 1 COMMENT '1电子2纸质',
  amount       DECIMAL(10,2) NOT NULL COMMENT '金额',
  status       TINYINT       DEFAULT 0 COMMENT '0正常1作废',
  print_time   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '打印时间',
  create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_invoice_no (invoice_no),
  KEY idx_charge (charge_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发票表';

-- 供应商（采购管理自拟）
CREATE TABLE IF NOT EXISTS biz_supplier (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(100) NOT NULL COMMENT '供应商名称',
  contact     VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  address     VARCHAR(255) DEFAULT NULL COMMENT '地址',
  status      TINYINT      DEFAULT 1 COMMENT '状态 0停用1启用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商表';

-- 采购订单（自拟：0待入库 1已入库 2已取消）
CREATE TABLE IF NOT EXISTS biz_purchase_order (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no    VARCHAR(30)   DEFAULT NULL COMMENT '采购单号 PO+日期+序号',
  supplier_id BIGINT        NOT NULL COMMENT '供应商ID',
  drug_id     BIGINT        NOT NULL COMMENT '药品ID',
  quantity    INT           NOT NULL COMMENT '采购数量',
  unit_price  DECIMAL(10,2) NOT NULL COMMENT '采购单价',
  total       DECIMAL(10,2) NOT NULL COMMENT '总金额',
  status      TINYINT       DEFAULT 0 COMMENT '0待入库1已入库2已取消',
  operator_id BIGINT        DEFAULT NULL COMMENT '经办人',
  create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购订单表';

-- 妇幼保健：孕产妇管理（自拟）
CREATE TABLE IF NOT EXISTS biz_maternal_record (
  id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id       BIGINT       NOT NULL COMMENT '患者ID',
  last_period_date DATE         DEFAULT NULL COMMENT '末次月经日期',
  expected_date    DATE         DEFAULT NULL COMMENT '预产期',
  checkup_count    INT          DEFAULT 0 COMMENT '产检次数',
  status           TINYINT      DEFAULT 1 COMMENT '1孕中2已分娩',
  remark           VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted          TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_patient (patient_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '孕产妇管理表';

-- 妇幼保健：儿童保健（自拟）
CREATE TABLE IF NOT EXISTS biz_child_care (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  patient_id   BIGINT       NOT NULL COMMENT '儿童患者ID',
  birth_weight DECIMAL(5,2) DEFAULT NULL COMMENT '出生体重kg',
  height       DECIMAL(5,1) DEFAULT NULL COMMENT '身高cm',
  weight       DECIMAL(5,2) DEFAULT NULL COMMENT '体重kg',
  check_date   DATE         DEFAULT NULL COMMENT '检查日期',
  remark       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_patient (patient_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '儿童保健表';

-- 短信记录（文档 §12.1）
CREATE TABLE IF NOT EXISTS sys_sms_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  phone         VARCHAR(20)  NOT NULL COMMENT '手机号',
  template_code VARCHAR(50)  DEFAULT NULL COMMENT '模板编码',
  content       VARCHAR(500) DEFAULT NULL COMMENT '短信内容',
  send_status   TINYINT      DEFAULT 1 COMMENT '0失败1成功',
  send_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_phone (phone)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短信记录表';

-- 自定义报表配置（文档 §11.1）
CREATE TABLE IF NOT EXISTS sys_report_config (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  report_name VARCHAR(100) NOT NULL COMMENT '报表名称',
  report_type VARCHAR(50)  DEFAULT 'custom' COMMENT '报表类型',
  query_sql   TEXT         DEFAULT NULL COMMENT '展示用SQL（服务端白名单构建）',
  params      VARCHAR(2000) DEFAULT NULL COMMENT '设计器配置JSON',
  creator_id  BIGINT       DEFAULT NULL COMMENT '创建人',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '报表配置表';

-- 菜单按钮（44-47 段 + 71-76 段）
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(44, '用户维护', 14, '#', 'system:user:edit',  NULL, 3, 1, 1),
(45, '科室维护', 16, '#', 'system:dept:edit',  NULL, 3, 1, 1),
(46, '菜单管理', 1,  'menus', 'system:menu:edit', NULL, 2, 5, 1),
(47, '随访管理', 18, '#', 'patient:followup',  NULL, 3, 1, 1),
(71, '发票管理', 36, '#', 'finance:invoice',   NULL, 3, 2, 1),
(72, '采购管理', 31, '#', 'pharmacy:purchase', NULL, 3, 1, 1),
(73, '妇幼保健', 58, '#', 'health:maternal',   NULL, 3, 1, 1),
(74, '短信发送', 62, '#', 'message:sms',       NULL, 3, 1, 1),
(75, '自定义报表', 60, '#', 'report:custom',    NULL, 3, 2, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m
ON m.id IN (44, 45, 46, 71, 72, 73, 74, 75)
WHERE r.role_code = 'admin';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (47)
WHERE r.role_code IN ('doctor', 'nurse');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 71 FROM sys_role r WHERE r.role_code = 'cashier';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 72 FROM sys_role r WHERE r.role_code = 'pharmacist';

-- 采购供应商种子
INSERT INTO biz_supplier (id, name, contact, phone, address) VALUES
(1, '国药控股深圳分公司', '王经理', '13800009001', '深圳市龙岗区药谷路 1 号'),
(2, '华南医药贸易有限公司', '李经理', '13800009002', '广州市黄埔区医药港 8 号')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 妇幼保健种子
INSERT INTO biz_maternal_record (patient_id, last_period_date, expected_date, checkup_count, status, remark)
SELECT 7, '2026-06-01', '2027-03-08', 2, 1, '孕检记录正常'
WHERE NOT EXISTS (SELECT 1 FROM biz_maternal_record WHERE patient_id = 7);

INSERT INTO biz_child_care (patient_id, birth_weight, height, weight, check_date, remark)
SELECT 11, 3.4, 92.0, 13.5, CURDATE(), '发育正常'
WHERE NOT EXISTS (SELECT 1 FROM biz_child_care WHERE patient_id = 11);
