package com.campus.common.log;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志事件
 *
 * <p>由 common 模块的日志切面发布，system 模块监听后异步落库，
 * 避免公共模块反向依赖业务模块。</p>
 *
 * @author campus-mart
 */
@Data
public class OperLogEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模块标题 */
    private String title;

    /** 业务类型 0其它 1新增 2修改 3删除 4授权 5导出 ... */
    private String businessType;

    /** 方法名称 */
    private String method;

    /** 请求方式 */
    private String requestMethod;

    /** 操作类别 0其它 1后台用户 2手机端用户 */
    private String operatorType;

    /** 操作人员 */
    private String operName;

    /** 部门名称 */
    private String deptName;

    /** 请求URL */
    private String operUrl;

    /** 主机地址 */
    private String operIp;

    /** 请求参数 */
    private String operParam;

    /** 返回参数 */
    private String jsonResult;

    /** 操作状态 0正常 1异常 */
    private String status;

    /** 错误消息 */
    private String errorMsg;

    /** 消耗时间(毫秒) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
