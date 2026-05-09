package com.smartcrew.agent.api.schedule.service;

import com.smartcrew.agent.api.schedule.entity.LifeTaskRecord;

import java.util.List;

/**
 * 生活日程任务记录服务接口。
 */
public interface LifeTaskRecordService {

    /**
     * 创建任务记录。若缺少明确时间信息，状态设为 NEEDS_CONFIRMATION。
     *
     * @param record 任务记录
     * @return 创建后的任务记录
     */
    LifeTaskRecord create(LifeTaskRecord record);

    /**
     * 查询指定用户的任务记录列表。
     *
     * @param userId 用户 ID
     * @return 任务记录列表
     */
    List<LifeTaskRecord> listByUserId(Long userId);

    /**
     * 按 ID 查询任务记录。
     *
     * @param id 记录 ID
     * @return 任务记录，不存在返回 null
     */
    LifeTaskRecord getById(Long id);

    /**
     * 更新任务状态。
     *
     * @param id     记录 ID
     * @param status 新状态
     * @return 更新后的任务记录
     */
    LifeTaskRecord updateStatus(Long id, String status);

    /**
     * 更新任务记录。
     *
     * @param record 任务记录
     * @return 更新后的任务记录
     */
    LifeTaskRecord update(LifeTaskRecord record);
}
