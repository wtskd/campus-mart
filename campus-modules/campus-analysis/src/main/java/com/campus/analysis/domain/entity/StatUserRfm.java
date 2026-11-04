package com.campus.analysis.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * stat_user_rfm 实体
 *
 * @author campus-mart
 */
@Data
@TableName("stat_user_rfm")
public class StatUserRfm {

    /**  */
    @TableId(value = "user_id")
    private Long userId;

    /** 最近一次交易距今天数 */
    private Integer recencyDays;

    /** 交易次数 */
    private Integer frequency;

    /** 交易金额 */
    private BigDecimal monetary;

    /**  */
    private Integer rScore;

    /**  */
    private Integer fScore;

    /**  */
    private Integer mScore;

    /** 用户分层 */
    private String segment;

    /**  */
    private LocalDate calcDate;

}
