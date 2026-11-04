package com.campus.ai.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品卖点摘要响应
 *
 * @author campus-mart
 */
@Data
public class SummaryResponse {

    private Long itemId;

    private String title;

    private String summary;

    private List<String> points = new ArrayList<>();

    private String provider;

    private Long costMs;
}
