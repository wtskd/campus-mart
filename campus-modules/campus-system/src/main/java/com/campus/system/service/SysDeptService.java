package com.campus.system.service;

import com.campus.system.domain.dto.TreeSelectVO;
import com.campus.system.domain.entity.SysDept;

import java.util.List;

/**
 * 学院（部门）管理服务。
 *
 * @author campus-mart
 */
public interface SysDeptService {

    /** 扁平列表（含 ancestors，前端可自行建树） */
    List<SysDept> list(String deptName, String status);

    SysDept detail(Long deptId);

    void add(SysDept dept);

    void update(SysDept dept);

    void remove(Long deptId);

    /** 下拉树（部门管理下拉、用户归属学院下拉共用） */
    List<TreeSelectVO> treeSelect();
}
