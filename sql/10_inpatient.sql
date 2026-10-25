-- ============================================================
-- 医疗后台管理系统 · 住院管理模块（阶段4，接口文档 §六）
-- ward_id 复用 sys_dept 科室ID（科室即病区）；床位号病区作用域唯一
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 床位表 biz_bed
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_bed (
  id        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  ward_id   BIGINT        NOT NULL COMMENT '病区ID（复用科室ID）',
  bed_no    VARCHAR(20)   NOT NULL COMMENT '床位号',
  bed_type  TINYINT       DEFAULT 1 COMMENT '1普通2重症3VIP',
  price     DECIMAL(10,2) NOT NULL COMMENT '床位费/天',
  status    TINYINT       DEFAULT 0 COMMENT '0空闲1占用2维修',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted   TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ward_bed_no (ward_id, bed_no)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '床位表';

-- ------------------------------------------------------------
-- 住院登记表 biz_admission（转科=原记录终结+新建在院记录）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_admission (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  admission_no   VARCHAR(30)   DEFAULT NULL COMMENT '住院号 ZY+日期+序号',
  patient_id     BIGINT        NOT NULL COMMENT '患者ID',
  patient_name   VARCHAR(50)   DEFAULT NULL COMMENT '姓名快照',
  dept_id        BIGINT        NOT NULL COMMENT '科室（病区）ID',
  bed_id         BIGINT        NOT NULL COMMENT '床位ID',
  doctor_id      BIGINT        DEFAULT NULL COMMENT '主治医生',
  nurse_id       BIGINT        DEFAULT NULL COMMENT '责任护士',
  admission_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '入院时间',
  discharge_time DATETIME      DEFAULT NULL COMMENT '出院/转出时间',
  diagnosis      VARCHAR(500)  DEFAULT NULL COMMENT '入院诊断',
  deposit        DECIMAL(10,2) DEFAULT 0.00 COMMENT '押金',
  status         TINYINT       DEFAULT 0 COMMENT '0在院1出院2转科（终结态）',
  create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT       DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_admission_no (admission_no),
  KEY idx_patient_status (patient_id, status),
  KEY idx_bed (bed_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '住院登记表';

-- ------------------------------------------------------------
-- 医嘱表 biz_medical_order（0未执行→1已执行→2已停止 单调）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_medical_order (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  admission_id BIGINT       NOT NULL COMMENT '住院ID',
  order_type   TINYINT      DEFAULT 1 COMMENT '1长期2临时',
  content      VARCHAR(500) NOT NULL COMMENT '医嘱内容',
  drug_id      BIGINT       DEFAULT NULL COMMENT '药品ID（可选）',
  dosage       VARCHAR(50)  DEFAULT NULL COMMENT '用量',
  usage_method VARCHAR(20)  DEFAULT NULL COMMENT '用法：口服/静滴等',
  frequency    VARCHAR(20)  DEFAULT NULL COMMENT '频次',
  start_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  stop_time    DATETIME     DEFAULT NULL COMMENT '停止时间',
  doctor_id    BIGINT       NOT NULL COMMENT '开嘱医生',
  status       TINYINT      DEFAULT 0 COMMENT '0未执行1已执行2已停止',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_admission (admission_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医嘱表';

-- ------------------------------------------------------------
-- 护理记录表 biz_nursing_record（计划书第八章补建）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_nursing_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  admission_id   BIGINT       NOT NULL COMMENT '住院ID',
  record_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  temperature    DECIMAL(4,1) DEFAULT NULL COMMENT '体温℃',
  pulse          INT          DEFAULT NULL COMMENT '脉搏次/分',
  breath         INT          DEFAULT NULL COMMENT '呼吸次/分',
  blood_pressure VARCHAR(20)  DEFAULT NULL COMMENT '血压 如120/80',
  content        VARCHAR(500) DEFAULT NULL COMMENT '护理内容',
  nurse_id       BIGINT       NOT NULL COMMENT '记录护士',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删1已删',
  PRIMARY KEY (id),
  KEY idx_admission (admission_id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '护理记录表';

-- ------------------------------------------------------------
-- 菜单：住院管理（parent_id=9）：列表 49 + 按钮四件套 51-55（文档 §6.2⑧）
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status) VALUES
(49, '床位一览', 9, 'beds', 'inpatient:list',        'Bed', 2, 1, 1),
(50, '入院登记', 9, '#',    'inpatient:admission:add', NULL, 3, 1, 1),
(51, '出院转科', 9, '#',    'inpatient:admission:edit', NULL, 3, 2, 1),
(52, '开医嘱',   9, '#',    'inpatient:order:add',     NULL, 3, 3, 1),
(53, '医嘱执行', 9, '#',    'inpatient:order:operate', NULL, 3, 4, 1),
(54, '护理记录', 9, '#',    'inpatient:nursing:add',   NULL, 3, 5, 1);

-- 角色分配：admin 全量；doctor 入院/出院/转科+开医嘱；nurse 医嘱执行+护理；dean 只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (49, 50, 51, 52, 53, 54)
WHERE r.role_code IN ('admin');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (49, 50, 51, 52)
WHERE r.role_code = 'doctor';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (49, 53, 54)
WHERE r.role_code = 'nurse';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 49 FROM sys_role r WHERE r.role_code = 'dean';

-- ------------------------------------------------------------
-- 床位种子：内科/外科 各 8 张床
-- ------------------------------------------------------------
INSERT INTO biz_bed (ward_id, bed_no, bed_type, price, status)
SELECT 1, CONCAT('A', LPAD(n, 2, '0')), IF(n <= 6, 1, 2), 50.00, 0
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) t
WHERE NOT EXISTS (SELECT 1 FROM biz_bed WHERE ward_id = 1);

INSERT INTO biz_bed (ward_id, bed_no, bed_type, price, status)
SELECT 2, CONCAT('B', LPAD(n, 2, '0')), IF(n <= 6, 1, 2), 50.00, 0
FROM (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) t
WHERE NOT EXISTS (SELECT 1 FROM biz_bed WHERE ward_id = 2);
