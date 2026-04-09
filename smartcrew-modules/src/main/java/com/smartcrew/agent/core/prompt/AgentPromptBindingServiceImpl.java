package com.smartcrew.agent.core.prompt;

import com.smartcrew.agent.api.admin.domain.request.AgentPromptBindingItemRequest;
import com.smartcrew.agent.api.admin.domain.request.AgentPromptBindingUpdateRequest;
import com.smartcrew.agent.api.prompt.domain.entity.AgentPromptBinding;
import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import com.smartcrew.agent.api.prompt.domain.vo.AgentPromptBindingVo;
import com.smartcrew.agent.api.prompt.mapper.AgentPromptBindingMapper;
import com.smartcrew.agent.api.prompt.mapper.PromptTemplateMapper;
import com.smartcrew.agent.api.prompt.service.AgentPromptBindingService;
import com.smartcrew.agent.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent Prompt 绑定关系服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentPromptBindingServiceImpl implements AgentPromptBindingService {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(AgentPromptBindingServiceImpl.class);

    /**
     * 绑定关系 Mapper。
     */
    private final AgentPromptBindingMapper agentPromptBindingMapper;

    /**
     * Prompt 模板 Mapper。
     */
    private final PromptTemplateMapper promptTemplateMapper;

    @Override
    public List<AgentPromptBindingVo> listByAgentCode(String agentCode) {
        List<AgentPromptBinding> bindings = agentPromptBindingMapper.selectByAgentCode(agentCode);
        Map<Long, PromptTemplate> templateMap = loadTemplateMapFromBindings(bindings);
        return bindings.stream()
                .map(binding -> toVo(binding, templateMap.get(binding.getPromptTemplateId())))
                .toList();
    }

    @Override
    public List<AgentPromptBindingVo> listResolvedByAgentCode(String agentCode) {
        List<AgentPromptBinding> bindings = agentPromptBindingMapper.selectByAgentCode(agentCode);
        Map<Long, PromptTemplate> templateMap = loadTemplateMapFromBindings(bindings);
        List<AgentPromptBindingVo> result = new ArrayList<>();
        for (AgentPromptBinding binding : bindings) {
            PromptTemplate template = templateMap.get(binding.getPromptTemplateId());
            if (template == null) {
                log.warn("Agent Prompt 绑定引用的模板不存在，agentCode={}, promptTemplateId={}",
                        agentCode, binding.getPromptTemplateId());
                continue;
            }
            result.add(toVo(binding, template));
        }
        return result;
    }

    @Override
    public List<AgentPromptBindingVo> replaceBindings(String agentCode, AgentPromptBindingUpdateRequest request) {
        List<AgentPromptBindingItemRequest> bindingRequests = request.getBindings() == null
                ? List.of()
                : request.getBindings();
        validateRequests(bindingRequests);
        Map<Long, PromptTemplate> templateMap = loadTemplateMapByIds(bindingRequests.stream()
                .map(AgentPromptBindingItemRequest::getPromptTemplateId)
                .toList());

        agentPromptBindingMapper.deleteByAgentCode(agentCode);

        int sortOrder = 1;
        for (AgentPromptBindingItemRequest bindingRequest : bindingRequests) {
            AgentPromptBinding binding = new AgentPromptBinding();
            binding.setAgentCode(agentCode);
            binding.setPromptTemplateId(bindingRequest.getPromptTemplateId());
            binding.setSortOrder(sortOrder++);
            agentPromptBindingMapper.insert(binding);
        }

        return agentPromptBindingMapper.selectByAgentCode(agentCode).stream()
                .map(binding -> toVo(binding, templateMap.get(binding.getPromptTemplateId())))
                .toList();
    }

    /**
     * 校验绑定请求。
     */
    private void validateRequests(List<AgentPromptBindingItemRequest> bindingRequests) {
        List<Long> promptTemplateIds = bindingRequests.stream()
                .map(AgentPromptBindingItemRequest::getPromptTemplateId)
                .filter(Objects::nonNull)
                .toList();
        Set<Long> uniqueIds = promptTemplateIds.stream().collect(Collectors.toSet());
        if (uniqueIds.size() != promptTemplateIds.size()) {
            throw new ServiceException(400, "同一个 Prompt 模板不能重复绑定到同一个 Agent");
        }
        Map<Long, PromptTemplate> templateMap = loadTemplateMapByIds(promptTemplateIds);
        if (templateMap.size() != uniqueIds.size()) {
            throw new ServiceException(404, "存在未找到的 Prompt 模板，无法保存绑定关系");
        }
    }

    /**
     * 根据绑定列表加载模板映射。
     */
    private Map<Long, PromptTemplate> loadTemplateMapFromBindings(List<AgentPromptBinding> bindings) {
        return loadTemplateMapByIds(bindings.stream()
                .map(AgentPromptBinding::getPromptTemplateId)
                .toList());
    }

    /**
     * 按模板 ID 列表加载模板映射。
     */
    private Map<Long, PromptTemplate> loadTemplateMapByIds(List<Long> promptTemplateIds) {
        if (promptTemplateIds.isEmpty()) {
            return Map.of();
        }
        return promptTemplateMapper.selectBatchIds(promptTemplateIds).stream()
                .collect(Collectors.toMap(PromptTemplate::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 转换为绑定视图对象。
     */
    private AgentPromptBindingVo toVo(AgentPromptBinding binding, PromptTemplate template) {
        if (template == null) {
            return AgentPromptBindingVo.builder()
                    .id(binding.getId())
                    .agentCode(binding.getAgentCode())
                    .promptTemplateId(binding.getPromptTemplateId())
                    .templateName("模板已删除")
                    .category("")
                    .templateContent("")
                    .sortOrder(binding.getSortOrder())
                    .build();
        }
        return AgentPromptBindingVo.builder()
                .id(binding.getId())
                .agentCode(binding.getAgentCode())
                .promptTemplateId(binding.getPromptTemplateId())
                .templateName(template.getTemplateName())
                .category(template.getCategory())
                .templateContent(template.getTemplateContent())
                .sortOrder(binding.getSortOrder())
                .build();
    }
}
