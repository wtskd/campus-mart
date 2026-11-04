package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字典下拉选项：{ "label":"在售", "value":"2", "cssClass":null, "isDefault":"N" }
 *
 * <p>这是前端（后台 + 小程序）高频调用的数据，会整体缓存在 Redis 的 dict:{dictType} 里，
 * 所以保持扁平、不嵌套实体。</p>
 *
 * @author campus-mart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字典下拉项")
public class DictOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;

    private String value;

    private String cssClass;

    private String isDefault;

    /** 标签样式（success/warning/danger...），后台表格用 el-tag 展示状态时使用 */
    private String listClass;
}
