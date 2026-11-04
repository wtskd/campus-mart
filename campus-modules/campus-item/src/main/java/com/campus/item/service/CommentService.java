package com.campus.item.service;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.item.dto.CommentAuditDTO;
import com.campus.item.dto.CommentCreateDTO;
import com.campus.item.dto.CommentCreateVO;
import com.campus.item.dto.CommentQueryDTO;
import com.campus.item.dto.CommentVO;

/**
 * 商品评价服务
 *
 * @author campus-mart
 */
public interface CommentService {

    /**
     * 商品评价分页（仅展示 status='0' 的正常评价）
     */
    PageResult<CommentVO> pageByItem(Long itemId, PageQuery query);

    /**
     * 发表评价（先成交后评价：订单必填，且必须是已完成订单）
     */
    CommentCreateVO create(CommentCreateDTO dto);

    /**
     * 后台评价分页
     */
    PageResult<CommentVO> pageAdmin(CommentQueryDTO query);

    /**
     * 后台审核评价（0 通过 / 1 隐藏）
     */
    void audit(CommentAuditDTO dto);

    /**
     * 后台删除评价
     */
    void delete(Long commentId);
}
