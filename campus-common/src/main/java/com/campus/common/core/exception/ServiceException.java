package com.campus.common.core.exception;

/**
 * 业务异常：由全局异常处理器捕获并转换成统一响应体
 *
 * @author campus-mart
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务码，默认 500 */
    private final int code;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static ServiceException of(String message) {
        return new ServiceException(message);
    }

    public static ServiceException of(int code, String message) {
        return new ServiceException(code, message);
    }

    /** 条件成立时抛出 */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new ServiceException(message);
        }
    }

    public int getCode() {
        return code;
    }
}
