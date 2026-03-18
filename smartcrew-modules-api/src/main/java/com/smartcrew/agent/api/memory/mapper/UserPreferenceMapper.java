package com.smartcrew.agent.api.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.memory.domain.entity.UserPreference;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * UserPreferenceMapper 接口，负责对应领域对象的数据访问操作。
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {

    @Select("select * from user_preference where user_id = #{userId}")
    List<UserPreference> selectByUserId(@Param("userId") Long userId);

    @Select("select * from user_preference where user_id = #{userId} and pref_key = #{prefKey} limit 1")
    UserPreference selectByUserIdAndPrefKey(@Param("userId") Long userId, @Param("prefKey") String prefKey);

    @Delete("delete from user_preference where user_id = #{userId} and pref_key = #{prefKey}")
    int deleteByUserIdAndPrefKey(@Param("userId") Long userId, @Param("prefKey") String prefKey);
}
