package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_category 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_category")
public class BizCategory {

    /**  */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /** 父分类，0为一级 */
    private Long parentId;

    /**  */
    private String categoryName;

    /**  */
    private String icon;

    /**  */
    private Integer orderNum;

    /** 0正常 1停用 */
    private String status;

    /**  */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
