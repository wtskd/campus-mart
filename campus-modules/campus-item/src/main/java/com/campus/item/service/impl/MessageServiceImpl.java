package com.campus.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.item.domain.entity.BizMessage;
import com.campus.item.dto.MessageQueryDTO;
import com.campus.item.dto.MessageVO;
import com.campus.item.mapper.BizMessageMapper;
import com.campus.item.service.MessageService;
import com.campus.item.service.MsgType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息服务实现：只承载系统通知 / 业务通知（订单、举报、AI 审核），不提供用户私信。
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /** 未读 */
    private static final String UNREAD = "0";

    /** 已读 */
    private static final String READ = "1";

    private final BizMessageMapper messageMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<MessageVO> myPage(MessageQueryDTO query) {
        Long userId = SecurityUtils.requireUserId();
        LambdaQueryWrapper<BizMessage> wrapper = new LambdaQueryWrapper<BizMessage>()
                .eq(BizMessage::getUserId, userId);
        if (query.getMsgType() != null) {
            wrapper.eq(BizMessage::getMsgType, query.getMsgType());
        }
        if (StringUtils.hasText(query.getIsRead())) {
            wrapper.eq(BizMessage::getIsRead, query.getIsRead().trim());
        }
        wrapper.orderByDesc(BizMessage::getCreateTime).orderByDesc(BizMessage::getMessageId);
        Page<BizMessage> page = messageMapper.selectPage(query.toPage(), wrapper);
        List<MessageVO> list = new ArrayList<>();
        for (BizMessage message : page.getRecords()) {
            list.add(toVO(message));
        }
        return pageResult(page, list);
    }

    @Override
    public long unreadCount() {
        Long userId = SecurityUtils.requireUserId();
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<BizMessage>()
                .eq(BizMessage::getUserId, userId)
                .eq(BizMessage::getIsRead, UNREAD));
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void read(Long messageId) {
        Long userId = SecurityUtils.requireUserId();
        if (messageId == null) {
            throw new ServiceException(400, "消息ID不能为空");
        }
        BizMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ServiceException(404, "消息不存在");
        }
        if (!userId.equals(message.getUserId())) {
            throw new ServiceException(403, "无权操作该消息");
        }
        if (READ.equals(message.getIsRead())) {
            return;
        }
        messageMapper.update(null, new LambdaUpdateWrapper<BizMessage>()
                .eq(BizMessage::getMessageId, messageId)
                .eq(BizMessage::getUserId, userId)
                .set(BizMessage::getIsRead, READ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll() {
        Long userId = SecurityUtils.requireUserId();
        messageMapper.update(null, new LambdaUpdateWrapper<BizMessage>()
                .eq(BizMessage::getUserId, userId)
                .eq(BizMessage::getIsRead, UNREAD)
                .set(BizMessage::getIsRead, READ));
    }

    @Override
    public PageResult<MessageVO> adminPage(MessageQueryDTO query) {
        LambdaQueryWrapper<BizMessage> wrapper = new LambdaQueryWrapper<>();
        if (query.getUserId() != null) {
            wrapper.eq(BizMessage::getUserId, query.getUserId());
        }
        if (query.getMsgType() != null) {
            wrapper.eq(BizMessage::getMsgType, query.getMsgType());
        }
        if (StringUtils.hasText(query.getIsRead())) {
            wrapper.eq(BizMessage::getIsRead, query.getIsRead().trim());
        }
        if (query.getBeginTime() != null) {
            wrapper.ge(BizMessage::getCreateTime, query.getBeginTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(BizMessage::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(BizMessage::getCreateTime).orderByDesc(BizMessage::getMessageId);
        Page<BizMessage> page = messageMapper.selectPage(query.toPage(), wrapper);
        List<MessageVO> list = new ArrayList<>();
        for (BizMessage message : page.getRecords()) {
            list.add(toVO(message));
        }
        // 后台列表补接收人昵称（批量，避免 N+1）
        Map<Long, String> nicknames = loadNicknames(list.stream()
                .map(MessageVO::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        for (MessageVO vo : list) {
            vo.setReceiverNickname(nicknames.get(vo.getUserId()));
        }
        return pageResult(page, list);
    }

    // ------------------------------------------------------------------ 内部方法

    private PageResult<MessageVO> pageResult(Page<BizMessage> page, List<MessageVO> records) {
        PageResult<MessageVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    private MessageVO toVO(BizMessage message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        vo.setMsgTypeText(msgTypeText(message.getMsgType()));
        return vo;
    }

    private String msgTypeText(Integer msgType) {
        return MsgType.textOf(msgType);
    }

    private Map<Long, String> loadNicknames(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        Set<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT user_id, COALESCE(NULLIF(nickname, ''), username) AS nickname "
                + "FROM sys_user WHERE user_id IN (" + placeholders + ")";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ids.toArray());
        Map<Long, String> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("user_id");
            Object nickname = row.get("nickname");
            if (id != null) {
                map.put(((Number) id).longValue(), nickname == null ? null : nickname.toString());
            }
        }
        return map;
    }
}
