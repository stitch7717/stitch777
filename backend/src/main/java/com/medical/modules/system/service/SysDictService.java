package com.medical.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.BusinessException;
import com.medical.modules.system.entity.SysDictData;
import com.medical.modules.system.entity.SysDictType;
import com.medical.modules.system.mapper.SysDictDataMapper;
import com.medical.modules.system.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictDataMapper dictDataMapper;
    private final SysDictTypeMapper dictTypeMapper;

    /**
     * 按类型查询字典数据：dictType 必传，未知类型返回空数组
     */
    public List<SysDictData> dataByType(String dictType) {
        return dictDataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort).orderByAsc(SysDictData::getId));
    }

    /**
     * 字典类型列表（页面左侧导航，补充接口）
     */
    public List<SysDictType> typeList() {
        return dictTypeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getId));
    }

    /**
     * 新增字典数据：同类型未删行标签唯一（可删除后重建同名）；可带 color
     */
    @Transactional
    public Long addData(SysDictData data) {
        if (!StringUtils.hasText(data.getDictType()) || !StringUtils.hasText(data.getDictLabel())) {
            throw new BusinessException("字典类型与标签不能为空");
        }
        if (!StringUtils.hasText(data.getDictValue())) {
            data.setDictValue(data.getDictLabel());
        }
        if (data.getColor() != null && !List.of("red", "yellow", "green").contains(data.getColor())) {
            throw new BusinessException("颜色仅支持 red/yellow/green");
        }
        Long dup = dictDataMapper.selectCount(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, data.getDictType())
                .eq(SysDictData::getDictLabel, data.getDictLabel()));
        if (dup > 0) {
            throw new BusinessException("该类型下标签已存在");
        }
        data.setSort(data.getSort() == null ? 0 : data.getSort());
        data.setStatus(1);
        dictDataMapper.insert(data);
        return data.getId();
    }

    /**
     * 修改字典数据：阶段口径当前仅改色（red/yellow/green）
     */
    @Transactional
    public void updateColor(Long id, String color) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data == null) {
            throw new BusinessException("字典数据不存在");
        }
        if (!List.of("red", "yellow", "green").contains(color)) {
            throw new BusinessException("颜色仅支持 red/yellow/green");
        }
        SysDictData patch = new SysDictData();
        patch.setId(id);
        patch.setColor(color);
        dictDataMapper.updateById(patch);
    }

    /**
     * 删除字典数据（逻辑删除）
     */
    @Transactional
    public void deleteData(Long id) {
        if (dictDataMapper.selectById(id) == null) {
            throw new BusinessException("字典数据不存在");
        }
        dictDataMapper.deleteById(id);
    }
}
