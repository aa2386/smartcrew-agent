# Agent 开发指南

## Agent 接口定义

Agent 是 SmartCrew 平台的核心抽象，所有智能体都需要实现 `com.smartcrew.agent.api.agent.service.Agent` 接口。

### 接口方法

```java
public interface Agent {
    String code();
    String name();
    boolean supports(String capability);
    AgentDispatchResponse handle(AgentDispatchCommand command);
}
```

### 方法说明

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `code()` | String | Agent 唯一标识，全局不可重复 |
| `name()` | String | Agent 显示名称 |
| `supports(String capability)` | boolean | 判断是否支持某项能力 |
| `handle(AgentDispatchCommand command)` | AgentDispatchResponse | 处理用户指令 |

## 开发步骤

### 第一步：创建 Agent 类

```java
package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyCustomAgent implements Agent {

    @Override
    public String code() {
        return "my-custom-agent";
    }

    @Override
    public String name() {
        return "自定义智能体";
    }

    @Override
    public boolean supports(String capability) {
        return "custom-chat".equalsIgnoreCase(capability);
    }

    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        return AgentDispatchResponse.builder()
                .traceId(command.getTraceId())
                .agentCode(code())
                .accepted(true)
                .message("处理成功")
                .build();
    }
}
```

### 第二步：注册到数据库

在 `agent_definition` 表中插入记录：

```sql
INSERT INTO agent_definition (agent_code, agent_name, agent_type, enabled)
VALUES ('my-custom-agent', '自定义智能体', 'custom', 1);
```

### 第三步：绑定提示词

```sql
INSERT INTO agent_prompt_binding (agent_code, prompt_template_id, sort_order)
VALUES ('my-custom-agent', 1, 1);
```

## Agent 类型

| 类型 | 说明 | 示例 |
|------|------|------|
| initial | 初始智能体，处理用户首次请求 | InitialAgent |
| routing | 路由智能体，分发到其他 Agent | RoutingAgent |
| tool | 工具智能体，执行特定工具 | ToolAgent |
| custom | 自定义智能体 | 用户自定义 |

## 最佳实践

1. **单一职责**：每个 Agent 只负责一类任务
2. **能力明确**：`supports` 方法要精确匹配能力
3. **错误处理**：`handle` 方法要处理异常情况
4. **日志记录**：关键操作要记录日志

## 常见问题

### Agent 未被识别

检查是否添加了 `@Component` 注解，确保 Spring 能够扫描到。

### 能力匹配失败

检查 `supports` 方法的实现，确保能力字符串匹配正确。

### 响应格式错误

确保返回 `AgentDispatchResponse` 对象，而不是其他类型。
