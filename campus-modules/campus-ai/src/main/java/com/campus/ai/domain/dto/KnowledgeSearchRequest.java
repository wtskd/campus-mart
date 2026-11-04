package com.campus.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库召回测试请求
 *
 * @author campus-mart
 */
@Data
public class KnowledgeSearchRequest {

    @NotBlank(message = "检索关键词不能为空")
    @Size(max = 200, message = "检索关键词过长")
    private String question;

    /** 返回条数，默认 5 */
    private Integer limit;
}
