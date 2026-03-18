package com.smartcrew.agent.controller.memory;

import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;
import com.smartcrew.agent.api.memory.service.UserPreferenceService;
import com.smartcrew.agent.common.domain.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 记忆控制器，提供用户偏好查询与写入接口。
 */
@RestController
@RequestMapping("/api/v1/memory/preferences")
public class MemoryController {

    /**
     * 用户偏好服务。
     */
    private final UserPreferenceService userPreferenceService;

    /**
     * 构造 MemoryController 所需的依赖对象。
     */
    public MemoryController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    /**
     * 查询列表数据。
     */
    @GetMapping("/{userId}")
    public R<List<UserPreferenceVo>> list(@PathVariable("userId") Long userId) {
        return R.ok(userPreferenceService.listByUserId(userId));
    }

    /**
     * 新增或更新数据。
     */
    @PutMapping("/{userId}")
    public R<UserPreferenceVo> upsert(@PathVariable("userId") Long userId,
                                      @Valid @RequestBody UserPreferenceUpsertRequest request) {
        return R.ok(userPreferenceService.upsert(userId, request));
    }
}
