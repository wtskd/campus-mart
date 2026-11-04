package com.campus.item.service;

import com.campus.common.core.domain.PageResult;
import com.campus.item.dto.MessageQueryDTO;
import com.campus.item.dto.MessageVO;

/**
 * 消息服务：系统通知 / 业务通知（订单、举报、AI 审核），不提供用户私信
 *
 * @author campus-mart
 */
public interface MessageService {

    /** 我的消息分页 */
    PageResult<MessageVO> myPage(MessageQueryDTO query);

    /** 我的未读消息数 */
    long unreadCount();

    /** 标记单条消息已读 */
    void read(Long messageId);

    /** 标记全部消息已读 */
    void readAll();

    /** 后台消息分页 */
    PageResult<MessageVO> adminPage(MessageQueryDTO query);
}
