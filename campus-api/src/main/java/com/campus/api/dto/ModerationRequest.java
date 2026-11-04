package com.campus.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 内容审核请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "内容审核请求")
public class ModerationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;

    private String title;

    private String description;

    /** 场景：item 商品 / comment 评论 / message 私信 */
    private String scene = "item";
}
