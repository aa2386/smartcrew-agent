package com.smartcrew.agent.api.experience.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperienceHitLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 经验命中日志数据访问接口。
 */
@Mapper
public interface AgentExperienceHitLogMapper extends BaseMapper<AgentExperienceHitLog> {
}
