package com.smartcrew.agent.api.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.agent.domain.entity.AgentToolBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * AgentToolBindingMapper 接口，负责对应领域对象的数据访问操作。
 */
@Mapper
public interface AgentToolBindingMapper extends BaseMapper<AgentToolBinding> {
}
