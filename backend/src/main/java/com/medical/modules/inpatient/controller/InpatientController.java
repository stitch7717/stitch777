package com.medical.modules.inpatient.controller;

import com.medical.common.Result;
import com.medical.modules.inpatient.dto.AdmissionDetailVO;
import com.medical.modules.inpatient.dto.AdmissionRequest;
import com.medical.modules.inpatient.dto.BedVO;
import com.medical.modules.inpatient.entity.BizBed;
import com.medical.modules.inpatient.service.AdmissionService;
import com.medical.modules.inpatient.service.BedService;
import com.medical.modules.inpatient.service.OrderNursingService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.modules.system.entity.SysRole;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysRoleMapper;
import com.medical.modules.system.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inpatient")
@RequiredArgsConstructor
public class InpatientController {

    private final BedService bedService;
    private final AdmissionService admissionService;
    private final OrderNursingService orderNursingService;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final com.medical.modules.system.mapper.SysDeptMapper deptMapper;

    @GetMapping("/bed/list")
    @PreAuthorize("hasAuthority('inpatient:list')")
    public Result<List<BedVO>> beds(Long wardId, Integer status) {
        return Result.ok(bedService.list(wardId, status));
    }

    @GetMapping("/beds/free")
    @PreAuthorize("hasAuthority('inpatient:list')")
    public Result<List<BizBed>> freeBeds(Long wardId) {
        return Result.ok(bedService.freeBeds(wardId));
    }

    @PostMapping("/admission")
    @PreAuthorize("hasAuthority('inpatient:admission:add')")
    public Result<Map<String, Object>> admit(@Valid @RequestBody AdmissionRequest request) {
        return Result.ok(admissionService.admit(request));
    }

    @PutMapping("/admission/{id}/discharge")
    @PreAuthorize("hasAuthority('inpatient:admission:edit')")
    public Result<Void> discharge(@PathVariable Long id) {
        admissionService.discharge(id);
        return Result.ok();
    }

    @PostMapping("/admission/{id}/transfer")
    @PreAuthorize("hasAuthority('inpatient:admission:edit')")
    public Result<Map<String, Object>> transfer(@PathVariable Long id,
                                                @RequestBody Map<String, Long> body) {
        return Result.ok(admissionService.transfer(id,
                body.get("newDeptId"), body.get("newBedId"), body.get("newDoctorId")));
    }

    @GetMapping("/admission/{id}")
    @PreAuthorize("hasAuthority('inpatient:list')")
    public Result<AdmissionDetailVO> detail(@PathVariable Long id) {
        return Result.ok(admissionService.detail(id));
    }

    @PostMapping("/order")
    @PreAuthorize("hasAuthority('inpatient:order:add')")
    public Result<Map<String, Long>> addOrder(@Valid @RequestBody com.medical.modules.inpatient.dto.OrderRequest request) {
        return Result.ok(Map.of("id", orderNursingService.addOrder(request)));
    }

    @PutMapping("/order/{id}/execute")
    @PreAuthorize("hasAuthority('inpatient:order:operate')")
    public Result<Void> executeOrder(@PathVariable Long id) {
        orderNursingService.execute(id);
        return Result.ok();
    }

    @PutMapping("/order/{id}/stop")
    @PreAuthorize("hasAuthority('inpatient:order:operate')")
    public Result<Void> stopOrder(@PathVariable Long id) {
        orderNursingService.stop(id);
        return Result.ok();
    }

    @PostMapping("/nursing")
    @PreAuthorize("hasAuthority('inpatient:nursing:add')")
    public Result<Map<String, Long>> addNursing(@Valid @RequestBody com.medical.modules.inpatient.dto.NursingRequest request) {
        return Result.ok(Map.of("id", orderNursingService.addNursing(request)));
    }

    @GetMapping("/nursing/page")
    @PreAuthorize("hasAuthority('inpatient:list')")
    public Result<Object> nursingPage(com.medical.common.PageQuery query, Long admissionId) {
        return Result.ok(orderNursingService.nursingPage(query, admissionId));
    }

    /** 病区（科室）下拉 */
    @GetMapping("/depts")
    @PreAuthorize("hasAuthority('inpatient:list')")
    public Result<List<Map<String, Object>>> depts() {
        return Result.ok(deptMapper.selectList(Wrappers.<com.medical.modules.system.entity.SysDept>lambdaQuery()
                        .eq(com.medical.modules.system.entity.SysDept::getStatus, 1)
                        .orderByAsc(com.medical.modules.system.entity.SysDept::getSort))
                .stream().map(d -> Map.<String, Object>of("id", d.getId(), "name", d.getDeptName()))
                .toList());
    }

    /** 医生/护士下拉（按科室可选） */
    @GetMapping("/doctors")
    @PreAuthorize("hasAuthority('inpatient:list')")
    public Result<List<Map<String, Object>>> doctors(Long deptId) {
        return Result.ok(usersByRole("doctor", deptId));
    }

    @GetMapping("/nurses")
    @PreAuthorize("hasAuthority('inpatient:list')")
    public Result<List<Map<String, Object>>> nurses(Long deptId) {
        return Result.ok(usersByRole("nurse", deptId));
    }

    private List<Map<String, Object>> usersByRole(String roleCode, Long deptId) {
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            return List.of();
        }
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getRoleId, role.getId())
                        .eq(SysUser::getStatus, 1)
                        .eq(deptId != null, SysUser::getDeptId, deptId))
                .stream().map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getRealName() != null ? u.getRealName() : u.getUsername()))
                .toList();
    }
}
