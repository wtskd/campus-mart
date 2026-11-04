package com.campus.ai.controller;

import com.campus.ai.config.AiProperties;
import com.campus.ai.domain.dto.KnowledgeHitVO;
import com.campus.ai.domain.dto.KnowledgeSearchRequest;
import com.campus.ai.domain.dto.StatOverviewVO;
import com.campus.ai.domain.entity.AiCallLog;
import com.campus.ai.domain.entity.AiKnowledge;
import com.campus.ai.domain.entity.AiPromptTemplate;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.ai.service.AiCallLogService;
import com.campus.ai.service.KnowledgeService;
import com.campus.ai.service.PromptTemplateService;
import com.campus.ai.service.StatService;
import com.campus.ai.support.AiScene;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 管理端：调用日志、提示词模板、知识库、场景、使用概览
 *
 * @author campus-mart
 */
@Tag(name = "AI 管理端", description = "调用日志 / 提示词 / 知识库 / 统计")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAdminController {

    private final AiCallLogService callLogService;

    private final PromptTemplateService promptTemplateService;

    private final KnowledgeService knowledgeService;

    private final StatService statService;

    private final AiProviderRouter router;

    private final AiProperties aiProperties;

    /* ==================== 调用日志 ==================== */

    @Operation(summary = "AI 调用日志分页")
    @RequiresPerm({"ai:log:list"})
    @GetMapping("/log/page")
    public Result<PageResult<AiCallLog>> logPage(
            PageQuery query,
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.ok(callLogService.page(query, scene, provider, status, beginTime, endTime));
    }

    /* ==================== 提示词模板 ==================== */

    @Operation(summary = "提示词模板分页")
    @RequiresPerm({"ai:prompt:list"})
    @GetMapping("/prompt/page")
    public Result<PageResult<AiPromptTemplate>> promptPage(
            PageQuery query,
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(promptTemplateService.page(query, scene, code, keyword));
    }

    @Operation(summary = "新增提示词模板")
    @RequiresPerm({"ai:prompt:list"})
    @OperLog(title = "AI提示词模板", businessType = 2)
    @PostMapping("/prompt")
    public Result<AiPromptTemplate> createPrompt(@RequestBody AiPromptTemplate template) {
        return Result.ok(promptTemplateService.create(template));
    }

    @Operation(summary = "修改提示词模板")
    @RequiresPerm({"ai:prompt:list"})
    @OperLog(title = "AI提示词模板", businessType = 3)
    @PutMapping("/prompt")
    public Result<AiPromptTemplate> updatePrompt(@RequestBody AiPromptTemplate template) {
        return Result.ok(promptTemplateService.update(template));
    }

    @Operation(summary = "删除提示词模板")
    @RequiresPerm({"ai:prompt:list"})
    @OperLog(title = "AI提示词模板", businessType = 3)
    @DeleteMapping("/prompt/{id}")
    public Result<Void> deletePrompt(@PathVariable("id") Long id) {
        promptTemplateService.delete(id);
        return Result.ok();
    }

    /* ==================== 知识库 ==================== */

    @Operation(summary = "知识库分页")
    @RequiresPerm({"ai:knowledge:list"})
    @GetMapping("/knowledge/page")
    public Result<PageResult<AiKnowledge>> knowledgePage(
            PageQuery query,
            @RequestParam(value = "kbType", required = false) String kbType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return Result.ok(knowledgeService.page(query, kbType, keyword, status));
    }

    @Operation(summary = "新增知识条目")
    @RequiresPerm({"ai:knowledge:list"})
    @OperLog(title = "AI知识库", businessType = 2)
    @PostMapping("/knowledge")
    public Result<AiKnowledge> createKnowledge(@RequestBody AiKnowledge knowledge) {
        return Result.ok(knowledgeService.create(knowledge));
    }

    @Operation(summary = "修改知识条目")
    @RequiresPerm({"ai:knowledge:list"})
    @OperLog(title = "AI知识库", businessType = 3)
    @PutMapping("/knowledge")
    public Result<AiKnowledge> updateKnowledge(@RequestBody AiKnowledge knowledge) {
        return Result.ok(knowledgeService.update(knowledge));
    }

    @Operation(summary = "删除知识条目")
    @RequiresPerm({"ai:knowledge:list"})
    @OperLog(title = "AI知识库", businessType = 3)
    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledge(@PathVariable("id") Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "知识库召回测试")
    @RequiresPerm({"ai:knowledge:list"})
    @PostMapping("/knowledge/search")
    public Result<List<KnowledgeHitVO>> searchKnowledge(@Valid @RequestBody KnowledgeSearchRequest request) {
        return Result.ok(knowledgeService.search(request.getQuestion(), request.getLimit()));
    }

    /* ==================== 场景 / 统计 ==================== */

    @Operation(summary = "AI 场景列表")
    @GetMapping("/scene/list")
    public Result<List<Map<String, Object>>> sceneList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AiScene scene : AiScene.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("scene", scene.getScene());
            row.put("name", scene.getDisplayName());
            row.put("description", scene.getDescription());
            row.put("templateCode", scene.getTemplateCode());
            row.put("implemented", scene.isImplemented());
            row.put("aliases", scene.getAliases());
            list.add(row);
        }
        return Result.ok(list);
    }

    @Operation(summary = "AI 使用概览")
    @RequiresPerm({"ai:log:list"})
    @GetMapping("/stat/overview")
    public Result<StatOverviewVO> overview(
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        return Result.ok(statService.overview(days));
    }

    /* ==================== 运行时配置 / 连通性测试 ==================== */

    @Operation(summary = "当前 AI 配置", description = "返回生效的 provider、远程/本地模型与超时（密钥打码）")
    @RequiresPerm({"ai:log:list", "ai:config:view"})
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", router.primaryProvider());
        if (aiProperties.getRemote() != null) {
            Map<String, Object> remote = new LinkedHashMap<>();
            remote.put("baseUrl", aiProperties.getRemote().getBaseUrl());
            remote.put("model", aiProperties.getRemote().getModel());
            remote.put("hasApiKey", aiProperties.getRemote().getApiKey() != null
                    && !aiProperties.getRemote().getApiKey().isBlank());
            data.put("remote", remote);
        }
        if (aiProperties.getLocal() != null) {
            Map<String, Object> local = new LinkedHashMap<>();
            local.put("baseUrl", aiProperties.getLocal().getBaseUrl());
            local.put("model", aiProperties.getLocal().getModel());
            data.put("local", local);
        }
        data.put("timeoutSeconds", aiProperties.getTimeout());
        List<String> names = new ArrayList<>();
        for (com.campus.ai.provider.AiProvider p : router.allProviders()) {
            names.add(p.name());
        }
        data.put("providers", names);
        return Result.ok(data);
    }

    @Operation(summary = "AI 连通性测试", description = "对指定 provider 发一次极小的请求，返回是否成功与耗时")
    @RequiresPerm({"ai:log:list", "ai:config:test"})
    @PostMapping("/config/test")
    public Result<Map<String, Object>> configTest(@RequestBody(required = false) Map<String, Object> body) {
        String want = body == null || body.get("provider") == null
                ? router.primaryProvider()
                : String.valueOf(body.get("provider"));

        com.campus.ai.provider.AiProvider target = null;
        for (com.campus.ai.provider.AiProvider p : router.allProviders()) {
            if (p.name().equalsIgnoreCase(want)) {
                target = p;
                break;
            }
        }
        if (target == null) {
            return Result.fail(400, "未知的 provider：" + want + "（可选 remote / local / mock）");
        }

        long begin = System.currentTimeMillis();
        boolean available;
        try {
            available = target.available();
        } catch (Exception e) {
            available = false;
        }
        if (!available) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ok", false);
            data.put("provider", target.name());
            data.put("costMs", System.currentTimeMillis() - begin);
            data.put("message", "该 provider 未就绪（未配置密钥或本地模型未启动）");
            return Result.ok(data);
        }

        com.campus.ai.provider.AiResponse resp =
                target.chat("你是连通性测试助手，只回复两个字：正常", "ping", 0.1, 16);
        long cost = System.currentTimeMillis() - begin;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", resp != null && resp.isSuccess());
        data.put("provider", target.name());
        data.put("model", resp == null ? "" : resp.getModel());
        data.put("costMs", cost);
        data.put("message", resp != null && resp.isSuccess()
                ? "连接正常"
                : "调用失败：" + (resp == null ? "无响应" : resp.getError()));
        return Result.ok(data);
    }
}
