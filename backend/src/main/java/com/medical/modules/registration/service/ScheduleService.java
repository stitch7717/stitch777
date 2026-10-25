package com.medical.modules.registration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.registration.dto.ScheduleRequest;
import com.medical.modules.registration.dto.ScheduleVO;
import com.medical.modules.registration.entity.BizRegistration;
import com.medical.modules.registration.entity.BizSchedule;
import com.medical.modules.registration.mapper.RegistrationMapper;
import com.medical.modules.registration.mapper.ScheduleMapper;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ScheduleMapper scheduleMapper;
    private final RegistrationMapper registrationMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 排班查询：日期区间/科室/医生可选，默认从今日起
     */
    public List<ScheduleVO> list(LocalDate startDate, LocalDate endDate, Long deptId, Long doctorId) {
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        List<BizSchedule> list = scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .ge(BizSchedule::getScheduleDate, start)
                .le(endDate != null, BizSchedule::getScheduleDate, endDate)
                .eq(deptId != null, BizSchedule::getDeptId, deptId)
                .eq(doctorId != null, BizSchedule::getDoctorId, doctorId)
                .orderByAsc(BizSchedule::getScheduleDate)
                .orderByAsc(BizSchedule::getTimePeriod));

        Map<Long, String> doctorNames = userMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u.getRealName() == null ? u.getUsername() : u.getRealName(), (a, b) -> a));
        Map<Long, String> deptNames = deptMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        return list.stream().map(s -> {
            ScheduleVO vo = new ScheduleVO();
            vo.setId(s.getId());
            vo.setDoctorId(s.getDoctorId());
            vo.setDoctorName(doctorNames.get(s.getDoctorId()));
            vo.setDeptId(s.getDeptId());
            vo.setDeptName(deptNames.get(s.getDeptId()));
            vo.setScheduleDate(s.getScheduleDate().format(DATE_FMT));
            vo.setTimePeriod(s.getTimePeriod());
            vo.setRegType(s.getRegType());
            vo.setTotalNum(s.getTotalNum());
            vo.setRemainNum(s.getRemainNum());
            vo.setRegFee(s.getRegFee());
            vo.setStatus(s.getStatus());
            return vo;
        }).toList();
    }

    @Transactional
    public Long create(ScheduleRequest request) {
        if (request.getScheduleDate().isBefore(LocalDate.now())) {
            throw new BusinessException("排班日期不能早于今日");
        }
        if (request.getTimePeriod() == null || request.getTimePeriod() < 1 || request.getTimePeriod() > 4) {
            throw new BusinessException("时段仅支持：1上午(8:00-10:00) 2上午(10:00-12:00) 3下午(14:00-16:00) 4下午(16:00-18:00)");
        }
        Long dup = scheduleMapper.selectCount(Wrappers.<BizSchedule>lambdaQuery()
                .eq(BizSchedule::getDoctorId, request.getDoctorId())
                .eq(BizSchedule::getScheduleDate, request.getScheduleDate())
                .eq(BizSchedule::getTimePeriod, request.getTimePeriod()));
        if (dup > 0) {
            throw new BusinessException("该医生当日该时段已有排班");
        }
        BizSchedule schedule = new BizSchedule();
        schedule.setDoctorId(request.getDoctorId());
        schedule.setDeptId(request.getDeptId());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setTimePeriod(request.getTimePeriod());
        schedule.setRegType(request.getRegType());
        schedule.setTotalNum(request.getTotalNum());
        schedule.setRemainNum(request.getTotalNum());
        schedule.setRegFee(request.getRegFee());
        schedule.setStatus(1);
        scheduleMapper.insert(schedule);
        return schedule.getId();
    }

    /**
     * 停诊：存在待就诊/就诊中挂号时拒绝（退号语义归收费阶段，此处不自动退）
     */
    @Transactional
    public void stop(Long id) {
        BizSchedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        if (schedule.getStatus() == 0) {
            throw new BusinessException("该排班已停诊");
        }
        Long active = registrationMapper.selectCount(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getScheduleId, id)
                .in(BizRegistration::getStatus, BizRegistration.ST_WAITING, BizRegistration.ST_IN_CONSULT));
        if (active > 0) {
            throw new BusinessException("该排班存在待就诊挂号，请先处理");
        }
        BizSchedule patch = new BizSchedule();
        patch.setId(id);
        patch.setStatus(0);
        scheduleMapper.updateById(patch);
    }
}
