package com.campus.ai.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 一键发布文案响应
 *
 * @author campus-mart
 */
@Data
public class CopywritingResponse {

    private String title;

    private String summary;

    private String description;

    private List<String> tags = new ArrayList<>();

    /** AI 建议售价（模型给不出时回填用户填写的价格，可为空） */
    private BigDecimal suggestPrice;

    /** 实际生成方 remote / local / mock */
    private String provider;

    /** 耗时（毫秒） */
    private Long costMs;
}
