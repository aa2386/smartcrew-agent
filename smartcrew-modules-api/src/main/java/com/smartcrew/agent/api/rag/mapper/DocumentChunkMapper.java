package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.DocumentChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 鏂囨。鍒囩墖鏁版嵁璁块棶鎺ュ彛銆?
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 鎸夋枃妗?ID 鏌ヨ鍒囩墖鍒楄〃銆?     *
     * @param documentId 鏂囨。 ID銆?     * @return 鍒囩墖鍒楄〃銆?     */
    @Select("select * from document_chunk where document_id = #{documentId} order by chunk_index asc")
    List<DocumentChunk> selectByDocumentId(@Param("documentId") Long documentId);

    /**
     * 鎸夊悜閲?ID 鏌ヨ鍒囩墖銆?     *
     * @param vectorId 鍚戦噺 ID銆?     * @return 鍖归厤鍒扮殑鍒囩墖锛屾湭鍛戒腑鏃惰繑鍥?null銆?     */
    @Select("select * from document_chunk where vector_id = #{vectorId} limit 1")
    DocumentChunk selectByVectorId(@Param("vectorId") String vectorId);

    /**
     * 鎸夋枃妗?ID 鍒犻櫎鍒囩墖銆?     *
     * @param documentId 鏂囨。 ID銆?     * @return 褰卞搷琛屾暟銆?     */
    @Delete("delete from document_chunk where document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 鎸夊悜閲?ID 鍒犻櫎鍒囩墖銆?     *
     * @param vectorId 鍚戦噺 ID銆?     * @return 褰卞搷琛屾暟銆?     */
    @Delete("delete from document_chunk where vector_id = #{vectorId}")
    int deleteByVectorId(@Param("vectorId") String vectorId);
}
