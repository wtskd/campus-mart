package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_report 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_report")
public class BizReport {

    /**  */
    @TableId(value = "report_id", type = IdType.AUTO)
    private Long reportId;

    /**  */
    private Long itemId;

    /**  */
    private Long reporterId;

    /** 字典 report_reason */
    private String reasonType;

    /**  */
    private String content;

    /**  */
    private String images;

    /** 0待处理 1已处理 2驳回 */
    private Integer status;

    /**  */
    private Long handleUserId;

    /**  */
    private String handleRemark;

    /**  */
    private LocalDateTime handleTime;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
