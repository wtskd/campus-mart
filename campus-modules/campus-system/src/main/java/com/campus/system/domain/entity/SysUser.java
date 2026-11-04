package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_user 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户ID */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /** 学院ID */
    private Long deptId;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 用户类型 00管理端 01小程序端 */
    private String userType;

    /** BCrypt 密码 */
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 性别 0男 1女 2未知 */
    private String gender;

    /** 学号 */
    private String studentNo;

    /** 信用分 0-100 */
    private Integer creditScore;

    /** 状态 0正常 1停用 */
    private String status;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    private LocalDateTime loginDate;

    /** 备注 */
    private String remark;

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
