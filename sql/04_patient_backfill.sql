-- ============================================================
-- 修复：种子患者编号/卡号回补（编号在 create 时才生成，种子数据需补）
-- ============================================================
USE medical;

UPDATE biz_patient SET patient_no = CONCAT('P', LPAD(id, 5, '0')),
                       card_no = CONCAT('C1', LPAD(id, 4, '0'))
WHERE patient_no IS NULL OR card_no IS NULL;
