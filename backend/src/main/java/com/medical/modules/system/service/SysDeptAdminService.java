package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.inpatient.entity.BizBed;
import com.medical.modules.inpatient.mapper.BizBedMapper;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysDeptMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysDeptAdminService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final BizBedMapper bedMapper;

    @Transactional
    public void save(SysDept dept) {
        if (!org.springframework.util.StringUtils.hasText(dept.getDeptName())) {
            throw new BusinessException("科室名称必填");
        }
        if (dept.getParentId() == null) dept.setParentId(0L);
        if (dept.getStatus() == null) dept.setStatus(1);
        dept.setId(null);
        deptMapper.insert(dept);
    }

    @Transactional
    public void update(Long id, SysDept patch) {
        SysDept exist = require(id);
        if (org.springframework.util.StringUtils.hasText(patch.getDeptName())) exist.setDeptName(patch.getDeptName());
        if (patch.getDeptType() != null) exist.setDeptType(patch.getDeptType());
        if (patch.getSort() != null) exist.setSort(patch.getSort());
        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());
        deptMapper.updateById(exist);
    }

    /** 删除保护：有人员/有床位/有子科室 拒绝 */
    @Transactional
    public void delete(Long id) {
        require(id);
        if (deptMapper.selectCount(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getParentId, id)) > 0) {
            throw new BusinessException("存在子科室，不可删除");
        }
        if (userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getDeptId, id)) > 0) {
            throw new BusinessException("该科室下有人员，不可删除");
        }
        if (bedMapper.selectCount(Wrappers.<BizBed>lambdaQuery()
                .eq(BizBed::getWardId, id)) > 0) {
            throw new BusinessException("该科室下有床位，不可删除");
        }
        deptMapper.deleteById(id);
    }

    private SysDept require(Long id) {
        SysDept d = deptMapper.selectById(id);
        if (d == null) throw new BusinessException("科室不存在");
        return d;
    }
}
