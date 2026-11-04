package com.campus.item.service;

import com.campus.common.core.domain.PageResult;
import com.campus.item.domain.entity.BizBanner;
import com.campus.item.dto.BannerQueryDTO;
import com.campus.item.dto.BannerSaveDTO;

import java.util.List;

/**
 * 轮播服务
 *
 * @author campus-mart
 */
public interface BannerService {

    /** 前台轮播列表：仅上架且在生效时间内，按 sort 升序 */
    List<BizBanner> appList();

    /** 后台轮播列表（不分页，含下架/未生效，按 sort 升序） */
    List<BizBanner> adminList();

    /** 后台轮播分页 */
    PageResult<BizBanner> adminPage(BannerQueryDTO query);

    /** 后台新增轮播 */
    Long create(BannerSaveDTO dto);

    /** 后台修改轮播 */
    void update(BannerSaveDTO dto);

    /** 后台删除轮播 */
    void delete(Long bannerId);
}
