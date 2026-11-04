package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_banner 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_banner")
public class BizBanner {

    /**  */
    @TableId(value = "banner_id", type = IdType.AUTO)
    private Long bannerId;

    /**  */
    private String title;

    /**  */
    private String imageUrl;

    /**  */
    private String linkUrl;

    /**  */
    private Integer sort;

    /** 0上架 1下架 */
    private String status;

    /**  */
    private LocalDateTime startTime;

    /**  */
    private LocalDateTime endTime;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
