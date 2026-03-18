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

    /**
     * 按用户 ID 查询偏好列表。
     *
     * @param userId 用户 ID。
     * @return 结果列表。
     */
    @Select("select * from user_preference where user_id = #{userId}")
    List<UserPreference> selectByUserId(@Param("userId") Long userId);

    /**
     * 按用户 ID 和偏好键查询偏好。
     *
     * @param userId 用户 ID。
     * @param prefKey 偏好键。
     * @return 匹配到的偏好记录；未命中时返回 `null`。
     */
    @Select("select * from user_preference where user_id = #{userId} and pref_key = #{prefKey} limit 1")
    UserPreference selectByUserIdAndPrefKey(@Param("userId") Long userId, @Param("prefKey") String prefKey);

    /**
     * 按用户 ID 和偏好键删除偏好。
     *
     * @param userId 用户 ID。
     * @param prefKey 偏好键。
     * @return 受影响的行数。
     */
    @Delete("delete from user_preference where user_id = #{userId} and pref_key = #{prefKey}")
    int deleteByUserIdAndPrefKey(@Param("userId") Long userId, @Param("prefKey") String prefKey);
}
