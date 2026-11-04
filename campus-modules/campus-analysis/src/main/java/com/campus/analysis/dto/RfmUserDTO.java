package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * RFM 用户明细
 *
 * @author campus-mart
 */
@Data
@Schema(description = "RFM 用户明细")
public class RfmUserDTO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "院系名称")
    private String deptName;

    @Schema(description = "R：最近一次成交距今天数")
    private Integer recencyDays;

    @Schema(description = "F：成交次数")
    private Integer frequency;

    @Schema(description = "M：成交金额")
    private BigDecimal monetary;

    @Schema(description = "R 得分 1-5")
    private Integer rScore;

    @Schema(description = "F 得分 1-5")
    private Integer fScore;

    @Schema(description = "M 得分 1-5")
    private Integer mScore;

    @Schema(description = "人群分层")
    private String segment;
}
