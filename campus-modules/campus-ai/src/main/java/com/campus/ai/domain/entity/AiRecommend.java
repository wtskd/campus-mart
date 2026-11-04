package com.campus.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ai_recommend 实体
 *
 * @author campus-mart
 */
@Data
@TableName("ai_recommend")
public class AiRecommend {

    /**  */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**  */
    private Long userId;

    /**  */
    private Long itemId;

    /**  */
    private BigDecimal score;

    /** 推荐理由，如 同校同分类 */
    private String reason;

    /** home/detail/cart */
    private String scene;

    /**  */
    private LocalDate batchDate;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
