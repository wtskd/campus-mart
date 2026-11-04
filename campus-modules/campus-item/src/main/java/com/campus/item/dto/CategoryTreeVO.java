package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点
 *
 * @author campus-mart
 */
@Data
@Schema(description = "分类树节点")
public class CategoryTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "父分类ID，0 为一级")
    private Long parentId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序号，升序")
    private Integer orderNum;

    @Schema(description = "状态 0正常 1停用")
    private String status;

    @Schema(description = "子分类")
    private List<CategoryTreeVO> children = new ArrayList<>();
}
