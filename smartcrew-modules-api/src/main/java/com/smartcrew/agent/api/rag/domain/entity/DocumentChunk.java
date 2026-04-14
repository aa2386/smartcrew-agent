package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鏂囨。鍒囩墖瀹炰綋锛岀敤浜庝繚瀛樺垏鐗囧唴瀹瑰拰鍚戦噺鍏宠仈銆?
 */
@Data
@TableName("document_chunk")
@EqualsAndHashCode(callSuper = true)
public class DocumentChunk extends BaseEntity {

    /**
     * 涓婚敭 ID銆?
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 鏂囨。 ID銆?
     */
    private Long documentId;
    /**
     * 鍒囩墖搴忓彿銆?
     */
    private Integer chunkIndex;
    /**
     * 鍒囩墖鍐呭銆?
     */
    private String content;
    /**
     * 鍚戦噺 ID銆?
     */
    private String vectorId;
    /**
     * Token 鏁伴噺銆?
     */
    private Integer tokenCount;
    /**
     * 鍏冩暟鎹甝SON銆?
     */
    private String metadata;
}
