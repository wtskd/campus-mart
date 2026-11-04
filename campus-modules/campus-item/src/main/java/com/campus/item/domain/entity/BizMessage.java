package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_message 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_message")
public class BizMessage {

    /**  */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /** 接收人 */
    private Long userId;

    /** 1系统 2订单 3举报 4AI */
    private Integer msgType;

    /**  */
    private String title;

    /**  */
    private String content;

    /** 关联业务ID */
    private Long bizId;

    /** 0未读 1已读 */
    private String isRead;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
