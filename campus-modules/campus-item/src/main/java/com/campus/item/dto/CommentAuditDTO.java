package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 评价审核请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "评价审核请求")
public class CommentAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评价ID")
    @NotNull(message = "评价ID不能为空")
    private Long commentId;

    @Schema(description = "目标状态：0 通过（正常显示）/ 1 隐藏")
    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "^[01]$", message = "目标状态只能为 0（通过）或 1（隐藏）")
    private String status;
}
