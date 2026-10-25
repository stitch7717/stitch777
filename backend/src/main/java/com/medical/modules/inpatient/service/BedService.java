package com.medical.modules.inpatient.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.inpatient.dto.BedVO;
import com.medical.modules.inpatient.entity.BizAdmission;
import com.medical.modules.inpatient.entity.BizBed;
import com.medical.modules.inpatient.mapper.BizAdmissionMapper;
import com.medical.modules.inpatient.mapper.BizBedMapper;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BedService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizBedMapper bedMapper;
    private final BizAdmissionMapper admissionMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    /**
     * 床位列表（wardId/status 可选，聚合在院患者）
     */
    public List<BedVO> list(Long wardId, Integer status) {
        List<BizBed> beds = bedMapper.selectList(Wrappers.<BizBed>lambdaQuery()
                .eq(wardId != null, BizBed::getWardId, wardId)
                .eq(status != null, BizBed::getStatus, status)
                .orderByAsc(BizBed::getBedNo));
        if (beds.isEmpty()) {
            return List.of();
        }
        List<BizAdmission> inHospital = admissionMapper.selectList(Wrappers.<BizAdmission>lambdaQuery()
                .eq(BizAdmission::getStatus, BizAdmission.ST_IN)
                .in(BizAdmission::getBedId, beds.stream().map(BizBed::getId).toList()));
        Map<Long, BizAdmission> byBed = inHospital.stream()
                .collect(Collectors.toMap(BizAdmission::getBedId, Function.identity(), (a, b) -> a));

        Map<Long, String> deptNames = deptMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));

        return beds.stream().map(b -> {
            BedVO vo = new BedVO();
            vo.setId(b.getId());
            vo.setWardId(b.getWardId());
            vo.setWardName(deptNames.get(b.getWardId()));
            vo.setBedNo(b.getBedNo());
            vo.setBedType(b.getBedType());
            vo.setPrice(b.getPrice());
            vo.setStatus(b.getStatus());
            BizAdmission adm = byBed.get(b.getId());
            if (adm != null) {
                vo.setAdmissionId(adm.getId());
                vo.setPatientId(adm.getPatientId());
                vo.setPatientName(adm.getPatientName());
                vo.setDiagnosis(adm.getDiagnosis());
                vo.setAdmissionTime(adm.getAdmissionTime() == null ? null : adm.getAdmissionTime().format(FMT));
                vo.setDoctorName(userName(adm.getDoctorId()));
            }
            return vo;
        }).toList();
    }

    /** 空闲床位（入院选床用） */
    public List<BizBed> freeBeds(Long wardId) {
        return bedMapper.selectList(Wrappers.<BizBed>lambdaQuery()
                .eq(BizBed::getStatus, BizBed.ST_FREE)
                .eq(wardId != null, BizBed::getWardId, wardId)
                .orderByAsc(BizBed::getBedNo));
    }

    private String userName(Long id) {
        if (id == null) return null;
        SysUser u = userMapper.selectById(id);
        if (u == null) return null;
        return StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername();
    }
}
