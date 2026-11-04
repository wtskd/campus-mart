package com.campus.api.feign;

import com.campus.api.dto.ModerationDTO;
import com.campus.api.dto.ModerationRequest;
import com.campus.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调用 campus-ai 服务（商品发布/评论时做内容审核）
 *
 * @author campus-mart
 */
@FeignClient(name = "campus-ai", contextId = "aiClient", path = "/ai/inner")
public interface AiClient {

    /** 内容合规审核（敏感词 + 可选大模型复核） */
    @PostMapping("/moderate")
    Result<ModerationDTO> moderate(@RequestBody ModerationRequest req);
}
