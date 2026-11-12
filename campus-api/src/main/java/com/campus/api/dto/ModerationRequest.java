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

    /**
     * 待审核的整段文本（后台「在线试用」与评论审核用）
     *
     * <p>title/description 都为空时回退到这里，避免调用方只传一个 content 时
     * 审核到空文本、规则与模型都看到空内容而误判通过。</p>
     */
    private String content;

    /** 场景：item 商品 / comment 评论 / message 私信 */
    private String scene = "item";
}
