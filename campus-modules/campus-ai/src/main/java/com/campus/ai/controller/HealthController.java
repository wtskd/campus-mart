package com.campus.ai.controller;

import com.campus.ai.config.AiProperties;
import com.campus.ai.domain.dto.HealthVO;
import com.campus.ai.domain.dto.ProviderStatusVO;
import com.campus.ai.provider.AiProvider;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Provider 健康状态
 *
 * @author campus-mart
 */
@Tag(name = "AI 健康检查", description = "各 provider 可用状态")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class HealthController {

    private final AiProviderRouter router;

    private final AiProperties aiProperties;

    @Operation(summary = "AI 服务健康状态（remote/local/mock）")
    @GetMapping("/health")
    public Result<HealthVO> health() {
        HealthVO vo = new HealthVO();
        vo.setStatus("UP");
        vo.setService("campus-ai");
        vo.setConfiguredProvider(router.primaryProvider());
        List<ProviderStatusVO> providers = new ArrayList<>();
        for (AiProvider provider : router.allProviders()) {
            boolean available;
            try {
                available = provider.available();
            } catch (Exception e) {
                available = false;
            }
            providers.add(new ProviderStatusVO(provider.name(), available, modelOf(provider.name())));
        }
        vo.setProviders(providers);
        return Result.ok(vo);
    }

    private String modelOf(String provider) {
        if ("remote".equals(provider)) {
            return aiProperties.getRemote() == null ? "" : aiProperties.getRemote().getModel();
        }
        if ("local".equals(provider)) {
            return aiProperties.getLocal() == null ? "" : aiProperties.getLocal().getModel();
        }
        return "local-template";
    }
}
