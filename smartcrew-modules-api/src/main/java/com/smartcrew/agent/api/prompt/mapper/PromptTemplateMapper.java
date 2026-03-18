package com.smartcrew.agent.api.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * PromptTemplateMapper 接口，负责对应领域对象的数据访问操作。
 */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    /**
     * 按分类查询最新模板。
     *
     * @param category 模板分类。
     * @return 匹配到的模板实体；未命中时返回 `null`。
     */
    @Select("select * from prompt_template where category = #{category} order by update_time desc, id desc limit 1")
    PromptTemplate selectLatestByCategory(@Param("category") String category);
}
