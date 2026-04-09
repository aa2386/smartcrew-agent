package com.smartcrew.agent.api.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper。
 */
@Mapper
public interface ScUserMapper extends BaseMapper<ScUser> {

    /**
     * 按用户名查询用户。
     */
    @Select("select * from sc_user where username = #{username} limit 1")
    ScUser selectByUsername(@Param("username") String username);
}
