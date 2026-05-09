package com.smartcrew.agent.api.experience.service;

import com.smartcrew.agent.api.experience.domain.entity.AgentExperienceHitLog;
import com.smartcrew.agent.api.experience.domain.entity.AgentExperiencePool;
import com.smartcrew.agent.api.experience.domain.query.AgentExperienceHitLogQuery;
import com.smartcrew.agent.api.experience.domain.query.AgentExperiencePoolQuery;
import com.smartcrew.agent.api.experience.domain.vo.AgentExperienceHitLogVo;
import com.smartcrew.agent.api.experience.domain.vo.AgentExperienceRecallVo;
import com.smartcrew.agent.core.page.TableDataInfo;

import java.util.Optional;

/**
 * 经验池服务接口。
 */
public interface AgentExperienceService {

    /**
     * 分页召回全局经验。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    TableDataInfo<AgentExperienceRecallVo> recallGlobalExperiences(AgentExperiencePoolQuery query);

    /**
     * 记录成功经验。
     *
     * @param experiencePool 经验池实体
     * @return 落库后的经验实体
     */
    AgentExperiencePool recordSuccessfulExperience(AgentExperiencePool experiencePool);

    /**
     * 记录经验命中。
     *
     * @param hitLog 命中日志
     */
    void recordExperienceHit(AgentExperienceHitLog hitLog);

    /**
     * 分页查询经验命中日志。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    TableDataInfo<AgentExperienceHitLogVo> listExperienceHits(AgentExperienceHitLogQuery query);

    /**
     * 按主键查询经验详情。
     *
     * @param id 主键 ID
     * @return 匹配结果
     */
    Optional<AgentExperiencePool> findExperienceById(Long id);
}
