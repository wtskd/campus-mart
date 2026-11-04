package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台评价列表查询条件
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台评价列表查询条件")
public class CommentQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "评价人ID")
    private Long fromUserId;

    @Schema(description = "状态 0正常 1隐藏 2审核未通过")
    @Pattern(regexp = "^[012]$", message = "评价状态只能为 0、1、2")
    private String status;

    @Schema(description = "评价内容关键字")
    @Size(max = 50, message = "关键字不能超过 50 个字符")
    private String keyword;
}
