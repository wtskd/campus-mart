package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_file 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_file")
public class SysFile {

    /**  */
    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;

    /** 存储文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 存储路径 */
    private String filePath;

    /** 访问URL */
    private String fileUrl;

    /** 字节 */
    private Long fileSize;

    /** MIME类型 */
    private String fileType;

    /** local/minio */
    private String storage;

    /** 业务类型 item/avatar/... */
    private String bizType;

    /**  */
    private Long createBy;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
