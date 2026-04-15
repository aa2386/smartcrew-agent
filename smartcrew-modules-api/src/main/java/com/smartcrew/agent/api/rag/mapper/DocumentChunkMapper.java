package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.DocumentChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档切片数据访问接口。
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 按文档 ID 查询切片列表。
     *
     * @param documentId 文档 ID。
     * @return 切片列表。
     */
    @Select("select * from document_chunk where document_id = #{documentId} order by chunk_index asc")
    List<DocumentChunk> selectByDocumentId(@Param("documentId") Long documentId);

    /**
     * 按向量 ID 查询切片。
     *
     * @param vectorId 向量 ID。
     * @return 匹配到的切片，未命中时返回 null。
     */
    @Select("select * from document_chunk where vector_id = #{vectorId} limit 1")
    DocumentChunk selectByVectorId(@Param("vectorId") String vectorId);

    /**
     * 按文档 ID 删除切片。
     *
     * @param documentId 文档 ID。
     * @return 影响行数。
     */
    @Delete("delete from document_chunk where document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 按向量 ID 删除切片。
     *
     * @param vectorId 向量 ID。
     * @return 影响行数。
     */
    @Delete("delete from document_chunk where vector_id = #{vectorId}")
    int deleteByVectorId(@Param("vectorId") String vectorId);
}
