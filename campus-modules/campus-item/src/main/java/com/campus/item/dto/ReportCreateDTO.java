package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 前台提交举报请求
 *
 * <p>当前库表 biz_report 仅支持商品举报，targetType 非 item 时由服务端返回 400。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "提交举报请求")
public class ReportCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "举报对象类型：item 商品 / comment 评论 / user 用户", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "举报对象类型不能为空")
    @Pattern(regexp = "^(item|comment|user)$", message = "举报对象类型只能为 item、comment 或 user")
    private String targetType;

    @Schema(description = "举报对象ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    @Schema(description = "举报原因（字典 report_reason：fake 虚假信息 / illegal 违禁品 / price 价格违规 / harass 骚扰辱骂 等）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 32, message = "举报原因不能超过 32 个字符")
    private String reason;

    @Schema(description = "补充说明")
    @Size(max = 500, message = "补充说明不能超过 500 个字符")
    private String description;
}
