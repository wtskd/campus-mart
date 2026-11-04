package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_notice 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_notice")
public class BizNotice {

    /**  */
    @TableId(value = "notice_id", type = IdType.AUTO)
    private Long noticeId;

    /**  */
    private String title;

    /**  */
    private String content;

    /** 1公告 2活动 3规则 */
    private Integer noticeType;

    /** 0发布 1草稿 */
    private String status;

    /**  */
    private LocalDateTime publishTime;

    /**  */
    private Long createBy;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
