package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户新增/修改入参。
 *
 * <p>单独定义 DTO 而不是直接用实体：前端提交的 roleIds 不属于 sys_user 表，
 * 且 password 只允许在新增时写入，用实体接参容易把密码/删除标记一起更新掉。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "用户新增/修改参数")
public class SysUserSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID，修改时必填")
    private Long userId;

    private Long deptId;

    private String username;

    private String nickname;

    @Schema(description = "用户类型 00后台 01学生")
    private String userType;

    @Schema(description = "明文密码，仅新增时使用；为空则使用默认 123456")
    private String password;

    private String phone;

    private String email;

    private String avatar;

    @Schema(description = "性别 0男 1女 2未知")
    private String gender;

    private String studentNo;

    private Integer creditScore;

    @Schema(description = "状态 0正常 1停用")
    private String status;

    private String remark;

    @Schema(description = "角色ID集合")
    private List<Long> roleIds;
}
