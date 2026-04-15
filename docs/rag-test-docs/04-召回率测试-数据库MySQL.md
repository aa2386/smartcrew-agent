# 数据库配置指南 - MySQL

## 概述

SmartCrew 支持使用 MySQL 作为主数据库，本文档介绍 MySQL 数据库的配置方法。

## 依赖配置

在 `pom.xml` 中添加 MySQL 驱动依赖：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
</dependency>
```

## 连接配置

在 `application.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/smartcrew?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

## 连接池配置

推荐使用 HikariCP 连接池：

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      pool-name: SmartCrewHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
```

## 初始化脚本

执行 `sql/init-smartcrew-agent.sql` 脚本初始化数据库表结构：

```bash
mysql -u root -p smartcrew < sql/init-smartcrew-agent.sql
```

## 常见问题

### 连接超时

检查防火墙设置和 MySQL 服务状态：

```bash
# 检查 MySQL 服务
systemctl status mysql

# 检查端口
netstat -tlnp | grep 3306
```

### 字符集问题

确保数据库和表使用 UTF-8 编码：

```sql
CREATE DATABASE smartcrew CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
