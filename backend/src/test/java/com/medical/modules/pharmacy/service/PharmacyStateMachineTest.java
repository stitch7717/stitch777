package com.medical.modules.pharmacy.service;

import com.medical.common.BusinessException;
import com.medical.framework.LoginUser;
import com.medical.modules.clinic.entity.BizDrug;
import com.medical.modules.clinic.entity.BizPrescription;
import com.medical.modules.clinic.entity.BizPrescriptionItem;
import com.medical.modules.clinic.mapper.DrugMapper;
import com.medical.modules.clinic.mapper.PrescriptionItemMapper;
import com.medical.modules.clinic.mapper.PrescriptionMapper;
import com.medical.modules.clinic.service.PrescriptionService;
import com.medical.modules.pharmacy.dto.StockRequest;
import com.medical.modules.pharmacy.entity.BizStock;
import com.medical.modules.pharmacy.mapper.StockMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 药房状态机：处方审核 / 并发发药 / 库存不足，验证条件更新与库存不为负。
 */
@SpringBootTest
@ActiveProfiles("test")
class PharmacyStateMachineTest {

    private static final int THREADS = 2;

    @Autowired
    private StockService stockService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private DrugMapper drugMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Autowired
    private PrescriptionItemMapper itemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> prescriptionIds = new ArrayList<>();
    private Long drugId;

