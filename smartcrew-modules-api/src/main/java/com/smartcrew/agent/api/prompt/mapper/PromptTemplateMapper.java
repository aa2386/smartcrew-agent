package com.smartcrew.agent.api.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * PromptTemplateMapper 接口，负责模板数据访问。
 */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    /**
     * 按分类查询最新模板。
     */
    @Select("select * from prompt_template where category = #{category} order by update_time desc, id desc limit 1")
    PromptTemplate selectLatestByCategory(@Param("category") String category);

    /**
     * 分页查询按分类聚合后的最新模板。
     */
    @Select("""
            select p.*
            from prompt_template p
                     inner join (
                select max(id) as id
                from prompt_template
                group by category
            ) latest on latest.id = p.id
            order by p.category asc
            """)
    IPage<PromptTemplate> selectLatestCategoryPage(Page<PromptTemplate> page);
}
