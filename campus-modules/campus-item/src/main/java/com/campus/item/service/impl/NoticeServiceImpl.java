package com.campus.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.item.domain.entity.BizNotice;
import com.campus.item.dto.NoticeQueryDTO;
import com.campus.item.dto.NoticeSaveDTO;
import com.campus.item.mapper.BizNoticeMapper;
import com.campus.item.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告服务实现
 *
 * <p>biz_notice 无置顶、无浏览量字段，前台列表按发布时间倒序（发布中且已到发布时间）。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    /** 公告状态：0 发布 */
    private static final String STATUS_PUBLISHED = "0";

    /** 公告状态：1 草稿 */
    private static final String STATUS_DRAFT = "1";

    /** 前台公告列表最大条数 */
    private static final int APP_LIST_LIMIT = 20;

    private final BizNoticeMapper noticeMapper;

    @Override
    public List<BizNotice> appList() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<BizNotice> wrapper = new LambdaQueryWrapper<BizNotice>()
                .eq(BizNotice::getStatus, STATUS_PUBLISHED)
                .and(w -> w.isNull(BizNotice::getPublishTime).or().le(BizNotice::getPublishTime, now))
                .orderByDesc(BizNotice::getPublishTime)
                .orderByDesc(BizNotice::getNoticeId);
        Page<BizNotice> page = noticeMapper.selectPage(new Page<>(1, APP_LIST_LIMIT), wrapper);
        return page.getRecords();
    }

    @Override
    public BizNotice appDetail(Long noticeId) {
        if (noticeId == null) {
            throw new ServiceException(400, "公告ID不能为空");
        }
        BizNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || !STATUS_PUBLISHED.equals(notice.getStatus())) {
            throw new ServiceException(404, "公告不存在或已下架");
        }
        if (notice.getPublishTime() != null && notice.getPublishTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException(404, "公告尚未发布");
        }
        return notice;
    }

    @Override
    public BizNotice adminDetail(Long noticeId) {
        if (noticeId == null) {
            throw new ServiceException(400, "公告ID不能为空");
        }
        BizNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new ServiceException(404, "公告不存在");
        }
        return notice;
    }

    @Override
    public PageResult<BizNotice> adminPage(NoticeQueryDTO query) {
        LambdaQueryWrapper<BizNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(BizNotice::getTitle, query.getTitle().trim());
        }
        if (query.getNoticeType() != null) {
            wrapper.eq(BizNotice::getNoticeType, query.getNoticeType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(BizNotice::getStatus, query.getStatus().trim());
        }
        if (query.getBeginTime() != null) {
            wrapper.ge(BizNotice::getPublishTime, query.getBeginTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(BizNotice::getPublishTime, query.getEndTime());
        }
        wrapper.orderByDesc(BizNotice::getPublishTime).orderByDesc(BizNotice::getNoticeId);
        Page<BizNotice> page = noticeMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(NoticeSaveDTO dto) {
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : STATUS_PUBLISHED;
        BizNotice notice = new BizNotice();
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent());
        notice.setNoticeType(dto.getNoticeType() == null ? 1 : dto.getNoticeType());
        notice.setStatus(status);
        LocalDateTime publishTime = dto.getPublishTime();
        if (STATUS_PUBLISHED.equals(status) && publishTime == null) {
            publishTime = LocalDateTime.now();
        }
        notice.setPublishTime(publishTime);
        notice.setCreateBy(SecurityUtils.getUserId());
        noticeMapper.insert(notice);
        return notice.getNoticeId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(NoticeSaveDTO dto) {
        if (dto.getNoticeId() == null) {
            throw new ServiceException(400, "公告ID不能为空");
        }
        BizNotice exist = noticeMapper.selectById(dto.getNoticeId());
        if (exist == null) {
            throw new ServiceException(404, "公告不存在");
        }
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : exist.getStatus();
        BizNotice notice = new BizNotice();
        notice.setNoticeId(dto.getNoticeId());
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent());
        notice.setNoticeType(dto.getNoticeType());
        notice.setStatus(status);
        LocalDateTime publishTime = dto.getPublishTime();
        if (publishTime == null && STATUS_PUBLISHED.equals(status) && exist.getPublishTime() == null) {
            publishTime = LocalDateTime.now();
        }
        if (publishTime != null) {
            notice.setPublishTime(publishTime);
        }
        noticeMapper.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long noticeId) {
        if (noticeId == null) {
            throw new ServiceException(400, "公告ID不能为空");
        }
        BizNotice exist = noticeMapper.selectById(noticeId);
        if (exist == null) {
            throw new ServiceException(404, "公告不存在");
        }
        noticeMapper.deleteById(noticeId);
    }
}
