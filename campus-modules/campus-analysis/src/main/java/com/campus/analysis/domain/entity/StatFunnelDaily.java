package com.campus.analysis.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;

/**
 * stat_funnel_daily 实体
 *
 * @author campus-mart
 */
@Data
@TableName("stat_funnel_daily")
public class StatFunnelDaily {

    /**  */
    private LocalDate statDate;

    /** 浏览 */
    private Integer viewCount;

    /** 收藏 */
    private Integer favoriteCount;

    /** 下单 */
    private Integer orderCount;

    /** 支付 */
    private Integer payCount;

    /** 完成 */
    private Integer finishCount;

}
