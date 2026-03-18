package com.smartcrew.agent.core.mcp;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.mcp.domain.entity.McpInfo;
import com.smartcrew.agent.api.mcp.domain.request.McpInfoRequest;
import com.smartcrew.agent.api.mcp.domain.vo.McpInfoVo;
import com.smartcrew.agent.api.mcp.mapper.McpInfoMapper;
import com.smartcrew.agent.api.mcp.service.McpInfoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * MCP 服务实现，负责 MCP 服务配置的新增、更新与查询。
 */
@Service
public class McpInfoServiceImpl implements McpInfoService {

    /**
     * MCP 服务数据访问对象。
     */
    private final McpInfoMapper mcpInfoMapper;

    /**
     * 构造 McpInfoServiceImpl 所需的依赖对象。
     */
    public McpInfoServiceImpl(McpInfoMapper mcpInfoMapper) {
        this.mcpInfoMapper = mcpInfoMapper;
    }

    /**
     * 保存或更新数据。
     */
    @Override
    public McpInfoVo saveOrUpdate(McpInfoRequest request) {
        McpInfo entity = mcpInfoMapper.selectByServerName(request.getServerName());
        if (entity == null) {
            entity = new McpInfo();
        }
        entity.setServerName(request.getServerName());
        entity.setTransportType(request.getTransportType());
        entity.setCommand(request.getCommand());
        entity.setArguments(request.getArguments());
        entity.setEnv(request.getEnv());
        entity.setStatus(request.getStatus());
        entity.setDescription(request.getDescription());
        if (entity.getMcpId() == null) {
            mcpInfoMapper.insert(entity);
        } else {
            mcpInfoMapper.updateById(entity);
        }
        return toVo(entity);
    }

    /**
     * 查询全部数据。
     */
    @Override
    public List<McpInfoVo> listAll() {
        return mcpInfoMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 根据服务端名称查询数据。
     */
    @Override
    public Optional<McpInfoVo> findByServerName(String serverName) {
        return Optional.ofNullable(mcpInfoMapper.selectByServerName(serverName)).map(this::toVo);
    }

    /**
     * 将 MCP 服务实体转换为视图对象。
     */
    private McpInfoVo toVo(McpInfo entity) {
        return McpInfoVo.builder()
                .mcpId(entity.getMcpId())
                .serverName(entity.getServerName())
                .transportType(entity.getTransportType())
                .command(entity.getCommand())
                .arguments(entity.getArguments())
                .env(entity.getEnv())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .build();
    }
}
