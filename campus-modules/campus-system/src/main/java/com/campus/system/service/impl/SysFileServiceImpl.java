package com.campus.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.system.domain.dto.FileUploadVO;
import com.campus.system.domain.entity.SysFile;
import com.campus.system.mapper.SysFileMapper;
import com.campus.system.service.SysFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 文件上传实现。
 *
 * <p>只做本地磁盘存储：实训环境没有对象存储，本地目录 + 静态映射（campus-common 的 WebMvcConfig
 * 已把 /upload/** 映射到 campus.upload.path）就能满足图片/附件上传演示。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileServiceImpl implements SysFileService {

    /** 白名单：只放行展示类图片与常用附件，上传可执行后缀等于给服务器开后门 */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "pdf", "zip");

    private static final long MAX_SIZE = 10L * 1024 * 1024;

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String URL_PREFIX = "/upload/";

    @Value("${campus.upload.path:./upload}")
    private String uploadPath;

    private final SysFileMapper fileMapper;

    @Override
    public FileUploadVO upload(MultipartFile file, String bizType) {
        ServiceException.throwIf(file == null || file.isEmpty(), "上传文件不能为空");
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String ext = resolveExt(originalName);
        ServiceException.throwIf(!ALLOWED_EXT.contains(ext), "不支持的文件类型：" + ext);
        ServiceException.throwIf(file.getSize() > MAX_SIZE, "文件大小不能超过 10MB");

        String dateDir = LocalDate.now().format(DATE_DIR);
        String storedName = IdUtil.fastSimpleUUID() + "." + ext;
        Path dir = Paths.get(uploadPath, dateDir).toAbsolutePath().normalize();
        Path target = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("文件保存失败: {}", target, e);
            throw new ServiceException("文件保存失败");
        }

        String url = URL_PREFIX + dateDir + "/" + storedName;
        SysFile record = new SysFile();
        record.setFileName(storedName);
        record.setOriginalName(originalName);
        record.setFilePath(target.toString());
        record.setFileUrl(url);
        record.setFileSize(file.getSize());
        record.setFileType(ext);
        // storage 字段非空：明确记录存储介质，将来换 OSS 时按该字段区分
        record.setStorage("local");
        record.setBizType(StrUtil.blankToDefault(bizType, "common"));
        record.setCreateBy(SecurityUtils.getUserId());
        fileMapper.insert(record);

        FileUploadVO vo = new FileUploadVO();
        vo.setFileName(storedName);
        vo.setUrl(url);
        vo.setSize(file.getSize());
        return vo;
    }

    @Override
    public List<FileUploadVO> uploadBatch(MultipartFile[] files, String bizType) {
        if (files == null || files.length == 0) {
            throw new ServiceException("请选择要上传的文件");
        }
        List<FileUploadVO> result = new ArrayList<>(files.length);
        // 逐个上传：单个文件失败会中断并回滚不了已落盘的文件，但磁盘文件可由清理任务处理，
        // 这里不做额外的事务/补偿，保持实现简单可讲
        for (MultipartFile file : files) {
            result.add(upload(file, bizType));
        }
        return result;
    }

    @Override
    public PageResult<SysFile> page(PageQuery pageQuery, String originalName, String fileType, String bizType) {
        LambdaQueryWrapper<SysFile> wrapper = Wrappers.<SysFile>lambdaQuery()
                .like(StrUtil.isNotBlank(originalName), SysFile::getOriginalName, originalName)
                .eq(StrUtil.isNotBlank(fileType), SysFile::getFileType, fileType)
                .eq(StrUtil.isNotBlank(bizType), SysFile::getBizType, bizType)
                .orderByDesc(SysFile::getFileId);
        Page<SysFile> page = fileMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public void remove(String fileIds) {
        List<Long> ids = parseIds(fileIds);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的文件");
        for (Long fileId : ids) {
            SysFile record = fileMapper.selectById(fileId);
            if (record == null) {
                continue;
            }
            // 先删磁盘文件再删记录：文件不存在不影响记录清理（best-effort）
            if (StrUtil.isNotBlank(record.getFilePath())) {
                try {
                    Files.deleteIfExists(Paths.get(record.getFilePath()));
                } catch (IOException e) {
                    log.warn("删除磁盘文件失败: {}", record.getFilePath(), e);
                }
            }
            fileMapper.deleteById(fileId);
        }
    }

    /** 取后缀并统一小写；没有后缀时返回空串，交给白名单校验拦截 */
    private String resolveExt(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private List<Long> parseIds(String ids) {
        if (StrUtil.isBlank(ids)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                result.add(Long.valueOf(part.trim()));
            }
        }
        return result;
    }
}
