package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_login_log 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    /**  */
    @TableId(value = "info_id", type = IdType.AUTO)
    private Long infoId;

    /**  */
    private String username;

    /**  */
    private String ipaddr;

    /**  */
    private String loginLocation;

    /**  */
    private String browser;

    /**  */
    private String os;

    /** 0成功 1失败 */
    private String status;

    /**  */
    private String msg;

    /**  */
    private LocalDateTime loginTime;

}
