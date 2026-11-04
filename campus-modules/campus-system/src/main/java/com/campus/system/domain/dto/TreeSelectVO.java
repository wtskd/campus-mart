package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用下拉树节点（部门树、菜单树共用）。
 *
 * <p>id/label/children 是 Element Plus el-tree-select 的默认字段约定，
 * 前端不需要再写 props 映射。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "下拉树节点")
public class TreeSelectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String label;

    private List<TreeSelectVO> children = new ArrayList<>();
}
