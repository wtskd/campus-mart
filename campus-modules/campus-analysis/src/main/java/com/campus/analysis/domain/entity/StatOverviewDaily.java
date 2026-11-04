package com.campus.analysis.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * stat_overview_daily 实体
 *
 * @author campus-mart
 */
@Data
@TableName("stat_overview_daily")
public class StatOverviewDaily {

    /** 统计日期 */
    private LocalDate statDate;

    /** 成交额 */
    private BigDecimal gmv;

    /** 下单数 */
    private Integer orderCount;

    /** 完成订单数 */
    private Integer finishOrderCount;

    /** 取消订单数 */
    private Integer cancelOrderCount;

    /** 商品发布数 */
    private Integer itemPublishCount;

    /** 成交商品数 */
    private Integer itemSoldCount;

    /** 新增用户数 */
    private Integer newUserCount;

    /** 活跃用户数 */
    private Integer activeUserCount;

    /** 商品浏览数 */
    private Integer viewCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** AI调用次数 */
    private Integer aiCallCount;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
