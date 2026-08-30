package com.qc.template.service.impl;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import com.qc.template.exception.ThrowUtils;
import com.qc.template.mapper.ArticleMapper;
import com.qc.template.model.dto.article.ArticleQueryRequest;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.dto.image.ImageRequest;
import com.qc.template.model.entity.Article;
import com.qc.template.model.entity.User;
import com.qc.template.model.enums.ArticlePhaseEnum;
import com.qc.template.model.enums.ArticleStatusEnum;
import com.qc.template.model.vo.ArticleVO;
import com.qc.template.model.vo.UserArticleStatsVO;
import com.qc.template.service.ArticleAgentService;
import com.qc.template.service.ArticleService;
import com.qc.template.service.ImageServiceStrategy;
import com.qc.template.service.QuotaService;
import com.qc.template.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.qc.template.model.enums.ImageMethodEnum;

import static com.qc.template.constant.UserConstant.ADMIN_ROLE;
import static com.qc.template.constant.UserConstant.VIP_ROLE;

/**
 * 文章服务实现类
 *
 * @author <a href="https://codefather.cn">编程导航学习网</a>
 */
@Service
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Resource
    private QuotaService quotaService;

    @Resource
    private ArticleAgentService articleAgentService;

    @Resource
    private ImageServiceStrategy imageServiceStrategy;

    @Override
    public String createArticleTask(String topic, String style, List<String> enabledImageMethods,
                                    String referenceSummary, User loginUser) {
        // 处理配图方式：如果用户未选择，给普通用户设置默认的非 VIP 方式
        List<String> finalImageMethods = processImageMethods(enabledImageMethods, loginUser);
        
        // 校验配图方式权限（普通用户不能使用 NANO_BANANA 和 SVG_DIAGRAM）
        validateImageMethods(finalImageMethods, loginUser);

        // 生成任务ID
        String taskId = IdUtil.simpleUUID();

        // 创建文章记录
        Article article = new Article();
        article.setTaskId(taskId);
        article.setUserId(loginUser.getId());
        article.setTopic(topic);
        article.setReferenceSummary(referenceSummary);
        article.setStyle(style);
        article.setEnabledImageMethods(finalImageMethods != null && !finalImageMethods.isEmpty() 
                ? GsonUtils.toJson(finalImageMethods) : null);
        article.setStatus(ArticleStatusEnum.PENDING.getValue());
        article.setPhase(ArticlePhaseEnum.PENDING.getValue());
        article.setCreateTime(LocalDateTime.now());

        this.save(article);

        log.info("文章任务已创建: taskId={}, userId={}, style={}", taskId, loginUser.getId(), style);
        return taskId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createArticleTaskWithQuotaCheck(String topic, String style, List<String> enabledImageMethods,
                                                  String referenceSummary, User loginUser) {
        // 在同一事务中：先扣配额，再创建任务
        // 如果任务创建失败，配额会自动回滚
        quotaService.checkAndConsumeQuota(loginUser);
        return createArticleTask(topic, style, enabledImageMethods, referenceSummary, loginUser);
    }

    @Override
    public Article getByTaskId(String taskId) {
        return this.getOne(
                QueryWrapper.create().eq("taskId", taskId)
        );
    }

    @Override
    public ArticleVO getArticleDetail(String taskId, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");

        // Only the owner and administrators may view the article.
        checkArticlePermission(article, loginUser);

        return ArticleVO.objToVo(article);
    }

    @Override
    public Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser) {
        long current = request.getPageNum();
        long size = request.getPageSize();

        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("isDelete", 0)
                .orderBy("createTime", false);

        // Non-admin users can only view their own articles.
        if (!ADMIN_ROLE.equals(loginUser.getUserRole())) {
            queryWrapper.eq("userId", loginUser.getId());
        } else if (request.getUserId() != null) {
            queryWrapper.eq("userId", request.getUserId());
        }

        // Filter by status.
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            queryWrapper.eq("status", request.getStatus());
        }

        // 分页查询
        Page<Article> articlePage = this.page(new Page<>(current, size), queryWrapper);

        // 转换为 VO
        return convertToVOPage(articlePage);
    }

    @Override
    public UserArticleStatsVO getUserArticleStats(User loginUser) {
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        List<Article> articles = this.list(QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("isDelete", 0)
                .orderBy("createTime", false));

        long completedWorks = articles.stream()
                .filter(article -> ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()))
                .count();
        long totalCharacters = articles.stream()
                .map(article -> article.getFullContent() != null && !article.getFullContent().isBlank()
                        ? article.getFullContent() : article.getContent())
                .filter(content -> content != null && !content.isBlank())
                .mapToLong(String::length)
                .sum();

        UserArticleStatsVO stats = new UserArticleStatsVO();
        stats.setTotalWorks((long) articles.size());
        stats.setCompletedWorks(completedWorks);
        stats.setTotalCharacters(totalCharacters);
        stats.setLatestWorkTime(articles.isEmpty() ? null : articles.get(0).getCreateTime());
        return stats;
    }

    @Override
    public boolean deleteArticle(Long id, User loginUser) {
        Article article = this.getById(id);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);

        // Only the owner and administrators may delete the article.
        checkArticlePermission(article, loginUser);

        // 逻辑删除
        return this.removeById(id);
    }

    @Override
    public void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("文章记录不存在: taskId={}", taskId);
            return;
        }

        article.setStatus(status.getValue());
        article.setErrorMessage(errorMessage);
        this.updateById(article);

        log.info("文章状已更新, taskId={}, status={}", taskId, status.getValue());
    }

    @Override
    public void saveArticleContent(String taskId, ArticleState state) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("文章记录不存在: taskId={}", taskId);
            return;
        }

        article.setMainTitle(state.getTitle().getMainTitle());
        article.setSubTitle(state.getTitle().getSubTitle());
        article.setOutline(GsonUtils.toJson(state.getOutline().getSections()));
        article.setContent(state.getContent());
        article.setFullContent(state.getFullContent());
        
        // Save the cover image URL from the image with position 1.
        if (state.getImages() != null && !state.getImages().isEmpty()) {
            ArticleState.ImageResult cover = state.getImages().stream()
                .filter(img -> img.getPosition() != null && img.getPosition() == 1)
                .findFirst()
                .orElse(null);
            if (cover != null && cover.getUrl() != null) {
                article.setCoverImage(cover.getUrl());
            }
        }
        article.setImages(GsonUtils.toJson(state.getImages()));
        article.setCompletedTime(LocalDateTime.now());

        this.updateById(article);
        log.info("文章保存成功, taskId={}", taskId);
    }

    /**
     * 校验文章权限
     *
     * @param article   文章
     * @param loginUser 当前用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteArticles(List<Long> ids, User loginUser) {
        int deletedCount = 0;
        for (Long id : ids) {
            if (deleteArticle(id, loginUser)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Override
    public void updateArticleContent(String taskId, String mainTitle, String subTitle, String content, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "Article is not completed");

        if (mainTitle != null && !mainTitle.isBlank()) {
            article.setMainTitle(mainTitle.trim());
        }
        if (subTitle != null) {
            article.setSubTitle(subTitle.trim());
        }
        article.setFullContent(content);
        this.updateById(article);
    }

    @Override
    public String aiEditArticleContent(String taskId, String content, String instruction, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "Article is not completed");
        return articleAgentService.editArticleContent(content, instruction);
    }

    @Override
    public ArticleVO regenerateArticleImage(String taskId, Integer position, String prompt, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "Article is not completed");

        List<ArticleVO.ImageItem> images = GsonUtils.fromJson(article.getImages(),
                new TypeToken<List<ArticleVO.ImageItem>>() {});
        if (images == null) {
            images = new ArrayList<>();
        }

        ArticleVO.ImageItem image = images.stream()
                .filter(item -> position.equals(item.getPosition()))
                .findFirst()
                .orElse(null);
        if (image == null) {
            image = new ArticleVO.ImageItem();
            image.setPosition(position);
            image.setMethod(ImageMethodEnum.IMAGE_2.getValue());
            image.setSectionTitle(position == 1 ? "文章封面" : "文章配图");
            image.setDescription(position == 1 ? "cover" : "section");
            images.add(image);
        }

        String oldUrl = image.getUrl();
        String generationPrompt = prompt == null || prompt.isBlank()
                ? (image.getKeywords() == null || image.getKeywords().isBlank()
                    ? article.getMainTitle() + "，真实、清晰、适合文章配图的场景"
                    : image.getKeywords())
                : prompt.trim();
        ImageRequest imageRequest = ImageRequest.builder()
                .keywords(generationPrompt)
                .prompt(generationPrompt)
                .position(position)
                .type(position == 1 ? "cover" : "section")
                .sourceImageUrl(oldUrl)
                .build();
        ImageServiceStrategy.ImageResult result = imageServiceStrategy.getImageAndUpload(image.getMethod(), imageRequest);
        ThrowUtils.throwIf(!result.isSuccess(), ErrorCode.SYSTEM_ERROR, "Image regeneration failed");

        ensureImageVersions(image);
        ArticleVO.ImageVersion newVersion = new ArticleVO.ImageVersion();
        newVersion.setId(IdUtil.simpleUUID());
        newVersion.setUrl(result.getUrl());
        newVersion.setPrompt(generationPrompt);
        newVersion.setCreatedTime(LocalDateTime.now());
        image.getVersions().add(newVersion);
        image.setSelectedVersionId(newVersion.getId());
        image.setUrl(newVersion.getUrl());
        image.setMethod(result.getMethod().getValue());
        image.setKeywords(generationPrompt);
        article.setImages(GsonUtils.toJson(images));
        if (position == 1) {
            article.setCoverImage(result.getUrl());
        }
        if (article.getFullContent() != null && oldUrl != null) {
            article.setFullContent(article.getFullContent().replace(oldUrl, result.getUrl()));
        }
        this.updateById(article);
        return ArticleVO.objToVo(article);
    }

    @Override
    public ArticleVO selectArticleImageVersion(String taskId, Integer position, String versionId, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "Article is not completed");
        List<ArticleVO.ImageItem> images = GsonUtils.fromJson(article.getImages(),
                new TypeToken<List<ArticleVO.ImageItem>>() {});
        ThrowUtils.throwIf(images == null || images.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "No article images found");
        ArticleVO.ImageItem image = images.stream()
                .filter(item -> position.equals(item.getPosition()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Image not found"));
        String oldUrl = image.getUrl();
        ensureImageVersions(image);
        ArticleVO.ImageVersion selectedVersion = image.getVersions().stream()
                .filter(version -> versionId.equals(version.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Image version not found"));
        image.setSelectedVersionId(selectedVersion.getId());
        image.setUrl(selectedVersion.getUrl());
        article.setImages(GsonUtils.toJson(images));
        if (position == 1) {
            article.setCoverImage(selectedVersion.getUrl());
        }
        if (article.getFullContent() != null && oldUrl != null) {
            article.setFullContent(article.getFullContent().replace(oldUrl, selectedVersion.getUrl()));
        }
        this.updateById(article);
        return ArticleVO.objToVo(article);
    }

    private void ensureImageVersions(ArticleVO.ImageItem image) {
        if (image.getVersions() != null && !image.getVersions().isEmpty()) {
            return;
        }
        image.setVersions(new ArrayList<>());
        if (image.getUrl() == null || image.getUrl().isBlank()) {
            return;
        }
        ArticleVO.ImageVersion originalVersion = new ArticleVO.ImageVersion();
        originalVersion.setId(IdUtil.simpleUUID());
        originalVersion.setUrl(image.getUrl());
        originalVersion.setPrompt(image.getKeywords());
        originalVersion.setCreatedTime(LocalDateTime.now());
        image.getVersions().add(originalVersion);
        image.setSelectedVersionId(originalVersion.getId());
    }

    private void checkArticlePermission(Article article, User loginUser) {
        if (!article.getUserId().equals(loginUser.getId()) &&
                !ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 将文章分页结果转换为 VO 分页
     *
     * @param articlePage 文章分页
     * @return VO 分页
     */
    private Page<ArticleVO> convertToVOPage(Page<Article> articlePage) {
        Page<ArticleVO> articleVOPage = new Page<>();
        articleVOPage.setPageNumber(articlePage.getPageNumber());
        articleVOPage.setPageSize(articlePage.getPageSize());
        articleVOPage.setTotalRow(articlePage.getTotalRow());

        List<ArticleVO> articleVOList = articlePage.getRecords().stream()
                .map(ArticleVO::objToVo)
                .collect(Collectors.toList());
        articleVOPage.setRecords(articleVOList);

        return articleVOPage;
    }

    @Override
    public void confirmTitle(String taskId, String mainTitle, String subTitle, String userDescription, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");

        // 校验权限
        checkArticlePermission(article, loginUser);

        // The current phase must be TITLE_SELECTING.
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.TITLE_SELECTING,
                ErrorCode.OPERATION_ERROR, "当前阶段不允许此操作");

        // 保存用户选择的标题和补充描述
        article.setMainTitle(mainTitle);
        article.setSubTitle(subTitle);
        article.setUserDescription(userDescription);
        article.setPhase(ArticlePhaseEnum.OUTLINE_GENERATING.getValue());

        this.updateById(article);
        log.info("用户确认标题, taskId={}, mainTitle={}", taskId, mainTitle);
    }

    @Override
    public void confirmOutline(String taskId, List<ArticleState.OutlineSection> outline, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");

        // 校验权限
        checkArticlePermission(article, loginUser);

        // The current phase must be OUTLINE_EDITING.
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.OUTLINE_EDITING,
                ErrorCode.OPERATION_ERROR, "当前阶段不允许此操作");

        // 保存用户编辑后的大纲
        article.setOutline(GsonUtils.toJson(outline));
        article.setPhase(ArticlePhaseEnum.CONTENT_GENERATING.getValue());

        this.updateById(article);
        log.info("用户确认大纲, taskId={}, sectionsCount={}", taskId, outline.size());
    }

    @Override
    public void updatePhase(String taskId, ArticlePhaseEnum phase) {
        Article article = getByTaskId(taskId);
        if (article == null) {
            log.error("文章记录不存在: taskId={}", taskId);
            return;
        }

        article.setPhase(phase.getValue());
        this.updateById(article);
        log.info("文章阶段已更新: taskId={}, phase={}", taskId, phase.getValue());
    }

    @Override
    public void saveTitleOptions(String taskId, List<ArticleState.TitleOption> titleOptions) {
        Article article = getByTaskId(taskId);
        if (article == null) {
            log.error("文章记录不存在: taskId={}", taskId);
            return;
        }

        article.setTitleOptions(GsonUtils.toJson(titleOptions));
        this.updateById(article);
        log.info("标题方案已保存: taskId={}, optionsCount={}", taskId, titleOptions.size());
    }

    @Override
    public List<ArticleState.OutlineSection> aiModifyOutline(String taskId, String modifySuggestion, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "Article not found");

        // 校验权限
        checkArticlePermission(article, loginUser);

        // AI outline editing is restricted to VIP users and administrators.
        ThrowUtils.throwIf(!isVipOrAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, 
                "AI 修改大纲功能仅限 VIP 会员使用");

        // The current phase must be OUTLINE_EDITING.
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.OUTLINE_EDITING,
                ErrorCode.OPERATION_ERROR, "当前阶段不允许此操作");

        // 获取当前大纲
        List<ArticleState.OutlineSection> currentOutline = GsonUtils.fromJson(
                article.getOutline(),
                new TypeToken<List<ArticleState.OutlineSection>>(){}
        );

        // 调用 AI 修改大纲
        List<ArticleState.OutlineSection> modifiedOutline = articleAgentService.aiModifyOutline(
                article.getMainTitle(),
                article.getSubTitle(),
                currentOutline,
                modifySuggestion
        );

        // 保存修改后的大纲
        article.setOutline(GsonUtils.toJson(modifiedOutline));
        this.updateById(article);

        log.info("AI修改大纲完成, taskId={}, sectionsCount={}", taskId, modifiedOutline.size());
        return modifiedOutline;
    }

    /**
     * 处理配图方式
     * 如果用户未选择，给普通用户设置默认的非 VIP 方式，VIP 用户不限制
     */
    private List<String> processImageMethods(List<String> enabledImageMethods, User loginUser) {
        // Preserve explicitly selected image methods.
        if (enabledImageMethods != null && !enabledImageMethods.isEmpty()) {
            return enabledImageMethods;
        }

        // VIP users and administrators support all image methods.
        if (isVipOrAdmin(loginUser)) {
            return null;
        }

        // 普用户：返回默认的非 VIP 方式
        return List.of(
                ImageMethodEnum.PEXELS.getValue(),
                ImageMethodEnum.MERMAID.getValue(),
                ImageMethodEnum.ICONIFY.getValue(),
                ImageMethodEnum.EMOJI_PACK.getValue()
        );
    }

    /**
     * 校验配图方式权限
     * 普通用户不能使用 NANO_BANANA 和 SVG_DIAGRAM
     */
    private void validateImageMethods(List<String> enabledImageMethods, User loginUser) {
        if (enabledImageMethods == null || enabledImageMethods.isEmpty()) {
            return;
        }

        // VIP users and administrators have no restrictions.
        if (isVipOrAdmin(loginUser)) {
            return;
        }

        // Validate methods for regular users.
        for (String method : enabledImageMethods) {
            if (ImageMethodEnum.NANO_BANANA.getValue().equals(method) ||
                ImageMethodEnum.NANO_BANANA_APICLAUDE.getValue().equals(method) ||
                ImageMethodEnum.IMAGE_2.getValue().equals(method) ||
                ImageMethodEnum.SVG_DIAGRAM.getValue().equals(method)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, 
                        "高级配图功能（AI 生图、SVG 图表）仅限 VIP 会员使用");
            }
        }
    }

    /**
     * 判断是否为 VIP 或管理员
     */
    private boolean isVipOrAdmin(User user) {
        return ADMIN_ROLE.equals(user.getUserRole()) || 
               VIP_ROLE.equals(user.getUserRole());
    }
}
