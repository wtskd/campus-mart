package com.campus.common.core.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果
 *
 * @author campus-mart
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "当前页码")
    private long current;

    @Schema(description = "每页条数")
    private long size;

    @Schema(description = "总页数")
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(page.getRecords());
        r.setTotal(page.getTotal());
        r.setCurrent(page.getCurrent());
        r.setSize(page.getSize());
        r.setPages(page.getPages());
        return r;
    }

    /** 实体分页 -> VO 分页 */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> mapper) {
        PageResult<V> r = new PageResult<>();
        List<V> list = page.getRecords() == null ? Collections.emptyList()
                : page.getRecords().stream().map(mapper).collect(Collectors.toList());
        r.setRecords(list);
        r.setTotal(page.getTotal());
        r.setCurrent(page.getCurrent());
        r.setSize(page.getSize());
        r.setPages(page.getPages());
        return r;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> r = new PageResult<>();
        r.setRecords(Collections.emptyList());
        r.setTotal(0);
        r.setCurrent(1);
        r.setSize(10);
        r.setPages(0);
        return r;
    }
}
