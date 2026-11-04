package com.campus.item.service;

import com.campus.common.core.domain.PageResult;
import com.campus.item.domain.entity.BizNotice;
import com.campus.item.dto.NoticeQueryDTO;
import com.campus.item.dto.NoticeSaveDTO;

import java.util.List;

/**
 * 公告服务
 *
 * @author campus-mart
 */
public interface NoticeService {

    /** 前台公告列表：仅已发布且已到发布时间的，按发布时间倒序，最多 20 条 */
    List<BizNotice> appList();

    /** 前台公告详情（草稿与未到发布时间的不返回） */
    BizNotice appDetail(Long noticeId);

    /** 后台公告详情（不过滤状态，草稿也能回显编辑） */
    BizNotice adminDetail(Long noticeId);

    /** 后台公告分页 */
    PageResult<BizNotice> adminPage(NoticeQueryDTO query);

    /** 后台新增公告 */
    Long create(NoticeSaveDTO dto);

    /** 后台修改公告 */
    void update(NoticeSaveDTO dto);

    /** 后台删除公告 */
    void delete(Long noticeId);
}
