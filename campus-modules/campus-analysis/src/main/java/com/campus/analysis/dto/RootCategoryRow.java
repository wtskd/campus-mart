package com.campus.analysis.dto;

import lombok.Data;

/**
 * 一级分类行
 *
 * @author campus-mart
 */
@Data
public class RootCategoryRow {

    /** 分类 ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;
}
