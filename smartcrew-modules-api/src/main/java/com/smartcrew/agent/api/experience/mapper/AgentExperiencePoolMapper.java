package com.smartcrew.agent.api.experience.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperiencePool;
import org.apache.ibatis.annotations.Mapper;

/**
 * 经验池数据访问接口。
 */
@Mapper
public interface AgentExperiencePoolMapper extends BaseMapper<AgentExperiencePool> {
}
