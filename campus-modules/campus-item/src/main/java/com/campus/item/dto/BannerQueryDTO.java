package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台轮播分页查询参数
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台轮播分页查询参数")
public class BannerQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标题（模糊匹配）")
    private String title;

    @Schema(description = "状态：0上架 1下架")
    private String status;
}
