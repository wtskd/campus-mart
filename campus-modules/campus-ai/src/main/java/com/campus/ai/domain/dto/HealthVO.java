package com.campus.ai.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 健康检查响应
 *
 * @author campus-mart
 */
@Data
public class HealthVO {

    private String status = "UP";

    private String service = "campus-ai";

    /** 当前配置的主 provider（campus.ai.provider） */
    private String configuredProvider;

    /** 各 provider 状态 */
    private List<ProviderStatusVO> providers = new ArrayList<>();
}
