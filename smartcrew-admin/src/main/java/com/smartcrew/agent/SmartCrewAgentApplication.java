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
        printSuccessBanner();
    }

    /**
     * 打印启动成功横幅。
     */
    private static void printSuccessBanner() {
        System.out.println();
        System.out.println("\u001B[32m" + "╔════════════════════════════════════════════════════════════════════════════════════════════════════════╗" + "\u001B[0m");
        System.out.println("\u001B[32m" + "║" + "\u001B[0m" + "                                                                                                        \u001B[32m" + "║" + "\u001B[0m");
        System.out.println("\u001B[32m" + "║" + "\u001B[0m" + "     \u001B[1;32m✓ SmartCrew-Agent 启动成功！\u001B[0m" + "                                                                         \u001B[32m" + "║" + "\u001B[0m");
        System.out.println("\u001B[32m" + "║" + "\u001B[0m" + "     \u001B[36m➜  访问地址: \u001B[1;36mhttp://localhost:8085\u001B[0m" + "                                                                  \u001B[32m" + "║" + "\u001B[0m");
        System.out.println("\u001B[32m" + "║" + "\u001B[0m" + "                                                                                                        \u001B[32m" + "║" + "\u001B[0m");
        System.out.println("\u001B[32m" + "╚════════════════════════════════════════════════════════════════════════════════════════════════════════╝" + "\u001B[0m");
        System.out.println();
    }
}
