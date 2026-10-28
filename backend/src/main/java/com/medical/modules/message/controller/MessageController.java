package com.medical.modules.message.controller;

import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.common.Result;
import com.medical.modules.message.entity.SysMessage;
import com.medical.modules.message.entity.SysNotice;
import com.medical.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/api/message/page")
    public Result<PageResult<SysMessage>> page(PageQuery query, Integer msgType, Integer isRead) {
        return Result.ok(messageService.page(query, msgType, isRead));
    }

    @PutMapping("/api/message/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.read(id);
        return Result.ok();
    }

    @PutMapping("/api/message/read-all")
    public Result<Void> readAll() {
        messageService.readAll();
        return Result.ok();
    }

    @GetMapping("/api/message/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", messageService.unreadCount()));
    }

    /** 发布公告：仅公告管理权限 */
    @PreAuthorize("hasAuthority('message:notice')")
    @PostMapping("/api/notice")
    public Result<Map<String, Long>> publish(@RequestBody SysNotice notice) {
        return Result.ok(Map.of("id", messageService.publishNotice(notice)));
    }

    @GetMapping("/api/notice/page")
    public Result<PageResult<Map<String, Object>>> noticePage(PageQuery query, Integer noticeType, Long deptId) {
        return Result.ok(messageService.noticePage(query, noticeType, deptId));
    }

    @PreAuthorize("hasAuthority('message:notice')")
    @DeleteMapping("/api/notice/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        messageService.deleteNotice(id);
        return Result.ok();
    }

    /** 预警列表（实时聚合：低库存/近效期/未处理危急值） */
    @GetMapping("/api/warning/list")
    public Result<List<Map<String, Object>>> warnings() {
        return Result.ok(messageService.warningList());
    }
}
