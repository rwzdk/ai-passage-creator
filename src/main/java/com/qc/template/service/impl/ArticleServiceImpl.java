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
 * 鏂囩珷鏈嶅姟瀹炵幇绫?
 *
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
        // 澶勭悊閰嶅浘鏂瑰紡锛氬鏋滅敤鎴锋湭閫夋嫨锛岀粰鏅€氱敤鎴疯缃粯璁ょ殑闈?VIP 鏂瑰紡
        List<String> finalImageMethods = processImageMethods(enabledImageMethods, loginUser);
        
        // 鏍￠獙閰嶅浘鏂瑰紡鏉冮檺锛堟櫘閫氱敤鎴蜂笉鑳戒娇鐢?NANO_BANANA 鍜?SVG_DIAGRAM锛?
        validateImageMethods(finalImageMethods, loginUser);

        // 鐢熸垚浠诲姟ID
        String taskId = IdUtil.simpleUUID();

        // 鍒涘缓鏂囩珷璁板綍
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

        log.info("鏂囩珷浠诲姟宸插垱寤? taskId={}, userId={}, style={}", taskId, loginUser.getId(), style);
        return taskId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createArticleTaskWithQuotaCheck(String topic, String style, List<String> enabledImageMethods,
                                                  String referenceSummary, User loginUser) {
        // 鍦ㄥ悓涓€浜嬪姟涓細鍏堟墸閰嶉锛屽啀鍒涘缓浠诲姟
        // 濡傛灉浠诲姟鍒涘缓澶辫触锛岄厤棰濅細鑷姩鍥炴粴
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
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);

        // 鏍￠獙鏉冮檺锛氬彧鑳芥煡鐪嬭嚜宸辩殑鏂囩珷锛堢鐞嗗憳闄ゅ锛?
        checkArticlePermission(article, loginUser);

        return ArticleVO.objToVo(article);
    }

    @Override
    public Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser) {
        long current = request.getPageNum();
        long size = request.getPageSize();

        // 鏋勫缓鏌ヨ鏉′欢
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("isDelete", 0)
                .orderBy("createTime", false);

        // 闈炵鐞嗗憳鍙兘鏌ョ湅鑷繁鐨勬枃绔?
        if (!ADMIN_ROLE.equals(loginUser.getUserRole())) {
            queryWrapper.eq("userId", loginUser.getId());
        } else if (request.getUserId() != null) {
            queryWrapper.eq("userId", request.getUserId());
        }

        // 鎸夌姸鎬佺瓫閫?
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            queryWrapper.eq("status", request.getStatus());
        }

        // 鍒嗛〉鏌ヨ
        Page<Article> articlePage = this.page(new Page<>(current, size), queryWrapper);

        // 杞崲涓?VO
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

        // 鏍￠獙鏉冮檺锛氬彧鑳藉垹闄よ嚜宸辩殑鏂囩珷锛堢鐞嗗憳闄ゅ锛?
        checkArticlePermission(article, loginUser);

        // 閫昏緫鍒犻櫎
        return this.removeById(id);
    }

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
    public void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("鏂囩珷璁板綍涓嶅瓨鍦? taskId={}", taskId);
            return;
        }

        article.setStatus(status.getValue());
        article.setErrorMessage(errorMessage);
        this.updateById(article);

        log.info("鏂囩珷鐘舵€佸凡鏇存柊, taskId={}, status={}", taskId, status.getValue());
    }

    @Override
    public void saveArticleContent(String taskId, ArticleState state) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("鏂囩珷璁板綍涓嶅瓨鍦? taskId={}", taskId);
            return;
        }

        article.setMainTitle(state.getTitle().getMainTitle());
        article.setSubTitle(state.getTitle().getSubTitle());
        article.setOutline(GsonUtils.toJson(state.getOutline().getSections()));
        article.setContent(state.getContent());
        article.setFullContent(state.getFullContent());
        
        // 淇濆瓨灏侀潰鍥?URL锛堜粠 images 鍒楄〃涓彁鍙?position=1 鐨?URL锛?
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
        log.info("鏂囩珷淇濆瓨鎴愬姛, taskId={}", taskId);
    }

    @Override
    public void updateArticleContent(String taskId, String mainTitle, String subTitle, String content, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "鏂囩珷灏氭湭鍒涗綔瀹屾垚锛屾殏涓嶈兘缂栬緫");

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
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "鏂囩珷灏氭湭鍒涗綔瀹屾垚锛屾殏涓嶈兘浣跨敤 AI 缂栬緫");
        return articleAgentService.editArticleContent(content, instruction);
    }

    @Override
    public ArticleVO regenerateArticleImage(String taskId, Integer position, String prompt, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "鏂囩珷灏氭湭鍒涗綔瀹屾垚锛屾殏涓嶈兘淇敼閰嶅浘");

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
            image.setSectionTitle(position == 1 ? "鏂囩珷灏侀潰" : "鏂囩珷閰嶅浘");
            image.setDescription(position == 1 ? "cover" : "section");
            images.add(image);
        }
        String oldUrl = image.getUrl();
        String generationPrompt = prompt == null || prompt.isBlank()
                ? (image.getKeywords() == null || image.getKeywords().isBlank()
                    ? article.getMainTitle() + "锛岀數褰辨殤鏈熸。鐑害涓庤浼楄褰辨疆鐨勭湡瀹炲満鏅?
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
        ThrowUtils.throwIf(!result.isSuccess(), ErrorCode.SYSTEM_ERROR, "鍥剧墖閲嶆柊鐢熸垚澶辫触");

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
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "鏂囩珷灏氭湭鍒涗綔瀹屾垚锛屾殏涓嶈兘鍒囨崲閰嶅浘");
        List<ArticleVO.ImageItem> images = GsonUtils.fromJson(article.getImages(),
                new TypeToken<List<ArticleVO.ImageItem>>() {});
        ThrowUtils.throwIf(images == null || images.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "鏂囩珷娌℃湁鍙垏鎹㈢殑閰嶅浘");
        ArticleVO.ImageItem image = images.stream()
                .filter(item -> position.equals(item.getPosition()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "閰嶅浘涓嶅瓨鍦?));
        String oldUrl = image.getUrl();
        ensureImageVersions(image);
        ArticleVO.ImageVersion selectedVersion = image.getVersions().stream()
                .filter(version -> versionId.equals(version.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "鍥剧墖鐗堟湰涓嶅瓨鍦?));
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

    /**
     * 鏍￠獙鏂囩珷鏉冮檺
     *
     * @param article   鏂囩珷
     * @param loginUser 褰撳墠鐢ㄦ埛
     */
    private void checkArticlePermission(Article article, User loginUser) {
        if (!article.getUserId().equals(loginUser.getId()) &&
                !ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 灏嗘枃绔犲垎椤电粨鏋滆浆鎹负 VO 鍒嗛〉
     *
     * @param articlePage 鏂囩珷鍒嗛〉
     * @return VO 鍒嗛〉
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
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);

        // 鏍￠獙鏉冮檺
        checkArticlePermission(article, loginUser);

        // 鏍￠獙褰撳墠闃舵锛堝繀椤绘槸 TITLE_SELECTING锛?
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.TITLE_SELECTING,
                ErrorCode.OPERATION_ERROR, "褰撳墠闃舵涓嶅厑璁告鎿嶄綔");

        // 淇濆瓨鐢ㄦ埛閫夋嫨鐨勬爣棰樺拰琛ュ厖鎻忚堪
        article.setMainTitle(mainTitle);
        article.setSubTitle(subTitle);
        article.setUserDescription(userDescription);
        article.setPhase(ArticlePhaseEnum.OUTLINE_GENERATING.getValue());

        this.updateById(article);
        log.info("鐢ㄦ埛纭鏍囬, taskId={}, mainTitle={}", taskId, mainTitle);
    }

    @Override
    public void confirmOutline(String taskId, List<ArticleState.OutlineSection> outline, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);

        // 鏍￠獙鏉冮檺
        checkArticlePermission(article, loginUser);

        // 鏍￠獙褰撳墠闃舵锛堝繀椤绘槸 OUTLINE_EDITING锛?
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.OUTLINE_EDITING,
                ErrorCode.OPERATION_ERROR, "褰撳墠闃舵涓嶅厑璁告鎿嶄綔");

        // 淇濆瓨鐢ㄦ埛缂栬緫鍚庣殑澶х翰
        article.setOutline(GsonUtils.toJson(outline));
        article.setPhase(ArticlePhaseEnum.CONTENT_GENERATING.getValue());

        this.updateById(article);
        log.info("鐢ㄦ埛纭澶х翰, taskId={}, sectionsCount={}", taskId, outline.size());
    }

    @Override
    public void updatePhase(String taskId, ArticlePhaseEnum phase) {
        Article article = getByTaskId(taskId);
        if (article == null) {
            log.error("鏂囩珷璁板綍涓嶅瓨鍦? taskId={}", taskId);
            return;
        }

        article.setPhase(phase.getValue());
        this.updateById(article);
        log.info("鏂囩珷闃舵宸叉洿鏂? taskId={}, phase={}", taskId, phase.getValue());
    }

    @Override
    public void saveTitleOptions(String taskId, List<ArticleState.TitleOption> titleOptions) {
        Article article = getByTaskId(taskId);
        if (article == null) {
            log.error("鏂囩珷璁板綍涓嶅瓨鍦? taskId={}", taskId);
            return;
        }

        article.setTitleOptions(GsonUtils.toJson(titleOptions));
        this.updateById(article);
        log.info("鏍囬鏂规宸蹭繚瀛? taskId={}, optionsCount={}", taskId, titleOptions.size());
    }

    @Override
    public List<ArticleState.OutlineSection> aiModifyOutline(String taskId, String modifySuggestion, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "鏂囩珷涓嶅瓨鍦?);

        // 鏍￠獙鏉冮檺
        checkArticlePermission(article, loginUser);

        // 鏍￠獙 VIP 鏉冮檺锛堟櫘閫氱敤鎴蜂笉鑳戒娇鐢?AI 淇敼澶х翰锛?
        ThrowUtils.throwIf(!isVipOrAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, 
                "AI 淇敼澶х翰鍔熻兘浠呴檺 VIP 浼氬憳浣跨敤");

        // 鏍￠獙褰撳墠闃舵锛堝繀椤绘槸 OUTLINE_EDITING锛?
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.OUTLINE_EDITING,
                ErrorCode.OPERATION_ERROR, "褰撳墠闃舵涓嶅厑璁告鎿嶄綔");

        // 鑾峰彇褰撳墠澶х翰
        List<ArticleState.OutlineSection> currentOutline = GsonUtils.fromJson(
                article.getOutline(),
                new TypeToken<List<ArticleState.OutlineSection>>(){}
        );

        // 璋冪敤 AI 淇敼澶х翰
        List<ArticleState.OutlineSection> modifiedOutline = articleAgentService.aiModifyOutline(
                article.getMainTitle(),
                article.getSubTitle(),
                currentOutline,
                modifySuggestion
        );

        // 淇濆瓨淇敼鍚庣殑澶х翰
        article.setOutline(GsonUtils.toJson(modifiedOutline));
        this.updateById(article);

        log.info("AI淇敼澶х翰瀹屾垚, taskId={}, sectionsCount={}", taskId, modifiedOutline.size());
        return modifiedOutline;
    }

    /**
     * 澶勭悊閰嶅浘鏂瑰紡
     * 濡傛灉鐢ㄦ埛鏈€夋嫨锛岀粰鏅€氱敤鎴疯缃粯璁ょ殑闈?VIP 鏂瑰紡锛孷IP 鐢ㄦ埛涓嶉檺鍒?
     */
    private List<String> processImageMethods(List<String> enabledImageMethods, User loginUser) {
        // 濡傛灉鐢ㄦ埛宸查€夋嫨锛岀洿鎺ヨ繑鍥?
        if (enabledImageMethods != null && !enabledImageMethods.isEmpty()) {
            return enabledImageMethods;
        }

        // VIP 鍜岀鐞嗗憳锛氫笉闄愬埗锛岃繑鍥?null 琛ㄧず鏀寔鎵€鏈夋柟寮?
        if (isVipOrAdmin(loginUser)) {
            return null;
        }

        // 鏅€氱敤鎴凤細杩斿洖榛樿鐨勯潪 VIP 鏂瑰紡
        return List.of(
                ImageMethodEnum.PEXELS.getValue(),
                ImageMethodEnum.MERMAID.getValue(),
                ImageMethodEnum.ICONIFY.getValue(),
                ImageMethodEnum.EMOJI_PACK.getValue()
        );
    }

    /**
     * 鏍￠獙閰嶅浘鏂瑰紡鏉冮檺
     * 鏅€氱敤鎴蜂笉鑳戒娇鐢?NANO_BANANA 鍜?SVG_DIAGRAM
     */
    private void validateImageMethods(List<String> enabledImageMethods, User loginUser) {
        if (enabledImageMethods == null || enabledImageMethods.isEmpty()) {
            return;
        }

        // VIP 鍜岀鐞嗗憳鏃犻檺鍒?
        if (isVipOrAdmin(loginUser)) {
            return;
        }

        // 鏅€氱敤鎴烽檺鍒?
        for (String method : enabledImageMethods) {
            if (ImageMethodEnum.NANO_BANANA.getValue().equals(method) ||
                ImageMethodEnum.NANO_BANANA_APICLAUDE.getValue().equals(method) ||
                ImageMethodEnum.IMAGE_2.getValue().equals(method) ||
                ImageMethodEnum.SVG_DIAGRAM.getValue().equals(method)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, 
                        "楂樼骇閰嶅浘鍔熻兘锛圓I 鐢熷浘銆丼VG 鍥捐〃锛変粎闄?VIP 浼氬憳浣跨敤");
            }
        }
    }

    /**
     * 鍒ゆ柇鏄惁涓?VIP 鎴栫鐞嗗憳
     */
    private boolean isVipOrAdmin(User user) {
        return ADMIN_ROLE.equals(user.getUserRole()) || 
               VIP_ROLE.equals(user.getUserRole());
    }
}
