package com.campus.item.service;

import com.campus.common.core.domain.PageResult;
import com.campus.item.dto.ReportCreateDTO;
import com.campus.item.dto.ReportHandleDTO;
import com.campus.item.dto.ReportQueryDTO;
import com.campus.item.dto.ReportVO;

/**
 * 举报服务
 *
 * @author campus-mart
 */
public interface ReportService {

    /** 前台提交举报（同用户同商品 24 小时内限一次） */
    Long create(ReportCreateDTO dto);

    /** 后台举报分页 */
    PageResult<ReportVO> adminPage(ReportQueryDTO query);

    /** 后台处理举报：通过时若为商品举报则商品置违规 */
    void handle(ReportHandleDTO dto);
}
