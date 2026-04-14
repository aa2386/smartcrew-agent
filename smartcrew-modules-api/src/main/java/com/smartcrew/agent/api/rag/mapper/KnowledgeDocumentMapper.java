package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 鐭ヨ瘑鏂囨。鏁版嵁璁块棶鎺ュ彛銆?
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /**
     * 鎸夌煡璇嗗簱 ID 鏌ヨ鏂囨。鍒楄〃銆?     *
     * @param baseId 鐭ヨ瘑搴?ID銆?     * @return 鏂囨。鍒楄〃銆?     */
    @Select("select * from knowledge_document where base_id = #{baseId} order by id asc")
    List<KnowledgeDocument> selectByBaseId(@Param("baseId") Long baseId);

    /**
     * 鎸夊鐞嗙姸鎬佹煡璇㈡枃妗ｅ垪琛ㄣ€?     *
     * @param status 澶勭悊鐘舵€併€?     * @return 鏂囨。鍒楄〃銆?     */
    @Select("select * from knowledge_document where status = #{status} order by id asc")
    List<KnowledgeDocument> selectByStatus(@Param("status") String status);
}
