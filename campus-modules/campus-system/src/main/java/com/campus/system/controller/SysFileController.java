package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.dto.FileUploadVO;
import com.campus.system.domain.entity.SysFile;
import com.campus.system.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传与文件库管理。
 *
 * <p>上传接口只要求登录（网关已校验令牌）：学生发布商品、评价晒图都要用到，
 * 而 system:file:upload 只授给了后台角色，做权限码会误伤前台用户。
 * 列表 / 删除仍然走 system:file:list 和 system:file:remove。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-文件管理")
@RestController
@RequestMapping("/system/file")
@RequiredArgsConstructor
public class SysFileController {

    private final SysFileService fileService;

    @Operation(summary = "单文件上传")
    @PostMapping("/upload")
    @OperLog(title = "文件上传", businessType = 1)
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String bizType) {
        return Result.ok("上传成功", fileService.upload(file, bizType));
    }

    @Operation(summary = "多文件上传（前端兼容路径，字段名 files）")
    @PostMapping("/uploads")
    @OperLog(title = "文件上传", businessType = 1)
    public Result<List<FileUploadVO>> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                                  @RequestParam(required = false) String bizType) {
        return Result.ok("上传成功", fileService.uploadBatch(files, bizType));
    }

    @Operation(summary = "文件库分页列表")
    @GetMapping("/list")
    @RequiresPerm({"system:file:list"})
    public Result<PageResult<SysFile>> list(PageQuery pageQuery,
                                            @RequestParam(required = false) String originalName,
                                            @RequestParam(required = false) String fileType,
                                            @RequestParam(required = false) String bizType) {
        return Result.ok(fileService.page(pageQuery, originalName, fileType, bizType));
    }

    @Operation(summary = "删除文件记录（逗号分隔批量）")
    @DeleteMapping("/{fileIds}")
    @RequiresPerm({"system:file:remove", "system:file:list"})
    @OperLog(title = "文件管理", businessType = 3)
    public Result<Void> remove(@PathVariable String fileIds) {
        fileService.remove(fileIds);
        return Result.ok("删除成功", null);
    }
}
