package com.campus.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.domain.entity.AiPromptTemplate;
import com.campus.ai.mapper.AiPromptTemplateMapper;
import com.campus.ai.support.AiScene;
import com.campus.ai.support.PromptRenderer;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 提示词模板服务：按场景读取 ai_prompt_template + 变量渲染 + 管理端 CRUD
 *
 * <p>读取口径以库内 scene 为准（copywrite/chat/audit/price），并兼容历史别名
 * （item_copywriting / moderation / item_summary 等）。模板缺失、停用或查询异常时，
 * 自动使用代码内置默认模板，保证无网络/空库也能演示。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final AiPromptTemplateMapper templateMapper;

    /** 渲染后的提示词 */
    public record ResolvedPrompt(String templateCode, String systemPrompt, String userPrompt,
                                 double temperature, int maxTokens) {
    }

    /**
     * 按场景解析提示词（带内置兜底）
     */
    public ResolvedPrompt resolve(String sceneKey, Map<String, String> variables) {
        AiScene scene = AiScene.of(sceneKey);
        AiPromptTemplate template = findTemplate(scene);
        String system = template != null && StringUtils.hasText(template.getSystemPrompt())
                ? template.getSystemPrompt() : scene.defaultSystemPrompt();
        String user = template != null && StringUtils.hasText(template.getUserPrompt())
                ? template.getUserPrompt() : scene.defaultUserPrompt();
        double temperature = template != null && template.getTemperature() != null
                ? template.getTemperature().doubleValue() : scene.defaultTemperature();
        int maxTokens = template != null && template.getMaxTokens() != null && template.getMaxTokens() > 0
                ? template.getMaxTokens() : scene.defaultMaxTokens();
        String templateCode = template != null && StringUtils.hasText(template.getCode())
                ? template.getCode() : scene.getTemplateCode();
        return new ResolvedPrompt(templateCode,
                PromptRenderer.render(system, variables),
                PromptRenderer.render(user, variables),
                temperature, maxTokens);
    }

    /** 查询库内启用模板（scene 别名全量匹配，id 小的优先） */
    public AiPromptTemplate findTemplate(AiScene scene) {
        try {
            LambdaQueryWrapper<AiPromptTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiPromptTemplate::getStatus, "0")
                    .in(AiPromptTemplate::getScene, scene.dbScenes())
                    .orderByAsc(AiPromptTemplate::getId)
                    .last("LIMIT 1");
            List<AiPromptTemplate> list = templateMapper.selectList(wrapper);
            return list == null || list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            log.warn("读取 ai_prompt_template 失败，使用内置默认模板: {}", e.getMessage());
            return null;
        }
    }

    /* ==================== 管理端 ==================== */

    public PageResult<AiPromptTemplate> page(PageQuery query, String scene, String code, String keyword) {
        LambdaQueryWrapper<AiPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(scene)) {
            wrapper.in(AiPromptTemplate::getScene, AiScene.of(scene).dbScenes());
        }
        wrapper.eq(StringUtils.hasText(code), AiPromptTemplate::getCode, code);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiPromptTemplate::getName, keyword)
                    .or().like(AiPromptTemplate::getCode, keyword)
                    .or().like(AiPromptTemplate::getRemark, keyword));
        }
        wrapper.orderByAsc(AiPromptTemplate::getId);
        Page<AiPromptTemplate> page = templateMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    public AiPromptTemplate create(AiPromptTemplate template) {
        validate(template, true);
        template.setId(null);
        template.setCode(template.getCode().trim());
        template.setScene(AiScene.of(template.getScene()).getScene());
        if (!StringUtils.hasText(template.getStatus())) {
            template.setStatus("0");
        }
        if (template.getTemperature() == null) {
            template.setTemperature(new BigDecimal("0.70"));
        }
        if (template.getMaxTokens() == null || template.getMaxTokens() <= 0) {
            template.setMaxTokens(1024);
        }
        Long exists = templateMapper.selectCount(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getCode, template.getCode()));
        ServiceException.throwIf(exists != null && exists > 0, "模板编码已存在：" + template.getCode());
        templateMapper.insert(template);
        return template;
    }

    public AiPromptTemplate update(AiPromptTemplate template) {        ServiceException.throwIf(template == null || template.getId() == null, "模板ID不能为空");
        AiPromptTemplate existing = templateMapper.selectById(template.getId());
        ServiceException.throwIf(existing == null, "提示词模板不存在");
        if (StringUtils.hasText(template.getCode())) {
            template.setCode(template.getCode().trim());
            Long exists = templateMapper.selectCount(new LambdaQueryWrapper<AiPromptTemplate>()
                    .eq(AiPromptTemplate::getCode, template.getCode())
                    .ne(AiPromptTemplate::getId, template.getId()));
            ServiceException.throwIf(exists != null && exists > 0, "模板编码已存在：" + template.getCode());
        } else {
            template.setCode(existing.getCode());
        }
        if (!StringUtils.hasText(template.getScene())) {
            template.setScene(existing.getScene());
        } else {
            template.setScene(AiScene.of(template.getScene()).getScene());
        }
        if (!StringUtils.hasText(template.getStatus())) {
            template.setStatus(existing.getStatus());
        }
        if (!StringUtils.hasText(template.getSystemPrompt())) {
            template.setSystemPrompt(existing.getSystemPrompt());
        }
        if (!StringUtils.hasText(template.getUserPrompt())) {
            template.setUserPrompt(existing.getUserPrompt());
        }
        if (!StringUtils.hasText(template.getName())) {
            template.setName(existing.getName());
        }
        templateMapper.updateById(template);
        return templateMapper.selectById(template.getId());
    }

    public void delete(Long id) {
        ServiceException.throwIf(id == null, "模板ID不能为空");
        AiPromptTemplate existing = templateMapper.selectById(id);
        ServiceException.throwIf(existing == null, "提示词模板不存在或已删除");
        if (templateMapper.deleteById(id) <= 0) {
            throw new ServiceException("提示词模板不存在或已删除");
        }
    }

    private void validate(AiPromptTemplate template, boolean requireContent) {
        ServiceException.throwIf(template == null, "参数不能为空");
        ServiceException.throwIf(!StringUtils.hasText(template.getCode()), "模板编码不能为空");
        ServiceException.throwIf(!StringUtils.hasText(template.getName()), "模板名称不能为空");
        ServiceException.throwIf(!StringUtils.hasText(template.getScene()), "模板场景不能为空");
        if (requireContent) {
            ServiceException.throwIf(!StringUtils.hasText(template.getSystemPrompt()), "系统提示词不能为空");
            ServiceException.throwIf(!StringUtils.hasText(template.getUserPrompt()), "用户提示词不能为空");
        }
    }
}
