package com.campus.item.mapper;

import com.campus.item.dto.UserBriefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * sys_user 表自定义查询（本模块不持有 sys_user 实体）
 *
 * @author campus-mart
 */
@Mapper
public interface SysUserCustomMapper {

    /**
     * 按用户ID查询用户概要（只查未删除用户）
     */
    @Select("SELECT user_id, nickname, avatar, credit_score FROM sys_user"
            + " WHERE user_id = #{userId} AND del_flag = '0'")
    UserBriefVO selectBriefById(@Param("userId") Long userId);

    /**
     * 按用户ID批量查询用户概要（调用方需保证 userIds 非空）
     */
    @Select("<script>"
            + "SELECT user_id, nickname, avatar, credit_score FROM sys_user"
            + " WHERE del_flag = '0' AND user_id IN"
            + "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<UserBriefVO> selectBriefByIds(@Param("userIds") Collection<Long> userIds);
}
