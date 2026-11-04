package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台处理举报请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "后台处理举报请求")
public class ReportHandleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "举报ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "举报ID不能为空")
    private Long reportId;

    @Schema(description = "处理结果：1 通过（举报成立，商品置违规） / 2 驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处理结果不能为空")
    @Pattern(regexp = "^[12]$", message = "处理结果只能为 1（通过）或 2（驳回）")
    private String status;

    @Schema(description = "处理备注")
    @Size(max = 500, message = "处理备注不能超过 500 个字符")
    private String handleRemark;
}
