package com.campus.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户输出对象。
 *
 * <p>为什么不直接返回实体：实体带 password，一旦序列化出去就是密码哈希泄露；
 * 这里只暴露契约要求的字段，并补上 deptName/roleIds 便于前端直接渲染。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "用户信息")
public class SysUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long deptId;

    @Schema(description = "学院名称，列表展示用")
    private String deptName;

    private String username;

    private String nickname;

    private String userType;

    private String phone;

    private String email;

    private String avatar;

    private String gender;

    private String studentNo;

    private Integer creditScore;

    @Schema(description = "状态 0正常 1停用")
    private String status;

    private String loginIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginDate;

    private String remark;

    @Schema(description = "角色ID集合")
    private List<Long> roleIds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
