package com.campus.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.common.core.exception.ServiceException;
import com.campus.system.domain.dto.TreeSelectVO;
import com.campus.system.domain.entity.SysDept;
import com.campus.system.domain.entity.SysUser;
import com.campus.system.mapper.SysDeptMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 学院（部门）管理实现。
 *
 * <p>ancestors 字段维护「0,100,101」形式的祖级路径，查询子树时可以直接
 * {@code ancestors LIKE '0,100%'}，避免递归查库。</p>
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final String ROOT_ANCESTORS = "0";
    private static final String STATUS_ENABLED = "0";

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<SysDept> list(String deptName, String status) {
        LambdaQueryWrapper<SysDept> wrapper = Wrappers.<SysDept>lambdaQuery()
                .like(StrUtil.isNotBlank(deptName), SysDept::getDeptName, deptName)
                .eq(StrUtil.isNotBlank(status), SysDept::getStatus, status)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum);
        return deptMapper.selectList(wrapper);
    }

    @Override
    public SysDept detail(Long deptId) {
        SysDept dept = deptMapper.selectById(deptId);
        ServiceException.throwIf(dept == null, "学院不存在");
        return dept;
    }

    @Override
    public void add(SysDept dept) {
        ServiceException.throwIf(StrUtil.isBlank(dept.getDeptName()), "学院名称不能为空");
        if (dept.getParentId() == null) {
            dept.setParentId(ROOT_PARENT_ID);
        }
        if (dept.getOrderNum() == null) {
            dept.setOrderNum(0);
        }
        dept.setStatus(StrUtil.blankToDefault(dept.getStatus(), STATUS_ENABLED));
        // 逻辑删除字段没有自动填充，插入时显式置 '0'
        dept.setDelFlag(STATUS_ENABLED);
        dept.setAncestors(buildAncestors(dept.getParentId()));
        deptMapper.insert(dept);
    }

    @Override
    public void update(SysDept dept) {
        ServiceException.throwIf(dept.getDeptId() == null, "学院ID不能为空");
        SysDept exist = deptMapper.selectById(dept.getDeptId());
        ServiceException.throwIf(exist == null, "学院不存在");
        ServiceException.throwIf(dept.getDeptId().equals(dept.getParentId()), "上级学院不能选择自己");
        if (dept.getParentId() != null) {
            // 不能把学院挂到自己的子孙下面，否则树会成环、ancestors 也会自引用
            String parentAncestors = buildAncestors(dept.getParentId());
            ServiceException.throwIf(Arrays.asList(parentAncestors.split(","))
                            .contains(String.valueOf(dept.getDeptId())),
                    "上级学院不能选择自己的下级");
            dept.setAncestors(parentAncestors);
        }
        deptMapper.updateById(dept);
    }

    @Override
    public void remove(Long deptId) {
        ServiceException.throwIf(deptMapper.selectById(deptId) == null, "学院不存在");
        long children = deptMapper.selectCount(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getParentId, deptId));
        ServiceException.throwIf(children > 0, "存在下级学院，不允许删除");
        long users = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getDeptId, deptId));
        ServiceException.throwIf(users > 0, "学院下存在用户，不允许删除");
        deptMapper.deleteById(deptId);
    }

    @Override
    public List<TreeSelectVO> treeSelect() {
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum));
        Map<Long, TreeSelectVO> nodeMap = new LinkedHashMap<>();
        for (SysDept dept : depts) {
            TreeSelectVO node = new TreeSelectVO();
            node.setId(dept.getDeptId());
            node.setLabel(dept.getDeptName());
            nodeMap.put(dept.getDeptId(), node);
        }
        List<TreeSelectVO> roots = new ArrayList<>();
        for (SysDept dept : depts) {
            TreeSelectVO node = nodeMap.get(dept.getDeptId());
            TreeSelectVO parent = nodeMap.get(dept.getParentId());
            if (parent == null || Objects.equals(parent.getId(), node.getId())) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /** 父级为 0 时祖先路径就是 '0'；否则继承父级 ancestors 并追加父级ID */
    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return ROOT_ANCESTORS;
        }
        SysDept parent = deptMapper.selectById(parentId);
        ServiceException.throwIf(parent == null, "上级学院不存在");
        String base = StrUtil.blankToDefault(parent.getAncestors(), ROOT_ANCESTORS);
        return base + "," + parent.getDeptId();
    }
}
