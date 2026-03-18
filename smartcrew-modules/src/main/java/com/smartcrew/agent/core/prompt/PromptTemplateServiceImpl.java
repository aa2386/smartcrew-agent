package com.smartcrew.agent.core.prompt;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.prompt.domain.entity.PromptTemplate;
import com.smartcrew.agent.api.prompt.domain.request.PromptTemplateRequest;
import com.smartcrew.agent.api.prompt.domain.vo.PromptTemplateVo;
import com.smartcrew.agent.api.prompt.mapper.PromptTemplateMapper;
import com.smartcrew.agent.api.prompt.service.PromptTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 提示词模板服务实现，负责模板创建和查询。
 */
@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    /**
     * 提示词模板数据访问对象。
     */
    private final PromptTemplateMapper promptTemplateMapper;

    /**
     * 构造 PromptTemplateServiceImpl 所需的依赖对象。
     */
    public PromptTemplateServiceImpl(PromptTemplateMapper promptTemplateMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    /**
     * 创建目标资源。
     */
    @Override
    public PromptTemplateVo create(PromptTemplateRequest request) {
        PromptTemplate entity = new PromptTemplate();
        entity.setTemplateName(request.getTemplateName());
        entity.setTemplateContent(request.getTemplateContent());
        entity.setCategory(request.getCategory());
        entity.setRemark(request.getRemark());
        promptTemplateMapper.insert(entity);
        return toVo(entity);
    }

    /**
     * 查询全部数据。
     */
    @Override
    public List<PromptTemplateVo> listAll() {
        return promptTemplateMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 按分类查询数据。
     */
    @Override
    public Optional<PromptTemplateVo> queryByCategory(String category) {
        return Optional.ofNullable(promptTemplateMapper.selectLatestByCategory(category))
                .map(this::toVo);
    }

    /**
     * 将提示词模板实体转换为视图对象。
     */
    private PromptTemplateVo toVo(PromptTemplate entity) {
        return PromptTemplateVo.builder()
                .id(entity.getId())
                .templateName(entity.getTemplateName())
                .templateContent(entity.getTemplateContent())
                .category(entity.getCategory())
                .remark(entity.getRemark())
                .build();
    }
}
