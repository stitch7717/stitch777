package com.medical.modules.system.controller;

import com.medical.common.Result;
import com.medical.modules.system.entity.SysDictData;
import com.medical.modules.system.entity.SysDictType;
import com.medical.modules.system.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @GetMapping("/data")
    public Result<List<SysDictData>> data(@RequestParam String dictType) {
        return Result.ok(dictService.dataByType(dictType));
    }

    /** 字典类型列表（页面左侧导航，补充接口） */
    @GetMapping("/type/list")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<List<SysDictType>> typeList() {
        return Result.ok(dictService.typeList());
    }

    /** 新增字典数据（同类型未删行标签唯一，可带 color） */
    @PostMapping("/data")
    @PreAuthorize("hasAuthority('system:dict:add')")
    public Result<Map<String, Long>> addData(@RequestBody SysDictData data) {
        return Result.ok(Map.of("id", dictService.addData(data)));
    }

    /** 修改字典数据（当前仅改色 red/yellow/green） */
    @PutMapping("/data/{id}")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public Result<Void> updateColor(@PathVariable Long id, @RequestBody Map<String, String> body) {
        dictService.updateColor(id, body.get("color"));
        return Result.ok();
    }

    /** 删除字典数据（逻辑删除） */
    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public Result<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return Result.ok();
    }
}
