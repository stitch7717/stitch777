-- ============================================================
-- 医疗后台管理系统 · 药品与药房管理模块（阶段3）
-- 库存模型一药一行（uk_drug_id），入库 upsert 原子累加，出库条件内扣防负库存
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 库存表 biz_stock（一药一行）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_stock (
  id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  drug_id      BIGINT   NOT NULL COMMENT '药品ID',
  quantity     INT      DEFAULT 0 COMMENT '库存数量',
  warn_num     INT      DEFAULT 50 COMMENT '预警数量',
  produce_date DATE     DEFAULT NULL COMMENT '生产日期',
  expire_date  DATE     DEFAULT NULL COMMENT '有效期',
  warehouse_id BIGINT   DEFAULT NULL COMMENT '库房ID（预留）',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT  DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_drug_id (drug_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品库存表';

-- ------------------------------------------------------------
-- 出入库记录 biz_stock_record（1入库 2出库 3盘点预留）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_stock_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  drug_id     BIGINT       NOT NULL COMMENT '药品ID',
  record_type TINYINT      NOT NULL COMMENT '1入库 2出库 3盘点',
  quantity    INT          NOT NULL COMMENT '数量',
  batch_no    VARCHAR(50)  DEFAULT NULL COMMENT '批号',
  operator_id BIGINT       DEFAULT NULL COMMENT '操作人',
  remark      VARCHAR(255) DEFAULT NULL COMMENT '备注（发药/退药关联处方号）',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_drug (drug_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '出入库记录表';

-- ------------------------------------------------------------
-- 菜单：药品库存（parent_id=5 药房管理）+ 按钮
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(31, '药品库存', 5, 'stock',   'pharmacy:list',            'Box', 2, 1, 1),
(32, '药品管理', 31, '#',      'pharmacy:drug',            NULL, 3, 1, 1),
(33, '出入库',   31, '#',      'pharmacy:stock',           NULL, 3, 2, 1),
(34, '门诊发药', 31, '#',      'pharmacy:dispense:add',    NULL, 3, 3, 1),
(35, '退药',     31, '#',      'pharmacy:dispense:return', NULL, 3, 4, 1);

-- 角色分配：admin 全量；pharmacist 药房全量；dean 只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (31, 32, 33, 34, 35)
WHERE r.role_code IN ('admin', 'pharmacist');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 31 FROM sys_role r WHERE r.role_code = 'dean';

-- ------------------------------------------------------------
-- 种子库存（布洛芬 80 < 预警 100 触发库存预警演示；氯化钠 2026-01 过期演示近效期）
-- ------------------------------------------------------------
INSERT INTO biz_stock (drug_id, quantity, warn_num, expire_date, produce_date)
SELECT 1, 500, 100, '2027-06-30', '2026-01-01' WHERE NOT EXISTS (SELECT 1 FROM biz_stock WHERE drug_id = 1);
INSERT INTO biz_stock (drug_id, quantity, warn_num, expire_date, produce_date)
SELECT 2, 80, 100, '2027-03-15', '2025-09-01' WHERE NOT EXISTS (SELECT 1 FROM biz_stock WHERE drug_id = 2);
INSERT INTO biz_stock (drug_id, quantity, warn_num, expire_date, produce_date)
SELECT 3, 200, 50, '2027-09-30', '2026-04-01' WHERE NOT EXISTS (SELECT 1 FROM biz_stock WHERE drug_id = 3);
INSERT INTO biz_stock (drug_id, quantity, warn_num, expire_date, produce_date)
SELECT 4, 150, 50, '2026-12-31', '2026-02-01' WHERE NOT EXISTS (SELECT 1 FROM biz_stock WHERE drug_id = 4);
INSERT INTO biz_stock (drug_id, quantity, warn_num, expire_date, produce_date)
SELECT 5, 300, 100, '2026-01-10', '2025-06-01' WHERE NOT EXISTS (SELECT 1 FROM biz_stock WHERE drug_id = 5);
