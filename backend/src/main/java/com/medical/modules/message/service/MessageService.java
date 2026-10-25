package com.medical.modules.message.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.framework.LoginUser;
import com.medical.framework.SecurityUtils;
import com.medical.modules.exam.entity.BizExamReport;
import com.medical.modules.exam.mapper.BizExamReportMapper;
import com.medical.modules.message.entity.SysMessage;
import com.medical.modules.message.entity.SysNotice;
import com.medical.modules.message.mapper.SysMessageMapper;
import com.medical.modules.message.mapper.SysNoticeMapper;
import com.medical.modules.pharmacy.entity.BizStock;
import com.medical.modules.pharmacy.mapper.StockMapper;
import com.medical.modules.system.entity.SysUser;
import com.medical.modules.system.mapper.SysUserMapper;
import com.medical.modules.exam.mapper.BizExamReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final SysMessageMapper messageMapper;
    private final SysNoticeMapper noticeMapper;
    private final StockMapper stockMapper;
    private final com.medical.modules.clinic.mapper.DrugMapper drugMapper;
    private final BizExamReportMapper examReportMapper;
    private final SysUserMapper userMapper;

    // ---------------- 站内消息（receiver_id=当前用户恒等） ----------------

    public PageResult<SysMessage> page(PageQuery query, Integer msgType, Integer isRead) {
        LoginUser user = SecurityUtils.getCurrentUser();
        Page<SysMessage> page = messageMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysMessage>lambdaQuery()
                        .eq(SysMessage::getReceiverId, user.getId())
                        .eq(msgType != null, SysMessage::getMsgType, msgType)
                        .eq(isRead != null, SysMessage::getIsRead, isRead)
                        .orderByDesc(SysMessage::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 标记已读：仅本人消息，已读重复点击幂等 */
    @Transactional
    public void read(Long id) {
        LoginUser user = SecurityUtils.getCurrentUser();
        int updated = messageMapper.update(null, Wrappers.<SysMessage>lambdaUpdate()
                .set(SysMessage::getIsRead, 1)
                .eq(SysMessage::getId, id)
                .eq(SysMessage::getReceiverId, user.getId()));
        if (updated == 0) {
            throw new BusinessException("消息不存在或无权操作");
        }
    }

    @Transactional
    public void readAll() {
        LoginUser user = SecurityUtils.getCurrentUser();
        messageMapper.update(null, Wrappers.<SysMessage>lambdaUpdate()
                .set(SysMessage::getIsRead, 1)
                .eq(SysMessage::getReceiverId, user.getId())
                .eq(SysMessage::getIsRead, 0));
    }

    public long unreadCount() {
        LoginUser user = SecurityUtils.getCurrentUser();
        return messageMapper.selectCount(Wrappers.<SysMessage>lambdaQuery()
                .eq(SysMessage::getReceiverId, user.getId())
                .eq(SysMessage::getIsRead, 0));
    }

    /** 收费成功写站内信（收费员通知，应用内同步；MQ 消费链后续接入） */
    @Transactional
    public void notifyChargePaid(Long operatorId, String chargeNo, String patientName, String amount) {
        SysMessage msg = new SysMessage();
        msg.setReceiverId(operatorId);
        msg.setTitle("收费成功");
        msg.setContent(String.format("收费单 %s：患者 %s，实收 ￥%s", chargeNo, patientName, amount));
        msg.setMsgType(1);
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }

    // ---------------- 公告 ----------------

    /** 发布公告：科室公告必选科室并校验存在；status 缺省 1 */
    @Transactional
    public Long publishNotice(SysNotice notice) {
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new BusinessException("公告标题不能为空");
        }
        LoginUser user = SecurityUtils.getCurrentUser();
        if (notice.getNoticeType() != null && notice.getNoticeType() == SysNotice.TYPE_DEPT) {
            if (notice.getDeptId() == null) {
                throw new BusinessException("科室公告必选科室");
            }
        }
        notice.setPublisherId(user.getId());
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        notice.setPublishTime(java.time.LocalDateTime.now());
        noticeMapper.insert(notice);
        return notice.getId();
    }

    public PageResult<Map<String, Object>> noticePage(PageQuery query, Integer noticeType, Long deptId) {
        Page<SysNotice> page = noticeMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysNotice>lambdaQuery()
                        .eq(noticeType != null, SysNotice::getNoticeType, noticeType)
                        .eq(deptId != null, SysNotice::getDeptId, deptId)
                        .orderByDesc(SysNotice::getId));
        List<Long> publisherIds = page.getRecords().stream()
                .map(SysNotice::getPublisherId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> names = publisherIds.isEmpty() ? Map.of()
                : userMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, publisherIds))
                .stream().collect(Collectors.toMap(SysUser::getId,
                        u -> StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername(), (a, b) -> a));
        List<Map<String, Object>> rows = page.getRecords().stream().map(n -> Map.<String, Object>of(
                "id", n.getId(),
                "title", n.getTitle(),
                "content", n.getContent() == null ? "" : n.getContent(),
                "noticeType", n.getNoticeType(),
                "publisherName", names.getOrDefault(n.getPublisherId(), ""),
                "publishTime", n.getPublishTime() == null ? "" : n.getPublishTime().toString(),
                "status", n.getStatus())).toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), rows);
    }

    @Transactional
    public void deleteNotice(Long id) {
        if (noticeMapper.selectById(id) == null) {
            throw new BusinessException("公告不存在");
        }
        noticeMapper.deleteById(id);
    }

    // ---------------- 预警列表（实时聚合，不落 sys_message） ----------------

    /**
     * 实时聚合：低库存 + 近效期 + 未处理危急值
     */
    public List<Map<String, Object>> warningList() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate nearLine = LocalDate.now().plusDays(90);
        // 低库存 + 近效期（同药房口径）
        List<BizStock> stocks = stockMapper.selectList(Wrappers.<BizStock>lambdaQuery());
        List<Long> drugIds = stocks.stream().map(BizStock::getDrugId).distinct().toList();
        Map<Long, String> drugNames = drugIds.isEmpty() ? Map.of()
                : drugMapper.selectList(Wrappers.<com.medical.modules.clinic.entity.BizDrug>lambdaQuery()
                        .in(com.medical.modules.clinic.entity.BizDrug::getId, drugIds))
                .stream().collect(Collectors.toMap(com.medical.modules.clinic.entity.BizDrug::getId,
                        com.medical.modules.clinic.entity.BizDrug::getDrugName, (a, b) -> a));
        for (BizStock s : stocks) {
            boolean low = s.getQuantity() <= s.getWarnNum();
            boolean near = s.getExpireDate() != null && !s.getExpireDate().isAfter(nearLine);
            if (low) {
                result.add(Map.of("type", "low", "title",
                        drugNames.getOrDefault(s.getDrugId(), "药品#" + s.getDrugId()) + " 库存不足",
                        "detail", "当前 " + s.getQuantity() + "（预警线 " + s.getWarnNum() + "）", "time", ""));
            }
            if (near) {
                result.add(Map.of("type", "expire", "title",
                        drugNames.getOrDefault(s.getDrugId(), "药品#" + s.getDrugId()) + " 近效期",
                        "detail", "有效期至 " + s.getExpireDate(), "time", ""));
            }
        }
        // 未处理危急值（is_critical=1 且已发布且未处理）
        List<BizExamReport> criticals = examReportMapper.selectList(Wrappers.<BizExamReport>lambdaQuery()
                .eq(BizExamReport::getIsCritical, 1)
                .eq(BizExamReport::getStatus, BizExamReport.ST_PUBLISHED)
                .and(w -> w.isNull(BizExamReport::getCriticalHandled)
                        .or().eq(BizExamReport::getCriticalHandled, 0)));
        for (BizExamReport r : criticals) {
            result.add(Map.of("type", "critical", "title", "危急值未处理",
                    "detail", "报告 " + r.getReportNo(), "time",
                    r.getReportTime() == null ? "" : r.getReportTime().toString()));
        }
        return result;
    }
}
