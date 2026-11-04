package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_oper_log 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    /** 日志ID */
    @TableId(value = "oper_id", type = IdType.AUTO)
    private Long operId;

    /** 模块标题 */
    private String title;

    /** 业务类型 0其他 1新增 2修改 3删除 4查询 5导出 */
    private String businessType;

    /** 方法名 */
    private String method;

    /** HTTP方法 */
    private String requestMethod;

    /** 操作人类别 */
    private String operatorType;

    /** 操作人 */
    private String operName;

    /** 学院 */
    private String deptName;

    /** 请求URL */
    private String operUrl;

    /** IP */
    private String operIp;

    /** 请求参数（脱敏） */
    private String operParam;

    /** 返回结果 */
    private String jsonResult;

    /** 0成功 1失败 */
    private String status;

    /**  */
    private String errorMsg;

    /** 耗时ms */
    private Long costTime;

    /**  */
    private LocalDateTime operTime;

}
