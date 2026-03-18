package com.smartcrew.agent.api.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * ????????????
 */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    @Select("select * from prompt_template where category = #{category} order by update_time desc, id desc limit 1")
    PromptTemplate selectLatestByCategory(@Param("category") String category);
}
