package com.campus.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户-角色关系行（批量回填用户列表的 roleIds 用）。
 *
 * @author campus-mart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRef implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;
}
