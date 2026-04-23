package com.smartcrew.agent.api.prompt.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Prompt 模板实体。
 */
@Data
@TableName("prompt_template")
@EqualsAndHashCode(callSuper = true)
public class PromptTemplate extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称。
     */
    private String templateName;

    /**
     * 模板内容。
     */
    private String templateContent;

    /**
     * 模板分类标识。
     */
    private String category;
}
