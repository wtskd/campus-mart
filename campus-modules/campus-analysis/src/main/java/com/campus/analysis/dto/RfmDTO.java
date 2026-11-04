package com.campus.analysis.dto;

import com.campus.common.core.domain.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * RFM 用户分层结果
 *
 * @author campus-mart
 */
@Data
@Schema(description = "RFM 用户分层")
public class RfmDTO {

    @Schema(description = "统计日期 yyyy-MM-dd")
    private String calcDate;

    @Schema(description = "统计用户总数")
    private Long totalUsers;

    @Schema(description = "成交金额合计")
    private BigDecimal totalMonetary;

    @Schema(description = "各人群汇总")
    private List<RfmSegmentDTO> segments = new ArrayList<>();

    @Schema(description = "用户明细分页")
    private PageResult<RfmUserDTO> detail;
}
