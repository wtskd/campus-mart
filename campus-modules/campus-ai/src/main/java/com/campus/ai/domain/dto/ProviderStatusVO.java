package com.campus.ai.domain.dto;

import lombok.Data;

/**
 * 单个 Provider 状态
 *
 * @author campus-mart
 */
@Data
public class ProviderStatusVO {

    private String name;

    private Boolean available;

    private String model;

    public ProviderStatusVO() {
    }

    public ProviderStatusVO(String name, Boolean available, String model) {
        this.name = name;
        this.available = available;
        this.model = model;
    }
}
