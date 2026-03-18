package com.smartcrew.agent.core.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询参数对象，用于构建 MyBatis-Plus 分页请求。
 */
@Data
public class PageQuery implements Serializable {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码。
     */
    private Integer pageNum = 1;
    /**
     * 每页条数。
     */
    private Integer pageSize = 10;

    /**
     * 构建目标对象。
     */
    public <T> Page<T> build() {
        return new Page<>(Math.max(pageNum, 1), Math.max(pageSize, 1));
    }
}
