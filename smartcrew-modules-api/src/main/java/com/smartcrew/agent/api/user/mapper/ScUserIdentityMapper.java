package com.smartcrew.agent.api.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.user.domain.entity.ScUserIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户身份映射 Mapper。
 */
@Mapper
public interface ScUserIdentityMapper extends BaseMapper<ScUserIdentity> {

    /**
     * 按身份信息查询映射。
     */
    @Select("""
            select *
            from sc_user_identity
            where provider = #{provider}
              and provider_user_id = #{providerUserId}
              and tenant_key = #{tenantKey}
            limit 1
            """)
    ScUserIdentity selectByIdentity(@Param("provider") String provider,
                                    @Param("providerUserId") String providerUserId,
                                    @Param("tenantKey") String tenantKey);

    /**
     * 查询用户的全部身份映射。
     */
    @Select("select * from sc_user_identity where user_id = #{userId} order by id desc")
    List<ScUserIdentity> selectByUserId(@Param("userId") Long userId);
}
