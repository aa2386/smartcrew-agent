# 数据库配置指南 - MyBatis-Plus

## 概述

SmartCrew 使用 MyBatis-Plus 作为 ORM 框架，简化数据库操作。

## 依赖配置

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.11</version>
</dependency>
```

## 基础配置

```yaml
mybatis-plus:
  type-aliases-package: com.smartcrew.agent.api
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 实体类定义

```java
@Data
@TableName("sc_user")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String displayName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
```

## Mapper 接口

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM sc_user WHERE username = #{username}")
    User selectByUsername(String username);
}
```

## 分页插件

```java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

## 常用查询

```java
// 条件查询
List<User> users = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, "active")
        .orderByDesc(User::getCreateTime)
);

// 分页查询
Page<User> page = userMapper.selectPage(
    new Page<>(1, 10),
    new LambdaQueryWrapper<User>()
        .like(User::getUsername, "admin")
);
```
