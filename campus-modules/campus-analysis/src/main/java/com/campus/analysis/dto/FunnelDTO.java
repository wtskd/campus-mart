package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 转化漏斗 DTO
 *
 * @author campus-mart
 */
@Data
@Schema(description = "转化漏斗")
public class FunnelDTO {

    @Schema(description = "统计天数")
    private Integer days;

    @Schema(description = "商品曝光数（统计区间内发布商品的 view_count 汇总）")
    private Long viewCount;

    @Schema(description = "收藏数")
    private Long favoriteCount;

    @Schema(description = "下单数")
    private Long orderCount;

    @Schema(description = "成交数（status=2）")
    private Long finishCount;

    @Schema(description = "曝光->收藏 转化率")
    private Double viewToFavorite;

    @Schema(description = "收藏->下单 转化率")
    private Double favoriteToOrder;

    @Schema(description = "下单->成交 转化率")
    private Double orderToFinish;

    @Schema(description = "曝光->成交 转化率")
    private Double viewToFinish;

    @Schema(description = "漏斗层级明细")
    private List<FunnelStageDTO> stages = new ArrayList<>();
}
