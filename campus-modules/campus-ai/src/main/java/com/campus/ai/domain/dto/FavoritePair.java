package com.campus.ai.domain.dto;

import lombok.Data;

/**
 * 收藏关系（ItemCF 共现计算用）
 *
 * @author campus-mart
 */
@Data
public class FavoritePair {

    private Long itemId;

    private Long userId;
}
