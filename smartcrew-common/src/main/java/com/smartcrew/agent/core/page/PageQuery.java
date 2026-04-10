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
    private Integer pageNum;
    /**
     * 每页条数。
     */
    private Integer pageSize;

    /**
     * 当前请求是否显式携带了分页参数。
     */
    public boolean hasPaging() {
        return pageNum != null || pageSize != null;
    }

    /**
     * 构建目标对象。
     */
    public <T> Page<T> build() {
        int currentPage = pageNum == null ? 1 : Math.max(pageNum, 1);
        int currentSize = pageSize == null ? 10 : Math.max(pageSize, 1);
        return new Page<>(currentPage, currentSize);
    }
}
