package com.campus.system.service;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.system.domain.dto.FileUploadVO;
import com.campus.system.domain.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务。
 *
 * @author campus-mart
 */
public interface SysFileService {

    /** 单文件上传（前端兼容接口的扩展名），bizType 可为空 */
    FileUploadVO upload(MultipartFile file, String bizType);

    /** 多文件上传（前端 /file/uploads 用，字段名 files） */
    java.util.List<FileUploadVO> uploadBatch(MultipartFile[] files, String bizType);

    PageResult<SysFile> page(PageQuery pageQuery, String originalName, String fileType, String bizType);

    void remove(String fileIds);
}
