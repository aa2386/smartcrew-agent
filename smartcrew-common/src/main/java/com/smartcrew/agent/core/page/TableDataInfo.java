package com.smartcrew.agent.core.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 表格分页响应对象，用于封装列表数据和状态信息。
 */
@Data
public class TableDataInfo<T> implements Serializable {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总记录数。
     */
    private long total;
    /**
     * 数据列表。
     */
    private List<T> rows;
    /**
     * 响应状态码。
     */
    private int code;
    /**
     * 消息内容。
     */
    private String message;

    /**
     * 构建目标对象。
     */
    public static <T> TableDataInfo<T> build(List<T> rows) {
        TableDataInfo<T> data = new TableDataInfo<>();
        data.setTotal(rows.size());
        data.setRows(rows);
        data.setCode(200);
        data.setMessage("success");
        return data;
    }

    /**
     * 构建目标对象。
     */
    public static <T> TableDataInfo<T> build(IPage<T> page) {
        TableDataInfo<T> data = new TableDataInfo<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords());
        data.setCode(200);
        data.setMessage("success");
        return data;
    }
}
