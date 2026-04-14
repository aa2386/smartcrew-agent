package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 涓庣煡璇嗗簱缁戝畾瀹炰綋銆?
 */
@Data
@TableName("agent_knowledge_binding")
@EqualsAndHashCode(callSuper = true)
public class AgentKnowledgeBinding extends BaseEntity {

    /**
     * 涓婚敭 ID銆?
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * Agent 缂栫爜銆?
     */
    private String agentCode;
    /**
     * 鐭ヨ瘑搴撶紪鐮併€?
     */
    private String baseCode;
}
