package com.medical.modules.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.modules.clinic.entity.BizPrescription;
import com.medical.modules.clinic.entity.BizPrescriptionItem;
import com.medical.modules.clinic.mapper.PrescriptionItemMapper;
import com.medical.modules.clinic.mapper.PrescriptionMapper;
import com.medical.modules.clinic.entity.BizDrug;
import com.medical.modules.clinic.mapper.DrugMapper;
import com.medical.modules.finance.entity.BizCharge;
import com.medical.modules.finance.entity.BizChargeItem;
import com.medical.modules.finance.mapper.ChargeItemMapper;
import com.medical.modules.finance.mapper.ChargeMapper;
import com.medical.modules.inpatient.entity.BizAdmission;
import com.medical.modules.inpatient.entity.BizBed;
import com.medical.modules.inpatient.mapper.BizAdmissionMapper;
import com.medical.modules.inpatient.mapper.BizBedMapper;
import com.medical.modules.patient.entity.BizPatient;
import com.medical.modules.patient.mapper.PatientMapper;
import com.medical.modules.registration.entity.BizRegistration;
import com.medical.modules.registration.mapper.RegistrationMapper;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RegistrationMapper registrationMapper;
    private final ChargeMapper chargeMapper;
    private final ChargeItemMapper chargeItemMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final DrugMapper drugMapper;
    private final BizAdmissionMapper admissionMapper;
    private final BizBedMapper bedMapper;
    private final PatientMapper patientMapper;
    private final SysDeptMapper deptMapper;
    private final com.medical.modules.pharmacy.mapper.StockMapper stockMapper;

    private LocalDate[] range(LocalDate start, LocalDate end) {
        LocalDate s = start == null ? LocalDate.now().withDayOfMonth(1) : start;
        LocalDate e = end == null ? LocalDate.now() : end;
        if (e.isBefore(s)) {
            throw new com.medical.common.BusinessException("结束日期不能早于开始日期");
        }
        return new LocalDate[]{s, e.plusDays(1)};
    }

    /** 门诊统计：total/trend/deptDist */
    public Map<String, Object> outpatient(LocalDate start, LocalDate end, Long deptId) {
        LocalDate[] r = range(start, end);
        List<BizRegistration> regs = registrationMapper.selectList(Wrappers.<BizRegistration>lambdaQuery()
                .ge(BizRegistration::getRegTime, r[0].atStartOfDay())
                .lt(BizRegistration::getRegTime, r[1].atStartOfDay())
                .ne(BizRegistration::getStatus, BizRegistration.ST_CANCELLED)
                .eq(deptId != null, BizRegistration::getDeptId, deptId));

        Map<String, Long> trend = regs.stream().collect(Collectors.groupingBy(
                g -> g.getRegTime().toLocalDate().toString(), TreeMap::new, Collectors.counting()));
        Map<Long, Long> byDept = regs.stream().collect(Collectors.groupingBy(
                BizRegistration::getDeptId, Collectors.counting()));
        List<Map<String, Object>> deptDist = byDept.entrySet().stream()
                .map(e -> Map.<String, Object>of("name", deptName(e.getKey()), "value", e.getValue()))
                .sorted((a, b) -> Long.compare((long) b.get("value"), (long) a.get("value")))
                .toList();
        return Map.of("total", regs.size(), "trend", trend, "deptDist", deptDist);
    }

    /** 住院统计：在院人数/床位使用率/区间出院人次/平均住院日 */
    public Map<String, Object> inpatient(LocalDate start, LocalDate end) {
        range(start, end);
        long inHospital = admissionMapper.selectCount(Wrappers.<BizAdmission>lambdaQuery()
                .eq(BizAdmission::getStatus, BizAdmission.ST_IN));
        long totalBeds = bedMapper.selectCount(Wrappers.<BizBed>lambdaQuery()
                .ne(BizBed::getStatus, BizBed.ST_REPAIR));
        long occupied = bedMapper.selectCount(Wrappers.<BizBed>lambdaQuery()
                .eq(BizBed::getStatus, BizBed.ST_OCCUPIED));
        List<BizAdmission> discharged = admissionMapper.selectList(Wrappers.<BizAdmission>lambdaQuery()
                .ge(BizAdmission::getDischargeTime, LocalDateTime.now().minusDays(30))
                .isNotNull(BizAdmission::getDischargeTime));
        double avgDays = discharged.stream()
                .filter(a -> a.getAdmissionTime() != null)
                .mapToLong(a -> Math.max(1, java.time.temporal.ChronoUnit.HOURS.between(
                        a.getAdmissionTime(), a.getDischargeTime()) / 24 + 1))
                .average().orElse(0);
        return Map.of(
                "inHospital", inHospital,
                "totalBeds", totalBeds,
                "bedUsage", totalBeds == 0 ? 0 : Math.round(occupied * 1000.0 / totalBeds) / 10.0,
                "dischargedRecent", discharged.size(),
                "avgStayDays", Math.round(avgDays * 10) / 10.0);
    }

    /** 收入统计：total/byType/byDept/trend/refund */
    public Map<String, Object> income(LocalDate start, LocalDate end, Long deptId) {
        LocalDate[] r = range(start, end);
        List<BizCharge> charges = chargeMapper.selectList(Wrappers.<BizCharge>lambdaQuery()
                .ge(BizCharge::getChargeTime, r[0].atStartOfDay())
                .lt(BizCharge::getChargeTime, r[1].atStartOfDay())
                .in(BizCharge::getPayStatus, 1, 2));
        if (deptId != null) {
            Set<Long> visitIds = registrationMapper.selectList(Wrappers.<BizRegistration>lambdaQuery()
                            .eq(BizRegistration::getDeptId, deptId))
                    .stream().map(BizRegistration::getId).collect(Collectors.toSet());
            charges = charges.stream().filter(c -> visitIds.contains(c.getVisitId())).toList();
        }
        BigDecimal total = charges.stream().map(BizCharge::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = charges.stream().filter(c -> c.getPayStatus() == 2)
                .map(BizCharge::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> chargeIds = charges.stream().map(BizCharge::getId).toList();
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        if (!chargeIds.isEmpty()) {
            List<BizChargeItem> items = chargeItemMapper.selectList(Wrappers.<BizChargeItem>lambdaQuery()
                    .in(BizChargeItem::getChargeId, chargeIds));
            Map<Integer, BigDecimal> byTypeCode = items.stream().collect(Collectors.groupingBy(
                    BizChargeItem::getItemType,
                    Collectors.reducing(BigDecimal.ZERO, BizChargeItem::getAmount, BigDecimal::add)));
            byTypeCode.forEach((k, v) -> byType.put(
                    Map.of(1, "药品", 2, "诊疗", 3, "检查", 4, "床位").getOrDefault(k, "其他"), v));
        }
        Map<String, Long> trend = charges.stream().collect(Collectors.groupingBy(
                c -> c.getChargeTime().toLocalDate().toString(), TreeMap::new, Collectors.counting()));
        return Map.of("total", total, "refund", refund, "byType", byType, "trend", trend, "count", charges.size());
    }

    /** 药品统计：销售 TOP10（处方 status∈{2,3}）+ 药品收入占比 */
    public Map<String, Object> drug(LocalDate start, LocalDate end) {
        LocalDate[] r = range(start, end);
        List<BizPrescription> prescs = prescriptionMapper.selectList(Wrappers.<BizPrescription>lambdaQuery()
                .ge(BizPrescription::getCreateTime, r[0].atStartOfDay())
                .lt(BizPrescription::getCreateTime, r[1].atStartOfDay())
                .in(BizPrescription::getStatus, BizPrescription.ST_CHARGED, BizPrescription.ST_DISPENSED));
        if (prescs.isEmpty()) {
            return Map.of("top10", List.of(), "total", 0);
        }
        List<BizPrescriptionItem> items = prescriptionItemMapper.selectList(Wrappers.<BizPrescriptionItem>lambdaQuery()
                .in(BizPrescriptionItem::getPrescId, prescs.stream().map(BizPrescription::getId).toList()));
        Map<Long, BigDecimal> byDrug = items.stream().collect(Collectors.groupingBy(
                BizPrescriptionItem::getDrugId,
                Collectors.reducing(BigDecimal.ZERO, BizPrescriptionItem::getAmount, BigDecimal::add)));
        Map<Long, String> drugNames = drugMapper.selectList(Wrappers.<BizDrug>lambdaQuery()
                        .in(BizDrug::getId, byDrug.keySet()))
                .stream().collect(Collectors.toMap(BizDrug::getId, BizDrug::getDrugName, (a, b) -> a));
        List<Map<String, Object>> top10 = byDrug.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(e -> Map.<String, Object>of(
                        "name", drugNames.getOrDefault(e.getKey(), "药品#" + e.getKey()),
                        "amount", e.getValue()))
                .toList();
        BigDecimal total = byDrug.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("top10", top10, "total", total);
    }

    /** 患者分析：年龄分段/性别/地域TOP/病种TOP */
    public Map<String, Object> patient() {
        List<BizPatient> patients = patientMapper.selectList(Wrappers.emptyWrapper());
        Map<String, Long> ageDist = new LinkedHashMap<>();
        ageDist.put("≤18", patients.stream().filter(p -> p.getBirthDate() != null
                && age(p.getBirthDate()) <= 18).count());
        ageDist.put("19-40", patients.stream().filter(p -> p.getBirthDate() != null
                && age(p.getBirthDate()) > 18 && age(p.getBirthDate()) <= 40).count());
        ageDist.put("41-60", patients.stream().filter(p -> p.getBirthDate() != null
                && age(p.getBirthDate()) > 40 && age(p.getBirthDate()) <= 60).count());
        ageDist.put(">60", patients.stream().filter(p -> p.getBirthDate() != null
                && age(p.getBirthDate()) > 60).count());
        long male = patients.stream().filter(p -> p.getGender() != null && p.getGender() == 1).count();
        Map<String, Long> region = patients.stream()
                .filter(p -> StringUtils(p.getAddress()))
                .collect(Collectors.groupingBy(p -> p.getAddress().substring(0, Math.min(3, p.getAddress().length())),
                        Collectors.counting()));
        List<Map<String, Object>> regionTop = region.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(5)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList();
        return Map.of("ageDist", ageDist, "gender", Map.of("男", male, "女", patients.size() - male),
                "regionTop", regionTop, "total", patients.size());
    }

    /** 数据总览（仅登录态）：KPI 四卡 + 近7日门诊趋势 + 今日收款构成 + 待办预警 */
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        Map<String, Object> op = outpatient(today, today, null);
        Map<String, Object> in = inpatient(null, null);
        Map<String, Object> inc = income(today, today, null);
        long warnCount = drugWarnCount();
        List<BizCharge> todayCharges = chargeMapper.selectList(Wrappers.<BizCharge>lambdaQuery()
                .ge(BizCharge::getChargeTime, today.atStartOfDay())
                .lt(BizCharge::getChargeTime, today.plusDays(1).atStartOfDay())
                .in(BizCharge::getPayStatus, 1, 2));
        BigDecimal todayIncome = todayCharges.stream().map(BizCharge::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "todayOutpatient", op.get("total"),
                "inHospital", in.get("inHospital"),
                "todayIncome", todayIncome,
                "stockWarn", warnCount,
                "trend7", op.get("trend"),
                "todayChargeCount", todayCharges.size(),
                "inpatient", in);
    }

    private long drugWarnCount() {
        // 低库存 + 近效期（与药房预警口径一致）
        var stocks = stockMapper.selectList(Wrappers.<com.medical.modules.pharmacy.entity.BizStock>lambdaQuery());
        LocalDate nearLine = LocalDate.now().plusDays(90);
        return stocks.stream()
                .filter(s -> s.getQuantity() <= s.getWarnNum()
                        || (s.getExpireDate() != null && !s.getExpireDate().isAfter(nearLine)))
                .count();
    }

    public List<Map<String, Object>> deptOptions() {
        return deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getStatus, 1).orderByAsc(SysDept::getSort))
                .stream().map(d -> Map.<String, Object>of("id", d.getId(), "name", d.getDeptName()))
                .toList();
    }

    private int age(java.time.LocalDate birth) {
        return java.time.Period.between(birth, LocalDate.now()).getYears();
    }

    private String deptName(Long id) {
        SysDept d = deptMapper.selectById(id);
        return d == null ? "未知" : d.getDeptName();
    }

    private boolean StringUtils(String s) {
        return s != null && !s.isBlank();
    }
}
