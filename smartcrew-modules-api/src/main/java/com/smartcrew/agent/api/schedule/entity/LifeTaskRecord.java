package com.smartcrew.agent.api.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 生活日程任务记录实体。
 *
 * <p>存储项目内任务记录，状态覆盖 PENDING、DONE、CANCELLED、NEEDS_CONFIRMATION。
 * 本期不设计复杂 recurrence 表，重复规则以 metadata_json 文本形式保存。</p>
 */
@Data
@TableName("life_task_record")
@EqualsAndHashCode(callSuper = true)
public class LifeTaskRecord extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 任务标题 */
    private String title;

    /** 任务描述 */
    private String description;

    /** 截止时间 */
    private LocalDateTime dueTime;

    /** 原始时间文本（如"明天上午九点"） */
    private String timeText;

    /** 时区 */
    private String timezone;

    /** 任务状态：PENDING / DONE / CANCELLED / NEEDS_CONFIRMATION */
    private String status;

    /** 优先级：LOW / MEDIUM / HIGH */
    private String priority;

    /** 来源标识：如 DELEGATION、MANUAL */
    private String source;

    /** 关联的追踪 ID */
    private String traceId;

    /** 扩展元数据 JSON */
    private String metadataJson;
}
