-- ============================================================
-- 17_hardening.sql · 并发正确性 / 敏感字段 / 会话失效
-- 在 01~16 之后执行一次（MySQL 8.0）
-- ============================================================
USE medical;

-- ------------------------------------------------------------
-- 1. 排队号计数器：按「科室 + 日期」原子自增
--    原实现用 MAX(queue_no)+1，并发下会取到相同值
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_queue_seq (
  dept_id    BIGINT NOT NULL COMMENT '科室ID',
  seq_date   DATE   NOT NULL COMMENT '排队日期',
  current_no INT    NOT NULL DEFAULT 0 COMMENT '当前已分配到的排队号',
  PRIMARY KEY (dept_id, seq_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '排队号计数器';

-- 历史数据重排：同一科室同一天按 id 顺序重新编号，保证唯一
UPDATE biz_registration r
JOIN (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY dept_id, DATE(reg_time) ORDER BY id) AS rn
  FROM biz_registration
  WHERE deleted = 0
) t ON t.id = r.id
SET r.queue_no = t.rn;

-- 历史重复挂号：同一患者同一排班只保留最早一条有效记录
UPDATE biz_registration r
JOIN (
  SELECT id FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY patient_id, schedule_id ORDER BY id) AS rn
    FROM biz_registration
    WHERE deleted = 0 AND status IN (0, 1, 2)
  ) d WHERE d.rn > 1
) t ON t.id = r.id
SET r.deleted = 1;

-- 用历史最大值初始化计数器
INSERT INTO biz_queue_seq (dept_id, seq_date, current_no)
SELECT dept_id, DATE(reg_time), MAX(queue_no)
FROM biz_registration
WHERE deleted = 0
GROUP BY dept_id, DATE(reg_time)
ON DUPLICATE KEY UPDATE current_no = GREATEST(current_no, VALUES(current_no));

-- ------------------------------------------------------------
-- 2. 唯一约束：从数据库层兜底并发重复
-- ------------------------------------------------------------
ALTER TABLE biz_registration
  ADD COLUMN reg_date DATE GENERATED ALWAYS AS (DATE(reg_time)) STORED
    COMMENT '挂号日期（生成列，用于唯一约束）',
  ADD COLUMN active_flag TINYINT GENERATED ALWAYS AS (
    CASE WHEN deleted = 0 AND status IN (0, 1, 2) THEN 1 ELSE NULL END
  ) STORED COMMENT '有效挂号标记（退号/删除为 NULL，供唯一约束使用）',
  ADD UNIQUE KEY uk_dept_date_queue (dept_id, reg_date, queue_no),
  ADD UNIQUE KEY uk_patient_schedule_active (patient_id, schedule_id, active_flag);

-- ------------------------------------------------------------
-- 3. 患者身份证：列宽 + 等值检索列
--    AES/ECB 密文约 48 字符，原 VARCHAR(20) 装不下
-- ------------------------------------------------------------
ALTER TABLE biz_patient
  MODIFY COLUMN id_card VARCHAR(255) DEFAULT NULL COMMENT '身份证号（AES-256-GCM 加密）',
  ADD COLUMN id_card_hash CHAR(64) DEFAULT NULL COMMENT '身份证 HMAC-SHA256，用于等值检索',
  ADD KEY idx_id_card_hash (id_card_hash);

-- ------------------------------------------------------------
-- 4. JWT 会话版本号：改密/强制下线后旧 token 立即失效
-- ------------------------------------------------------------
ALTER TABLE sys_user
  ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT 'token 版本号，改密时 +1';

-- ------------------------------------------------------------
-- 5. 公告管理权限（原先任何登录用户都能发布/删除公告）
-- ------------------------------------------------------------
INSERT INTO sys_menu (id, menu_name, parent_id, path, perms, icon, menu_type, sort, status)
VALUES (77, '公告管理', 62, '#', 'message:notice', NULL, 3, 2, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 77 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 77);
