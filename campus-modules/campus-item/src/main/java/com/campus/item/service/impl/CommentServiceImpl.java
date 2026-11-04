package com.campus.item.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.api.dto.ModerationDTO;
import com.campus.api.dto.ModerationRequest;
import com.campus.api.feign.AiClient;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.item.domain.entity.BizComment;
import com.campus.item.domain.entity.BizOrder;
import com.campus.item.dto.CommentAuditDTO;
import com.campus.item.dto.CommentCreateDTO;
import com.campus.item.dto.CommentCreateVO;
import com.campus.item.dto.CommentQueryDTO;
import com.campus.item.dto.CommentVO;
import com.campus.item.dto.UserBriefVO;
import com.campus.item.mapper.BizCommentMapper;
import com.campus.item.mapper.BizOrderMapper;
import com.campus.item.service.CommentService;
import com.campus.item.service.SysUserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品评价服务实现
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /** 评价状态：正常（前台可见） */
    private static final String STATUS_NORMAL = "0";

    /** 评价状态：审核未通过 */
    private static final String STATUS_REJECTED = "2";

    /** 订单状态：已完成 */
    private static final int ORDER_STATUS_FINISHED = 2;

    private final BizCommentMapper commentMapper;

    private final BizOrderMapper orderMapper;

    private final SysUserLookupService sysUserLookupService;

    private final AiClient aiClient;

    @Override
    public PageResult<CommentVO> pageByItem(Long itemId, PageQuery query) {
        LambdaQueryWrapper<BizComment> wrapper = Wrappers.lambdaQuery(BizComment.class)
                .eq(BizComment::getItemId, itemId)
                .eq(BizComment::getStatus, STATUS_NORMAL)
                .orderByDesc(BizComment::getCreateTime)
                .orderByDesc(BizComment::getCommentId);
        Page<BizComment> page = commentMapper.selectPage(query.toPage(), wrapper);
        Map<Long, UserBriefVO> users = userMap(page.getRecords());
        return PageResult.of(page, comment -> toVO(comment, users));
    }

    @Override
    public CommentCreateVO create(CommentCreateDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        BizOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (!userId.equals(order.getBuyerId())) {
            throw new ServiceException(403, "只能评价自己购买的商品");
        }
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_FINISHED) {
            throw new ServiceException("订单未完成，暂不能评价");
        }
        if (!order.getItemId().equals(dto.getItemId())) {
            throw new ServiceException("订单与商品不匹配");
        }
        Long exist = commentMapper.selectCount(Wrappers.lambdaQuery(BizComment.class)
                .eq(BizComment::getOrderId, dto.getOrderId()));
        if (exist != null && exist > 0) {
            throw new ServiceException("该订单已评价，请勿重复评价");
        }

        // AI 内容审核：AI 服务不可用时兜底放行
        ModerationDTO moderation = moderate(dto.getContent(), order.getItemId());
        boolean pass = moderation == null || moderation.isPass();

        BizComment comment = new BizComment();
        comment.setOrderId(dto.getOrderId());
        comment.setItemId(order.getItemId());
        comment.setFromUserId(userId);
        comment.setToUserId(order.getSellerId());
        comment.setScore(dto.getRating());
        comment.setContent(dto.getContent().trim());
        comment.setImages(toJsonString(dto.getImages()));
        comment.setStatus(pass ? STATUS_NORMAL : STATUS_REJECTED);
        try {
            commentMapper.insert(comment);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("该订单已评价，请勿重复评价");
        }

        // 审核未通过：落库为 status='2'（后台可见、前台不展示），随后直接拒绝并提示
        if (!pass) {
            throw new ServiceException("评价内容未通过审核：" + buildModerationRemark(moderation));
        }

        CommentCreateVO vo = new CommentCreateVO();
        vo.setCommentId(comment.getCommentId());
        vo.setItemId(comment.getItemId());
        vo.setOrderId(comment.getOrderId());
        vo.setStatus(comment.getStatus());
        vo.setMessage("评价成功");
        return vo;
    }

    @Override
    public PageResult<CommentVO> pageAdmin(CommentQueryDTO query) {
        LambdaQueryWrapper<BizComment> wrapper = Wrappers.lambdaQuery(BizComment.class)
                .eq(query.getItemId() != null, BizComment::getItemId, query.getItemId())
                .eq(query.getFromUserId() != null, BizComment::getFromUserId, query.getFromUserId())
                .eq(StringUtils.hasText(query.getStatus()), BizComment::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), BizComment::getContent,
                        query.getKeyword() == null ? null : query.getKeyword().trim())
                .orderByDesc(BizComment::getCreateTime)
                .orderByDesc(BizComment::getCommentId);
        Page<BizComment> page = commentMapper.selectPage(query.toPage(), wrapper);
        Map<Long, UserBriefVO> users = userMap(page.getRecords());
        return PageResult.of(page, comment -> toVO(comment, users));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(CommentAuditDTO dto) {
        BizComment exist = commentMapper.selectById(dto.getCommentId());
        if (exist == null) {
            throw new ServiceException("评价不存在");
        }
        BizComment update = new BizComment();
        update.setCommentId(dto.getCommentId());
        update.setStatus(dto.getStatus());
        commentMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long commentId) {
        BizComment exist = commentMapper.selectById(commentId);
        if (exist == null) {
            throw new ServiceException("评价不存在");
        }
        commentMapper.deleteById(commentId);
    }

    // ============================ 私有工具 ============================

    private Map<Long, UserBriefVO> userMap(List<BizComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = new HashSet<>();
        for (BizComment comment : comments) {
            if (comment.getFromUserId() != null) {
                userIds.add(comment.getFromUserId());
            }
            if (comment.getToUserId() != null) {
                userIds.add(comment.getToUserId());
            }
        }
        return sysUserLookupService.getUserMap(userIds);
    }

    private CommentVO toVO(BizComment comment, Map<Long, UserBriefVO> users) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(comment.getCommentId());
        vo.setOrderId(comment.getOrderId());
        vo.setItemId(comment.getItemId());
        vo.setFromUserId(comment.getFromUserId());
        vo.setToUserId(comment.getToUserId());
        UserBriefVO from = users.get(comment.getFromUserId());
        if (from != null) {
            vo.setFromNickname(from.getNickname());
            vo.setFromAvatar(from.getAvatar());
        }
        UserBriefVO to = users.get(comment.getToUserId());
        if (to != null) {
            vo.setToNickname(to.getNickname());
        }
        vo.setScore(comment.getScore());
        vo.setContent(comment.getContent());
        vo.setImages(parseJsonList(comment.getImages()));
        vo.setReplyContent(comment.getReplyContent());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    /**
     * 评论内容 AI 审核：返回 null 表示 AI 服务不可用（调用方按放行处理）
     */
    private ModerationDTO moderate(String content, Long itemId) {
        try {
            ModerationRequest request = new ModerationRequest();
            request.setItemId(itemId);
            request.setTitle("商品评价");
            request.setDescription(content);
            request.setScene("comment");
            Result<ModerationDTO> result = aiClient.moderate(request);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            log.warn("评价内容审核返回异常结果，按放行处理：{}", result == null ? "null" : result.getMsg());
            return null;
        } catch (Exception e) {
            log.warn("评价内容审核调用失败，按放行处理：{}", e.getMessage());
            return null;
        }
    }

    private String buildModerationRemark(ModerationDTO moderation) {
        if (moderation == null) {
            return "内容不符合平台规范";
        }
        if (moderation.getWords() != null && !moderation.getWords().isEmpty()) {
            return "命中敏感词：" + String.join("、", moderation.getWords());
        }
        return StringUtils.hasText(moderation.getReason()) ? moderation.getReason() : "内容不符合平台规范";
    }

    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(list);
    }

    private List<String> parseJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JSONUtil.toList(json, String.class);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
