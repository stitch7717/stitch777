package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.modules.system.dto.MenuVO;
import com.medical.modules.system.entity.SysDept;
import com.medical.modules.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;

    public List<MenuVO> tree() {
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSort).orderByAsc(SysDept::getId));
        List<MenuVO> nodes = depts.stream().map(d -> {
            MenuVO vo = new MenuVO();
            vo.setId(d.getId());
            vo.setMenuName(d.getDeptName());
            vo.setParentId(d.getParentId());
            vo.setSort(d.getSort());
            return vo;
        }).toList();
        var byId = nodes.stream().collect(java.util.stream.Collectors.toMap(MenuVO::getId, n -> n));
        List<MenuVO> roots = new java.util.ArrayList<>();
        for (MenuVO node : nodes) {
            MenuVO parent = node.getParentId() == null || node.getParentId() == 0
                    ? null : byId.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        roots.sort(Comparator.comparing(MenuVO::getSort));
        return roots;
    }
}