    @BeforeEach
    void setUp() {
        cleanTestData();

        BizDrug drug = new BizDrug();
        drug.setDrugCode("TEST-" + System.nanoTime());
        drug.setDrugName("状态机测试药品");
        drug.setSalePrice(new BigDecimal("10.00"));
        drug.setStatus(1);
        drugMapper.insert(drug);
        drugId = drug.getId();

        BizStock stock = new BizStock();
        stock.setDrugId(drugId);
        stock.setQuantity(10);
        stock.setWarnNum(0);
        stockMapper.insert(stock);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    @Test
    @DisplayName("处方审核：待审核 → 已审核；重复审核返回 409")
    void auditPrescription_twice_secondConflicts() {
        Long prescId = newPrescription(BizPrescription.ST_PENDING, "AUDIT");

        prescriptionService.audit(prescId);

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM biz_prescription WHERE id = ?", Integer.class, prescId);
        assertThat(status).isEqualTo(BizPrescription.ST_AUDITED);

        assertThatThrownBy(() -> prescriptionService.audit(prescId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(409));
    }

    @Test
    @DisplayName("并发发药两张各需 10 的处方：仅 1 张成功，库存归零且不为负")
    void concurrentDispense_onlyOneSucceeds() throws Exception {
        String prescNo1 = newPrescriptionWithItem(BizPrescription.ST_AUDITED, 10, "DISPENSE-1");
        String prescNo2 = newPrescriptionWithItem(BizPrescription.ST_AUDITED, 10, "DISPENSE-2");

        Map<String, Integer> result = runConcurrently(prescNo1, prescNo2);

        assertThat(result.get("success")).isEqualTo(1);
        assertThat(result.get("conflict")).isEqualTo(1);

        Integer quantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM biz_stock WHERE drug_id = ?", Integer.class, drugId);
        assertThat(quantity).isZero();

        Integer dispensed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_prescription WHERE presc_no IN (?, ?) AND status = 3",
                Integer.class, prescNo1, prescNo2);
        assertThat(dispensed).isEqualTo(1);
    }

    @Test
    @DisplayName("入库 + 出库：数量原子加减")
    void stockInThenOut_updatesQuantity() {
        SecurityContextHolder.getContext().setAuthentication(authentication());
        try {
            StockRequest in = new StockRequest();
            in.setDrugId(drugId);
            in.setQuantity(5);
            stockService.stockIn(in);

            Integer afterIn = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM biz_stock WHERE drug_id = ?", Integer.class, drugId);
            assertThat(afterIn).isEqualTo(15);

            StockRequest out = new StockRequest();
            out.setDrugId(drugId);
            out.setQuantity(3);
            stockService.stockOut(out);

            Integer afterOut = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM biz_stock WHERE drug_id = ?", Integer.class, drugId);
            assertThat(afterOut).isEqualTo(12);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("发药后退药：处方状态 3 → 4，库存回补")
    void dispenseThenReturn_restoresStock() {
        String prescNo = newPrescriptionWithItem(BizPrescription.ST_AUDITED, 4, "RETURN");

        SecurityContextHolder.getContext().setAuthentication(authentication());
        try {
            stockService.dispense(prescNo);
            assertThat(stockQuantity()).isEqualTo(6);

            stockService.returnDrug(prescNo);
            assertThat(stockQuantity()).isEqualTo(10);
        } finally {
            SecurityContextHolder.clearContext();
        }

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM biz_prescription WHERE presc_no = ?", Integer.class, prescNo);
        assertThat(status).isEqualTo(BizPrescription.ST_RETURNED);
    }

    @Test
    @DisplayName("库存不足时出库返回 409，库存不变")
    void stockOut_insufficient_returnsConflict() {
        StockRequest request = new StockRequest();
        request.setDrugId(drugId);
        request.setQuantity(11);

        assertThatThrownBy(() -> stockService.stockOut(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(409));

        Integer quantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM biz_stock WHERE drug_id = ?", Integer.class, drugId);
        assertThat(quantity).isEqualTo(10);
    }

    private Map<String, Integer> runConcurrently(String prescNo1, String prescNo2) throws Exception {
        List<String> prescNos = List.of(prescNo1, prescNo2);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            String prescNo = prescNos.get(i % 2);
            futures.add(pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(authentication());
                ready.countDown();
                start.await();
                try {
                    stockService.dispense(prescNo);
                    return 200;
                } catch (BusinessException e) {
                    return e.getCode();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int success = 0;
        int conflict = 0;
        for (Future<Integer> future : futures) {
            int code = future.get(30, TimeUnit.SECONDS);
            if (code == 200) {
                success++;
            } else if (code == 409) {
                conflict++;
            }
        }
        pool.shutdown();
        return Map.of("success", success, "conflict", conflict);
    }

    private Long newPrescription(int status, String tag) {
        BizPrescription presc = new BizPrescription();
        presc.setPrescNo("TST-" + tag + "-" + (System.nanoTime() % 1_000_000_000L));
        presc.setPatientId(1L);
        presc.setDoctorId(2L);
        presc.setVisitId(1L);
        presc.setPrescType(1);
        presc.setTotalAmount(new BigDecimal("10.00"));
        presc.setStatus(status);
        prescriptionMapper.insert(presc);
        prescriptionIds.add(presc.getId());
        return presc.getId();
    }

    private String newPrescriptionWithItem(int status, int quantity, String tag) {
        Long prescId = newPrescription(status, tag);
        String prescNo = jdbcTemplate.queryForObject(
                "SELECT presc_no FROM biz_prescription WHERE id = ?", String.class, prescId);

        BizPrescriptionItem item = new BizPrescriptionItem();
        item.setPrescId(prescId);
        item.setDrugId(drugId);
        item.setDrugName("状态机测试药品");
        item.setQuantity(quantity);
        item.setPrice(new BigDecimal("10.00"));
        item.setAmount(new BigDecimal("10.00").multiply(BigDecimal.valueOf(quantity)));
        itemMapper.insert(item);
        return prescNo;
    }

    private Integer stockQuantity() {
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM biz_stock WHERE drug_id = ?", Integer.class, drugId);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        LoginUser user = new LoginUser(1L, "admin", "系统管理员", 1L, "admin");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private void cleanTestData() {
        jdbcTemplate.update("DELETE FROM biz_stock_record WHERE drug_id IN (SELECT id FROM biz_drug WHERE drug_code LIKE 'TEST-%')");
        jdbcTemplate.update("DELETE FROM biz_prescription_item WHERE drug_id IN (SELECT id FROM biz_drug WHERE drug_code LIKE 'TEST-%')");
        jdbcTemplate.update("DELETE FROM biz_prescription WHERE presc_no LIKE 'TST-%'");
        jdbcTemplate.update("DELETE FROM biz_stock WHERE drug_id IN (SELECT id FROM biz_drug WHERE drug_code LIKE 'TEST-%')");
        jdbcTemplate.update("DELETE FROM biz_drug WHERE drug_code LIKE 'TEST-%'");
        prescriptionIds.clear();
    }
}
