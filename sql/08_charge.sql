-- ============================================================
-- 医疗后台管理系统 · 收费与财务管理模块（阶段3）
-- 整单收费：挂号费（首单含）+ 已审核处方明细，一笔 biz_charge
-- 发票/医保/月结留后续阶段
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 收费单 biz_charge
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_charge (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  charge_no       VARCHAR(30)   DEFAULT NULL COMMENT '收费单号 CH+日期+序号',
  patient_id      BIGINT        NOT NULL COMMENT '患者ID',
  patient_name    VARCHAR(50)   DEFAULT NULL COMMENT '患者姓名快照（凭证）',
  visit_id        BIGINT        DEFAULT NULL COMMENT '就诊ID（挂号记录ID）',
  charge_type     TINYINT       DEFAULT 2 COMMENT '1挂号2门诊3住院（住院预留）',
  total_amount    DECIMAL(10,2) NOT NULL COMMENT '总金额（服务端重算）',
  discount_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  actual_amount   DECIMAL(10,2) NOT NULL COMMENT '实收金额',
  pay_type        TINYINT       DEFAULT 2 COMMENT '1现金2微信3支付宝4医保5银行卡',
  pay_status      TINYINT       DEFAULT 1 COMMENT '0待支付1已支付2已退费',
  operator_id     BIGINT        DEFAULT NULL COMMENT '收费员',
  charge_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '收费时间',
  refund_time     DATETIME      DEFAULT NULL COMMENT '退费时间（扩展列）',
  refund_operator_id BIGINT     DEFAULT NULL COMMENT '退费操作人（扩展列）',
  create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_charge_no (charge_no),
  KEY idx_visit (visit_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费单';

-- ------------------------------------------------------------
-- 收费明细 biz_charge_item（source_id：处方/项目 ID，退费精确回退）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_charge_item (
  id        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  charge_id BIGINT        NOT NULL COMMENT '收费单ID',
  item_type TINYINT       NOT NULL COMMENT '1药品2诊疗3检查4床位',
  item_name VARCHAR(100)  NOT NULL COMMENT '项目名称',
  source_id BIGINT        DEFAULT NULL COMMENT '处方/来源单ID（扩展列）',
  quantity  INT           DEFAULT 1 COMMENT '数量',
  price     DECIMAL(10,2) NOT NULL COMMENT '单价',
  amount    DECIMAL(10,2) NOT NULL COMMENT '金额',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted   TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_charge (charge_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费明细';

-- ------------------------------------------------------------
-- 菜单：门诊收费（parent_id=6 收费财务）+ 按钮
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(36, '门诊收费', 6, 'charge', 'charge:list',    'Money', 2, 1, 1),
(37, '收费退费', 36, '#',      'charge:operate', NULL, 3, 1, 1);

-- 角色分配：admin 全量；cashier 全量；dean 只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (36, 37)
WHERE r.role_code IN ('admin', 'cashier');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 36 FROM sys_role r WHERE r.role_code = 'dean';
