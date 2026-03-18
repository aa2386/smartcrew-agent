package com.smartcrew.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartCrew-Agent 应用启动入口，负责初始化 Spring Boot 容器并扫描项目组件。
 */
@MapperScan(basePackages = "com.smartcrew.agent.api", markerInterface = BaseMapper.class)
@SpringBootApplication(scanBasePackages = "com.smartcrew.agent")
public class SmartCrewAgentApplication {

    /**
     * 启动 Spring Boot 应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartCrewAgentApplication.class, args);
        System.out.println("项目启动成功");
    }
}
