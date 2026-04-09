package com.smartcrew.agent.core.auth;

import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.domain.entity.ScUserIdentity;
import com.smartcrew.agent.api.user.mapper.ScUserIdentityMapper;
import com.smartcrew.agent.api.user.mapper.ScUserMapper;
import com.smartcrew.agent.common.config.SmartCrewSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.SQLSyntaxErrorException;

/**
 * 默认管理员账号引导器。
 */
@Component
public class AdminAccountBootstrapRunner implements ApplicationRunner {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(AdminAccountBootstrapRunner.class);

    /**
     * 密码编码器。
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 安全配置。
     */
    private final SmartCrewSecurityProperties securityProperties;

    /**
     * 用户 Mapper。
     */
    private final ScUserMapper scUserMapper;

    /**
     * 身份映射 Mapper。
     */
    private final ScUserIdentityMapper scUserIdentityMapper;

    public AdminAccountBootstrapRunner(SmartCrewSecurityProperties securityProperties,
                                       ScUserMapper scUserMapper,
                                       ScUserIdentityMapper scUserIdentityMapper) {
        this.securityProperties = securityProperties;
        this.scUserMapper = scUserMapper;
        this.scUserIdentityMapper = scUserIdentityMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        SmartCrewSecurityProperties.BootstrapAdmin bootstrapAdmin = securityProperties.getAuth().getBootstrapAdmin();
        if (!bootstrapAdmin.isEnabled()) {
            return;
        }

        try {
            if (scUserMapper.selectByUsername(bootstrapAdmin.getUsername()) != null) {
                return;
            }
            initAdminAccount(bootstrapAdmin);
        } catch (RuntimeException ex) {
            if (schemaNotReady(ex)) {
                log.warn("检测到用户体系表尚未初始化，跳过默认管理员创建。原因: {}", extractRootMessage(ex));
                return;
            }
            throw ex;
        }
    }

    /**
     * 初始化默认管理员账号与本地身份。
     */
    private void initAdminAccount(SmartCrewSecurityProperties.BootstrapAdmin bootstrapAdmin) {
        ScUser adminUser = new ScUser();
        adminUser.setUsername(bootstrapAdmin.getUsername());
        adminUser.setPasswordHash(PASSWORD_ENCODER.encode(bootstrapAdmin.getPassword()));
        adminUser.setDisplayName(bootstrapAdmin.getDisplayName());
        adminUser.setRole("ADMIN");
        adminUser.setStatus("ENABLED");
        adminUser.setRemark("系统自动初始化管理员");
        scUserMapper.insert(adminUser);

        ScUserIdentity identity = new ScUserIdentity();
        identity.setUserId(adminUser.getId());
        identity.setProvider("LOCAL");
        identity.setProviderUserId(adminUser.getUsername());
        identity.setTenantKey("");
        identity.setRemark("系统自动初始化管理员本地身份");
        scUserIdentityMapper.insert(identity);
    }

    /**
     * 判断是否属于库表尚未准备好的兼容场景。
     */
    private boolean schemaNotReady(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof BadSqlGrammarException || cursor instanceof SQLSyntaxErrorException) {
                return true;
            }
            if (cursor instanceof DataAccessException && cursor.getMessage() != null && cursor.getMessage().contains("sc_user")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    /**
     * 提取最内层异常消息，便于日志定位。
     */
    private String extractRootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? throwable.getClass().getSimpleName() : cursor.getMessage();
    }
}
