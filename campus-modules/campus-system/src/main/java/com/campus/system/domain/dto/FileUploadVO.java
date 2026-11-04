package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 上传结果：{ "fileName":"uuid.png", "url":"/upload/2026/10/27/uuid.png", "size":12345 }
 *
 * <p>url 是相对网关的路径（/upload/**），网关会把该前缀转发到提供静态映射的服务，
 * 前端拼上网关地址即可预览，不依赖具体服务端口。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "文件上传结果")
public class FileUploadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "存储文件名")
    private String fileName;

    @Schema(description = "可访问的相对 URL")
    private String url;

    @Schema(description = "文件大小（字节）")
    private Long size;
}
