package com.medical.modules.inpatient.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.inpatient.dto.NursingRequest;
import com.medical.modules.inpatient.dto.OrderRequest;
import com.medical.modules.inpatient.dto.OrderVO;
import com.medical.modules.inpatient.entity.BizAdmission;
import com.medical.modules.inpatient.entity.BizMedicalOrder;
import com.medical.modules.inpatient.entity.BizNursingRecord;
import com.medical.modules.inpatient.mapper.BizAdmissionMapper;
import com.medical.modules.inpatient.mapper.BizMedicalOrderMapper;
import com.medical.modules.inpatient.mapper.BizNursingRecordMapper;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderNursingService {

    private final BizMedicalOrderMapper orderMapper;
    private final BizNursingRecordMapper nursingMapper;
    private final BizAdmissionMapper admissionMapper;
    private final SysUserMapper userMapper;

    /**
     * 开医嘱：锁住院记录校验在院（防并发出院窗口）
     */
    @Transactional
    public Long addOrder(OrderRequest request) {
        BizAdmission adm = admissionMapper.selectById(request.getAdmissionId());
        if (adm == null) {
            throw new BusinessException("住院记录不存在");
        }
        if (adm.getStatus() != BizAdmission.ST_IN) {
            throw new BusinessException("患者已出院或转科，不可开医嘱");
        }
        LoginUser doctor = SecurityUtils.getCurrentUser();
        BizMedicalOrder order = new BizMedicalOrder();
        order.setAdmissionId(request.getAdmissionId());
        order.setOrderType(request.getOrderType() == null ? 1 : request.getOrderType());
        order.setContent(request.getContent());
        order.setDrugId(request.getDrugId());
        order.setDosage(request.getDosage());
        order.setUsageMethod(request.getUsageMethod());
        order.setFrequency(request.getFrequency());
        order.setStartTime(LocalDateTime.now());
        order.setDoctorId(doctor.getId());
        order.setStatus(BizMedicalOrder.ST_PENDING);
        orderMapper.insert(order);
        return order.getId();
    }

    /**
     * 执行医嘱：0未执行→1已执行（单调，仅 0→1）
     */
    @Transactional
    public void execute(Long id) {
        int updated = orderMapper.update(null, Wrappers.<BizMedicalOrder>lambdaUpdate()
                .set(BizMedicalOrder::getStatus, BizMedicalOrder.ST_EXECUTED)
                .eq(BizMedicalOrder::getId, id)
                .eq(BizMedicalOrder::getStatus, BizMedicalOrder.ST_PENDING));
        if (updated == 0) {
            throw new BusinessException("仅未执行医嘱可执行");
        }
    }

    /**
     * 停止医嘱：1已执行→2已停止（未执行不可停止）
     */
    @Transactional
    public void stop(Long id) {
        BizMedicalOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("医嘱不存在");
        }
        if (order.getStatus() != BizMedicalOrder.ST_EXECUTED) {
            throw new BusinessException("仅执行中医嘱可停止");
        }
        int updated = orderMapper.update(null, Wrappers.<BizMedicalOrder>lambdaUpdate()
                .set(BizMedicalOrder::getStatus, BizMedicalOrder.ST_STOPPED)
                .set(BizMedicalOrder::getStopTime, LocalDateTime.now())
                .eq(BizMedicalOrder::getId, id)
                .eq(BizMedicalOrder::getStatus, BizMedicalOrder.ST_EXECUTED));
        if (updated == 0) {
            throw new BusinessException("停止医嘱失败，请刷新重试");
        }
    }

    /**
     * 护理记录：锁住院记录校验在院
     */
    @Transactional
    public Long addNursing(NursingRequest request) {
        BizAdmission adm = admissionMapper.selectById(request.getAdmissionId());
        if (adm == null) {
            throw new BusinessException("住院记录不存在");
        }
        if (adm.getStatus() != BizAdmission.ST_IN) {
            throw new BusinessException("患者已出院或转科，不可新增护理记录");
        }
        LoginUser nurse = SecurityUtils.getCurrentUser();
        BizNursingRecord record = new BizNursingRecord();
        record.setAdmissionId(request.getAdmissionId());
        record.setTemperature(request.getTemperature());
        record.setPulse(request.getPulse());
        record.setBreath(request.getBreath());
        record.setBloodPressure(request.getBloodPressure());
        record.setContent(request.getContent());
        record.setNurseId(nurse.getId());
        record.setRecordTime(LocalDateTime.now());
        nursingMapper.insert(record);
        return record.getId();
    }

    /**
     * 护理记录分页（admissionId 必填）
     */
    public PageResult<BizNursingRecord> nursingPage(PageQuery query, Long admissionId) {
        Page<BizNursingRecord> page = nursingMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<BizNursingRecord>lambdaQuery()
                        .eq(BizNursingRecord::getAdmissionId, admissionId)
                        .orderByDesc(BizNursingRecord::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    private String userName(Long id) {
        if (id == null) return null;
        SysUser u = userMapper.selectById(id);
        if (u == null) return null;
        return StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername();
    }
}
