-- ============================================================
-- 补充种子：更多医生/护士/患者（密码统一 123456）
-- ============================================================
USE medical;

-- ---------------- 医生（role_id=2） ----------------
INSERT INTO sys_user (id, username, password, real_name, gender, phone, email, dept_id, role_id, status) VALUES
(7,  'doc02',    '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '李外主任', 1, '13800000102', 'doc02@hospital.com',   2, 2, 1),
(8,  'doc03',    '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '王儿科',   0, '13800000103', 'doc03@hospital.com',   3, 2, 1),
(9,  'doc04',    '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '赵急诊',   1, '13800000104', 'doc04@hospital.com',   5, 2, 1),
(10, 'doc05',    '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '钱内科',   0, '13800000105', 'doc05@hospital.com',   1, 2, 1);

-- ---------------- 护士（role_id=3） ----------------
INSERT INTO sys_user (id, username, password, real_name, gender, phone, email, dept_id, role_id, status) VALUES
(11, 'nurse02',  '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '孙外科护士', 0, '13800000202', 'nurse02@hospital.com', 2, 3, 1),
(12, 'nurse03',  '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '周儿科护士', 0, '13800000203', 'nurse03@hospital.com', 3, 3, 1),
(13, 'nurse04',  '$2a$10$vTgKJi9m9MHc2ZtwZY26Sem0G6811wCpHzw17cJc3uZEY1aLdqMIa', '吴内科护士', 1, '13800000204', 'nurse04@hospital.com', 1, 3, 1);

-- ---------------- 患者（编号/卡号按既有规则） ----------------
INSERT INTO biz_patient (id, patient_no, name, gender, birth_date, id_card, phone, address, blood_type, allergy_history, medical_history, card_no, card_status, tags) VALUES
(6,  'P00006', '刘建国', 1, '1965-02-18', '440301196502181111', '13611112222', '深圳市福田区香蜜湖 1 号',   'A',  NULL,        '高血压病史10年',        'C10006', 1, '高风险,慢病'),
(7,  'P00007', '陈美玲', 0, '1988-07-22', '440301198807222222', '13622223333', '深圳市南山区蛇口 6 号',     'B',  '花粉过敏',  NULL,                    'C10007', 1, 'VIP'),
(8,  'P00008', '杨志强', 1, '1972-11-05', '440301197211053333', '13633334444', '深圳市宝安区西乡 15 号',    'O',  NULL,        '2型糖尿病',             'C10008', 1, '慢病'),
(9,  'P00009', '赵小燕', 0, '1995-03-30', '440301199503304444', '13644445555', '深圳市罗湖区莲塘 3 号',     'AB', '青霉素过敏', NULL,                    'C10009', 1, ''),
(10, 'P00010', '孙大爷', 1, '1948-09-12', '440301194809125555', '13655556666', '深圳市龙岗区布吉 8 号',     'A',  '磺胺过敏',  '冠心病,高血压',         'C10010', 1, '高风险,慢病'),
(11, 'P00011', '周小雨', 0, '2018-06-15', '440301201806156666', '13666667777', '深圳市福田区景田 2 号',     'O',  NULL,        NULL,                    'C10011', 1, ''),
(12, 'P00012', '吴大勇', 1, '1980-12-08', '440301198012087777', '13677778888', '深圳市南山区科技园 9 号',   'B',  NULL,        '既往阑尾炎手术',        'C10012', 1, 'VIP,慢病'),
(13, 'P00013', '郑淑芬', 0, '1956-04-25', '440301195604258888', '13688889999', '深圳市盐田区沙头角 4 号',   'A',  NULL,        '慢性支气管炎',          'C10013', 1, '慢病');
