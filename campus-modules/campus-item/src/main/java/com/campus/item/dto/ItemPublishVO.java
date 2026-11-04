package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 发布/编辑商品结果
 *
 * @author campus-mart
 */
@Data
@Schema(description = "发布/编辑商品结果")
public class ItemPublishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "商品编号")
    private String itemNo;

    @Schema(description = "状态 1待审核 2在售 6违规")
    private Integer status;

    @Schema(description = "内容审核是否通过")
    private Boolean auditPass;

    @Schema(description = "审核意见")
    private String auditRemark;

    @Schema(description = "提示信息")
    private String message;
}
