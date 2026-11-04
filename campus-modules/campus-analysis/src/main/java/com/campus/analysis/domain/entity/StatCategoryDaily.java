package com.campus.analysis.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * stat_category_daily 实体
 *
 * @author campus-mart
 */
@Data
@TableName("stat_category_daily")
public class StatCategoryDaily {

    /**  */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**  */
    private LocalDate statDate;

    /**  */
    private Long categoryId;

    /** 发布数 */
    private Integer itemCount;

    /**  */
    private Integer orderCount;

    /**  */
    private BigDecimal gmv;

}
