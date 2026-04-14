package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鐭ヨ瘑搴撳疄浣擄紝鎻忚堪 RAG 鐭ヨ瘑搴撶殑鍏冩暟鎹€?
 */
@Data
@TableName("knowledge_base")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBase extends BaseEntity {

    /**
     * 涓婚敭 ID銆?
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 鐭ヨ瘑搴撶紪鐮併€?
     */
    private String baseCode;
    /**
     * 鐭ヨ瘑搴撳悕绉般€?
     */
    private String baseName;
    /**
     * 鎻忚堪淇℃伅銆?
     */
    private String description;
    /**
     * 宓屽叆妯″瀷鍚嶇О銆?
     */
    private String embeddingModel;
    /**
     * 鍚戦噺鍛藉悕绌洪棿銆?
     */
    private String collectionName;
    /**
     * 鏄惁鍚敤銆?
     */
    private Boolean enabled;
}
