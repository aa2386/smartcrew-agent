package com.smartcrew.agent.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.memory.domain.entity.UserPreference;
import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;
import com.smartcrew.agent.api.memory.mapper.UserPreferenceMapper;
import com.smartcrew.agent.api.memory.service.UserPreferenceService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台长期偏好管理控制器。
 */
@RestController
@RequestMapping("/api/admin/preferences")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminPreferenceController {

    /**
     * 用户偏好服务。
     */
    private final UserPreferenceService userPreferenceService;

    /**
     * 用户偏好 Mapper。
     */
    private final UserPreferenceMapper userPreferenceMapper;

    public AdminPreferenceController(UserPreferenceService userPreferenceService,
                                     UserPreferenceMapper userPreferenceMapper) {
        this.userPreferenceService = userPreferenceService;
        this.userPreferenceMapper = userPreferenceMapper;
    }

    @GetMapping
    public TableDataInfo<UserPreferenceVo> list(@RequestParam(value = "userId", required = false) Long userId) {
        var rows = userPreferenceMapper.selectList(Wrappers.lambdaQuery(UserPreference.class)
                        .eq(userId != null, UserPreference::getUserId, userId)
                        .orderByAsc(UserPreference::getUserId)
                        .orderByAsc(UserPreference::getPrefKey))
                .stream()
                .map(this::toVo)
                .toList();
        return TableDataInfo.build(rows);
    }

    @PutMapping("/{userId}")
    public R<UserPreferenceVo> upsert(@PathVariable("userId") Long userId,
                                      @Valid @RequestBody UserPreferenceUpsertRequest request) {
        return R.ok(userPreferenceService.upsert(userId, request));
    }

    @DeleteMapping("/{userId}/{prefKey}")
    public R<Void> delete(@PathVariable("userId") Long userId, @PathVariable("prefKey") String prefKey) {
        userPreferenceService.delete(userId, prefKey);
        return R.ok("\u5220\u9664\u6210\u529f", null);
    }

    /**
     * 转换为偏好视图对象。
     */
    private UserPreferenceVo toVo(UserPreference entity) {
        return UserPreferenceVo.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .prefKey(entity.getPrefKey())
                .prefValue(entity.getPrefValue())
                .prefType(entity.getPrefType())
                .source(entity.getSource())
                .build();
    }
}
