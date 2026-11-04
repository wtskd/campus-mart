package com.campus.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.item.domain.entity.BizBanner;
import com.campus.item.dto.BannerQueryDTO;
import com.campus.item.dto.BannerSaveDTO;
import com.campus.item.mapper.BizBannerMapper;
import com.campus.item.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播服务实现
 *
 * <p>biz_banner 排序列为 sort（升序），前台只返回上架且在生效时间内的数据。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    /** 上架 */
    private static final String STATUS_ON = "0";

    private final BizBannerMapper bannerMapper;

    @Override
    public List<BizBanner> appList() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<BizBanner> wrapper = new LambdaQueryWrapper<BizBanner>()
                .eq(BizBanner::getStatus, STATUS_ON)
                .and(w -> w.isNull(BizBanner::getStartTime).or().le(BizBanner::getStartTime, now))
                .and(w -> w.isNull(BizBanner::getEndTime).or().ge(BizBanner::getEndTime, now))
                .orderByAsc(BizBanner::getSort)
                .orderByDesc(BizBanner::getBannerId);
        return bannerMapper.selectList(wrapper);
    }

    @Override
    public List<BizBanner> adminList() {
        LambdaQueryWrapper<BizBanner> wrapper = new LambdaQueryWrapper<BizBanner>()
                .orderByAsc(BizBanner::getSort)
                .orderByDesc(BizBanner::getBannerId);
        return bannerMapper.selectList(wrapper);
    }

    @Override
    public PageResult<BizBanner> adminPage(BannerQueryDTO query) {
        LambdaQueryWrapper<BizBanner> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(BizBanner::getTitle, query.getTitle().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(BizBanner::getStatus, query.getStatus().trim());
        }
        wrapper.orderByAsc(BizBanner::getSort).orderByDesc(BizBanner::getBannerId);
        Page<BizBanner> page = bannerMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BannerSaveDTO dto) {
        checkTimeRange(dto.getStartTime(), dto.getEndTime());
        BizBanner banner = new BizBanner();
        banner.setTitle(dto.getTitle().trim());
        banner.setImageUrl(dto.getImageUrl().trim());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setSort(dto.getSort() == null ? 0 : dto.getSort());
        banner.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : STATUS_ON);
        banner.setStartTime(dto.getStartTime());
        banner.setEndTime(dto.getEndTime());
        bannerMapper.insert(banner);
        return banner.getBannerId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BannerSaveDTO dto) {
        if (dto.getBannerId() == null) {
            throw new ServiceException(400, "轮播ID不能为空");
        }
        BizBanner exist = bannerMapper.selectById(dto.getBannerId());
        if (exist == null) {
            throw new ServiceException(404, "轮播不存在");
        }
        checkTimeRange(dto.getStartTime(), dto.getEndTime());
        BizBanner banner = new BizBanner();
        banner.setBannerId(dto.getBannerId());
        banner.setTitle(dto.getTitle().trim());
        banner.setImageUrl(dto.getImageUrl().trim());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setSort(dto.getSort());
        if (StringUtils.hasText(dto.getStatus())) {
            banner.setStatus(dto.getStatus().trim());
        }
        banner.setStartTime(dto.getStartTime());
        banner.setEndTime(dto.getEndTime());
        bannerMapper.updateById(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long bannerId) {
        if (bannerId == null) {
            throw new ServiceException(400, "轮播ID不能为空");
        }
        BizBanner exist = bannerMapper.selectById(bannerId);
        if (exist == null) {
            throw new ServiceException(404, "轮播不存在");
        }
        bannerMapper.deleteById(bannerId);
    }

    private void checkTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new ServiceException(400, "生效结束时间必须晚于开始时间");
        }
    }
}
