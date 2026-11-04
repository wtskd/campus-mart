package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_item 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_item")
public class BizItem {

    /** 商品ID */
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    /** 商品编号 */
    private String itemNo;

    /** 卖家ID */
    private Long sellerId;

    /** 分类ID */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 详细描述 */
    private String description;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 成色 1-10（10全新） */
    private Integer itemCondition;

    /** 1出售 2求购 3交换 */
    private Integer tradeType;

    /** 封面图 */
    private String coverUrl;

    /** 图片列表 */
    private String images;

    /** 标签数组 */
    private String tags;

    /** 交易地点 */
    private String location;

    /** 0站内 1手机 2微信 */
    private Integer contactType;

    /** 是否AI生成 0否 1是 */
    private String aiGenerated;

    /**  */
    private Integer viewCount;

    /**  */
    private Integer favoriteCount;

    /** 0草稿 1待审核 2在售 3已预订 4已售出 5下架 6违规 */
    private Integer status;

    /** 审核意见 */
    private String auditRemark;

    /**  */
    private LocalDateTime publishTime;

    /**  */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
