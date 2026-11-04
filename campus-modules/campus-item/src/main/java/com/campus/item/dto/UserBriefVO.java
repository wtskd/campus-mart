package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户概要（跨表查询 sys_user 用，本模块不持有 sys_user 实体）
 *
 * @author campus-mart
 */
@Data
@Schema(description = "用户概要")
public class UserBriefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "信用分 0-100")
    private Integer creditScore;
}
