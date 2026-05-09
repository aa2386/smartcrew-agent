package com.smartcrew.agent.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcrew.agent.api.schedule.entity.LifeTaskRecord;
import com.smartcrew.agent.api.schedule.mapper.LifeTaskRecordMapper;
import com.smartcrew.agent.api.schedule.service.LifeTaskRecordService;
import com.smartcrew.agent.common.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生活日程任务记录服务实现。
 *
 * <p>所有操作按 userId 隔离，不支持跨用户查询。缺失时间信息时自动设为 NEEDS_CONFIRMATION 状态。</p>
 */
@Service
public class LifeTaskRecordServiceImpl implements LifeTaskRecordService {

    private final LifeTaskRecordMapper mapper;

    public LifeTaskRecordServiceImpl(LifeTaskRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public LifeTaskRecord create(LifeTaskRecord record) {
        // 缺少明确时间信息时，设为待确认状态
        if (record.getDueTime() == null && StringUtils.isBlank(record.getTimeText())) {
            record.setStatus("NEEDS_CONFIRMATION");
        }
        if (StringUtils.isBlank(record.getStatus())) {
            record.setStatus("PENDING");
        }
        if (StringUtils.isBlank(record.getPriority())) {
            record.setPriority("MEDIUM");
        }
        if (StringUtils.isBlank(record.getSource())) {
            record.setSource("DELEGATION");
        }
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        mapper.insert(record);
        return record;
    }

    @Override
    public List<LifeTaskRecord> listByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<LifeTaskRecord>()
                .eq(LifeTaskRecord::getUserId, userId)
                .orderByDesc(LifeTaskRecord::getCreateTime));
    }

    @Override
    public LifeTaskRecord getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public LifeTaskRecord updateStatus(Long id, String status) {
        LifeTaskRecord record = mapper.selectById(id);
        if (record == null) {
            return null;
        }
        record.setStatus(status);
        record.setUpdateTime(LocalDateTime.now());
        mapper.updateById(record);
        return record;
    }

    @Override
    @Transactional
    public LifeTaskRecord update(LifeTaskRecord record) {
        record.setUpdateTime(LocalDateTime.now());
        mapper.updateById(record);
        return record;
    }
}
