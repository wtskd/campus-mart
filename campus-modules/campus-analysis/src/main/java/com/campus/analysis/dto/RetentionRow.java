package com.campus.analysis.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 留存统计行（按活跃日期分组）
 *
 * @author campus-mart
 */
@Data
public class RetentionRow {

    /** 活跃日期（队列日期） */
    private LocalDate cohortDate;

    /** 当日活跃用户数 */
    private Long activeUsers;

    /** 次日留存用户数 */
    private Long retained1;

    /** 3 日留存用户数（活跃后第 3 天） */
    private Long retained3;

    /** 7 日留存用户数（活跃后第 7 天） */
    private Long retained7;
}
