-- ============================================================
-- 医疗后台管理系统 · 挂号与预约模块（阶段3）
-- 排班表 + 挂号记录表（patient_name 快照 / creator_id 扩展列）
-- 号源扣减采用 DB 原子 UPDATE（remain_num>0 AND status=1，防超卖）
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 排班表 biz_schedule（接口文档 §4.1）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_schedule (
  id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  doctor_id     BIGINT        NOT NULL COMMENT '医生ID',
  dept_id       BIGINT        NOT NULL COMMENT '科室ID',
  schedule_date DATE          NOT NULL COMMENT '排班日期',
  time_period   TINYINT       NOT NULL COMMENT '时段 1上午2下午3晚上',
  reg_type      TINYINT       NOT NULL COMMENT '号别 1普通2专家3急诊',
  total_num     INT           NOT NULL COMMENT '总号源',
  remain_num    INT           NOT NULL COMMENT '剩余号源',
  reg_fee       DECIMAL(10,2) NOT NULL COMMENT '挂号费',
  status        TINYINT       DEFAULT 1 COMMENT '状态 0停诊1正常',
  create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_date_dept (schedule_date, dept_id),
  KEY idx_doctor (doctor_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '排班表';

-- ------------------------------------------------------------
-- 挂号记录表 biz_registration
-- queue_no：当日该科室按序排队号（退号不回收）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_registration (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  reg_no      VARCHAR(30)   DEFAULT NULL COMMENT '挂号单号 REG+日期+序号',
  patient_id  BIGINT        NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(50)  DEFAULT NULL COMMENT '患者姓名快照（凭证语义）',
  doctor_id   BIGINT        NOT NULL COMMENT '医生ID',
  dept_id     BIGINT        NOT NULL COMMENT '科室ID',
  schedule_id BIGINT        NOT NULL COMMENT '排班ID',
  reg_type    TINYINT       NOT NULL COMMENT '挂号类型 1普通2专家3急诊',
  reg_fee     DECIMAL(10,2) NOT NULL COMMENT '挂号费快照',
  queue_no    INT           NOT NULL COMMENT '当日科室排队号',
  status      TINYINT       DEFAULT 0 COMMENT '0待就诊1就诊中2已完成3已退号',
  reg_time    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '挂号时间',
  creator_id  BIGINT         DEFAULT NULL COMMENT '操作人（扩展列）',
  create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_reg_no (reg_no),
  KEY idx_dept_date (dept_id, reg_time),
  KEY idx_patient (patient_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '挂号记录表';

-- ------------------------------------------------------------
-- 菜单：现场挂号（parent_id=3 挂号预约）+ 按钮
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(23, '现场挂号', 3, 'walkin', 'registration:list',     'Postcard', 2, 1, 1),
(24, '挂号退号', 23, '#',      'registration:register', NULL, 3, 1, 1),
(25, '排班管理', 23, '#',      'registration:schedule', NULL, 3, 2, 1),
(26, '叫号',    23, '#',      'registration:call',     NULL, 3, 3, 1);

-- 角色分配：admin 全量；cashier 挂号窗口全操作；doctor 队列+叫号；dean 只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (23, 24, 25, 26)
WHERE r.role_code IN ('admin', 'cashier');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (23, 26)
WHERE r.role_code = 'doctor';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 23 FROM sys_role r WHERE r.role_code = 'dean';

-- ------------------------------------------------------------
-- 种子：今日/明日排班（doc01 内科，日期用 CURDATE() 保证可联调）
-- ------------------------------------------------------------
INSERT INTO biz_schedule (doctor_id, dept_id, schedule_date, time_period, reg_type, total_num, remain_num, reg_fee, status)
SELECT 2, 1, CURDATE(),   1, 2, 20, 20, 50.00, 1
WHERE NOT EXISTS (SELECT 1 FROM biz_schedule WHERE doctor_id = 2 AND schedule_date = CURDATE() AND time_period = 1);

INSERT INTO biz_schedule (doctor_id, dept_id, schedule_date, time_period, reg_type, total_num, remain_num, reg_fee, status)
SELECT 2, 1, CURDATE(),   2, 1, 30, 30, 20.00, 1
WHERE NOT EXISTS (SELECT 1 FROM biz_schedule WHERE doctor_id = 2 AND schedule_date = CURDATE() AND time_period = 2);

INSERT INTO biz_schedule (doctor_id, dept_id, schedule_date, time_period, reg_type, total_num, remain_num, reg_fee, status)
SELECT 2, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 2, 20, 20, 50.00, 1
WHERE NOT EXISTS (SELECT 1 FROM biz_schedule WHERE doctor_id = 2 AND schedule_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND time_period = 1);
