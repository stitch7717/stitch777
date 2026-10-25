package com.medical.modules.message.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.common.PageQuery;
import com.medical.common.PageResult;
import com.medical.modules.message.entity.SysSmsRecord;
import com.medical.modules.message.mapper.SysSmsRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 模拟短信网关：发送直记成功 */
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SysSmsRecordMapper smsMapper;

    public Long send(String phone, String templateCode, String content) {
        if (!org.springframework.util.StringUtils.hasText(phone)) {
            throw new BusinessException("手机号必填");
        }
        SysSmsRecord record = new SysSmsRecord();
        record.setPhone(phone);
        record.setTemplateCode(templateCode == null ? "MANUAL" : templateCode);
        record.setContent(content == null ? "" : content);
        record.setSendStatus(1);
        record.setSendTime(LocalDateTime.now());
        smsMapper.insert(record);
        return record.getId();
    }

    public PageResult<SysSmsRecord> page(PageQuery query, String phone) {
        Page<SysSmsRecord> page = smsMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<SysSmsRecord>lambdaQuery()
                        .eq(org.springframework.util.StringUtils.hasText(phone), SysSmsRecord::getPhone, phone)
                        .orderByDesc(SysSmsRecord::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
