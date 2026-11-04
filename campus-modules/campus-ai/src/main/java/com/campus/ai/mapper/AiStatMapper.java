package com.campus.ai.mapper;

import com.campus.ai.domain.dto.AiStatRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 调用统计 Mapper
 *
 * <p>status 约定：'0' 成功、'2' 降级兜底（由 mock 兜住）、'1' 失败。</p>
 *
 * @author campus-mart
 */
@Mapper
public interface AiStatMapper {

    @Select("SELECT COUNT(*) AS total_calls, "
            + "COALESCE(SUM(CASE WHEN status = '0' THEN 1 ELSE 0 END), 0) AS success_calls, "
            + "COALESCE(SUM(CASE WHEN status = '2' THEN 1 ELSE 0 END), 0) AS fallback_calls, "
            + "COALESCE(SUM(CASE WHEN status = '1' THEN 1 ELSE 0 END), 0) AS fail_calls, "
            + "COALESCE(AVG(cost_ms), 0) AS avg_cost_ms, "
            + "COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens, "
            + "COALESCE(SUM(completion_tokens), 0) AS completion_tokens, "
            + "COALESCE(SUM(total_tokens), 0) AS total_tokens "
            + "FROM ai_call_log WHERE create_time >= #{begin}")
    AiStatRow.Total overview(@Param("begin") LocalDateTime begin);

    @Select("SELECT scene, COUNT(*) AS calls, "
            + "COALESCE(SUM(CASE WHEN status = '0' THEN 1 ELSE 0 END), 0) AS success_calls, "
            + "COALESCE(SUM(CASE WHEN status = '2' THEN 1 ELSE 0 END), 0) AS fallback_calls, "
            + "COALESCE(SUM(CASE WHEN status = '1' THEN 1 ELSE 0 END), 0) AS fail_calls, "
            + "COALESCE(AVG(cost_ms), 0) AS avg_cost_ms, "
            + "COALESCE(SUM(total_tokens), 0) AS total_tokens "
            + "FROM ai_call_log WHERE create_time >= #{begin} "
            + "GROUP BY scene ORDER BY calls DESC")
    List<AiStatRow.Scene> sceneStats(@Param("begin") LocalDateTime begin);

    @Select("SELECT provider, COUNT(*) AS calls FROM ai_call_log "
            + "WHERE create_time >= #{begin} GROUP BY provider ORDER BY calls DESC")
    List<AiStatRow.Provider> providerStats(@Param("begin") LocalDateTime begin);

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS stat_date, COUNT(*) AS calls "
            + "FROM ai_call_log WHERE create_time >= #{begin} GROUP BY stat_date ORDER BY stat_date ASC")
    List<AiStatRow.Day> dayStats(@Param("begin") LocalDateTime begin);
}
