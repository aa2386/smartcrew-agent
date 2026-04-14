package com.smartcrew.agent.api.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鐭ヨ瘑鏂囨。瀹炰綋锛岀敤浜庤拷韪枃妗ｅ勭悊鐘舵€佷笌鍏冩暟鎹€?
 */
@Data
@TableName("knowledge_document")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocument extends BaseEntity {

    /**
     * 涓婚敭 ID銆?
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 鐭ヨ瘑搴?ID銆?
     */
    private Long baseId;
    /**
     * 鏂囨。缂栫爜銆?
     */
    private String documentCode;
    /**
     * 鏂囨。鍚嶇О銆?
     */
    private String documentName;
    /**
     * 瀛樺偍璺緞銆?
     */
    private String filePath;
    /**
     * 鏂囦欢绫诲瀷銆?
     */
    private String fileType;
    /**
     * 鏂囦欢澶у皬銆?
     */
    private Long fileSize;
    /**
     * 澶勭悊鐘舵€併€?
     */
    private String status;
    /**
     * 鍒囩墖鏁伴噺銆?
     */
    private Integer chunkCount;
    /**
     * 閿欒淇℃伅銆?
     */
    private String errorMessage;
}
