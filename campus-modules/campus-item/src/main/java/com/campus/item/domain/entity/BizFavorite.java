package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_favorite 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_favorite")
public class BizFavorite {

    /**  */
    @TableId(value = "favorite_id", type = IdType.AUTO)
    private Long favoriteId;

    /**  */
    private Long userId;

    /**  */
    private Long itemId;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
