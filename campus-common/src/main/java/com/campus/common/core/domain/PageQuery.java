package com.campus.common.core.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 通用分页请求参数
 *
 * @author campus-mart
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 */
    @Schema(description = "当前页码，从 1 开始")
    private Long current = 1L;

    /** 每页条数 */
    @Schema(description = "每页条数，最大 100")
    private Long size = 10L;

    /** 排序字段（下划线） */
    @Schema(description = "排序字段，如 create_time")
    private String orderBy;

    /** 是否升序 */
    @Schema(description = "是否升序")
    private Boolean asc = false;

    /** 附加查询参数（如时间区间） */
    @Schema(hidden = true)
    private Map<String, Object> params;

    public <T> Page<T> toPage() {
        long c = (current == null || current < 1) ? 1 : current;
        long s = (size == null || size < 1) ? 10 : Math.min(size, 100);
        return new Page<>(c, s);
    }
}
