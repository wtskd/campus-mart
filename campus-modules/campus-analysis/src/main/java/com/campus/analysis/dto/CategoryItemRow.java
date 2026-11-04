package com.campus.analysis.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 分类商品发布按天统计行
 *
 * @author campus-mart
 */
@Data
public class CategoryItemRow {

    /** 统计日期 */
    private LocalDate statDate;

    /** 一级分类 ID */
    private Long categoryId;

    /** 发布商品数 */
    private Long itemCount;
}
