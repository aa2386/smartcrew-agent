package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识文档数据访问接口。
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /**
     * 按知识库 ID 查询文档列表。
     *
     * @param baseId 知识库 ID。
     * @return 文档列表。
     */
    @Select("select * from knowledge_document where base_id = #{baseId} order by id asc")
    List<KnowledgeDocument> selectByBaseId(@Param("baseId") Long baseId);

    /**
     * 按处理状态查询文档列表。
     *
     * @param status 处理状态。
     * @return 文档列表。
     */
    @Select("select * from knowledge_document where status = #{status} order by id asc")
    List<KnowledgeDocument> selectByStatus(@Param("status") String status);
}
