package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品审核请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "商品审核请求")
public class ItemAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long itemId;

    @Schema(description = "是否通过：true 上架 / false 驳回下架")
    @NotNull(message = "审核结论不能为空")
    private Boolean pass;

    @Schema(description = "审核意见")
    @Size(max = 500, message = "审核意见不能超过 500 个字符")
    private String remark;
}
