package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_comment 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_comment")
public class BizComment {

    /**  */
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long commentId;

    /**  */
    private Long orderId;

    /**  */
    private Long itemId;

    /** 评价人 */
    private Long fromUserId;

    /** 被评价人 */
    private Long toUserId;

    /** 1-5 星 */
    private Integer score;

    /**  */
    private String content;

    /**  */
    private String images;

    /** 卖家回复 */
    private String replyContent;

    /** 0正常 1隐藏 */
    private String status;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
