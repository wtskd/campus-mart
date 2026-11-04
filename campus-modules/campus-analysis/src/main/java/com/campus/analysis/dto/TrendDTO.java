package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 趋势图数据
 *
 * @author campus-mart
 */
@Data
@Schema(description = "趋势图数据")
public class TrendDTO {

    @Schema(description = "统计天数")
    private Integer days;

    @Schema(description = "日期列表 yyyy-MM-dd（按时间升序）")
    private List<String> dates = new ArrayList<>();

    @Schema(description = "每日数据点")
    private List<TrendPointDTO> points = new ArrayList<>();

    @Schema(description = "区间成交额合计")
    private BigDecimal totalGmv;

    @Schema(description = "区间下单数合计")
    private Long totalOrderCount;

    @Schema(description = "区间成交订单数合计")
    private Long totalFinishOrderCount;

    @Schema(description = "区间新增用户数合计")
    private Long totalNewUserCount;

    @Schema(description = "区间新增商品数合计")
    private Long totalNewItemCount;

    @Schema(description = "区间 AI 调用次数合计")
    private Long totalAiCallCount;
}
