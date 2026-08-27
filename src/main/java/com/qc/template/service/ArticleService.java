package com.qc.template.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.qc.template.model.dto.article.ArticleQueryRequest;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.entity.Article;
import com.qc.template.model.entity.User;
import com.qc.template.model.enums.ArticlePhaseEnum;
import com.qc.template.model.enums.ArticleStatusEnum;
import com.qc.template.model.vo.ArticleVO;
import com.qc.template.model.vo.UserArticleStatsVO;

import java.util.List;

/**
 * 鏂囩珷鏈嶅姟鎺ュ彛
 *
 */
public interface ArticleService extends IService<Article> {

    /**
     * 鍒涘缓鏂囩珷浠诲姟
     *
     * @param topic     閫夐
     * @param style     鏂囩珷椋庢牸锛堝彲涓虹┖锛?
     * @param enabledImageMethods 鍏佽鐨勯厤鍥炬柟寮忓垪琛紙鍙负绌猴級
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     * @return 浠诲姟ID
     */
    String createArticleTask(String topic, String style, List<String> enabledImageMethods,
                             String referenceSummary, User loginUser);

    /**
     * 鍒涘缓鏂囩珷浠诲姟锛堝甫閰嶉妫€鏌ワ級
     * 灏嗛厤棰濇墸鍑忓拰浠诲姟鍒涘缓鏀惧湪鍚屼竴浜嬪姟涓紝纭繚鍘熷瓙鎬?
     *
     * @param topic     閫夐
     * @param style     鏂囩珷椋庢牸锛堝彲涓虹┖锛?
     * @param enabledImageMethods 鍏佽鐨勯厤鍥炬柟寮忓垪琛紙鍙负绌猴級
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     * @return 浠诲姟ID
     */
    String createArticleTaskWithQuotaCheck(String topic, String style, List<String> enabledImageMethods,
                                           String referenceSummary, User loginUser);

    /**
     * 鏍规嵁浠诲姟ID鑾峰彇鏂囩珷
     *
     * @param taskId 浠诲姟ID
     * @return 鏂囩珷瀹炰綋
     */
    Article getByTaskId(String taskId);

    /**
     * 鑾峰彇鏂囩珷璇︽儏锛堝甫鏉冮檺鏍￠獙锛?
     *
     * @param taskId    浠诲姟ID
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     * @return 鏂囩珷VO
     */
    ArticleVO getArticleDetail(String taskId, User loginUser);

    /**
     * 鍒嗛〉鏌ヨ鏂囩珷鍒楄〃
     *
     * @param request   鏌ヨ璇锋眰
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     * @return 鍒嗛〉缁撴灉
     */
    Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser);

    /** 鑾峰彇褰撳墠鐢ㄦ埛鐨勫垱浣滅粺璁°€?*/
    UserArticleStatsVO getUserArticleStats(User loginUser);

    /**
     * 鍒犻櫎鏂囩珷锛堝甫鏉冮檺鏍￠獙锛?
     *
     * @param id        鏂囩珷ID
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     * @return 鏄惁鎴愬姛
     */
    boolean deleteArticle(Long id, User loginUser);

    /**
     * 鎵归噺鍒犻櫎鏂囩珷锛岄€愭潯鎵ц宸叉湁鐨勫綊灞炴潈闄愭牎楠屻€?     *
     * @param ids       鏂囩珷 ID 鍒楄〃
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     * @return 鎴愬姛鍒犻櫎鐨勬暟閲?     */
    int deleteArticles(List<Long> ids, User loginUser);

    /**
     * 鏇存柊鏂囩珷鐘舵€?
     *
     * @param taskId       浠诲姟ID
     * @param status       鐘舵€佹灇涓?
     * @param errorMessage 错误信息（可选）
     */
    void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage);

    /**
     * 淇濆瓨鏂囩珷鍐呭
     *
     * @param taskId 浠诲姟ID
     * @param state  鏂囩珷鐘舵€佸璞?
     */
    void saveArticleContent(String taskId, ArticleState state);

    void updateArticleContent(String taskId, String mainTitle, String subTitle, String content, User loginUser);

    String aiEditArticleContent(String taskId, String content, String instruction, User loginUser);

    ArticleVO regenerateArticleImage(String taskId, Integer position, String prompt, User loginUser);

    ArticleVO selectArticleImageVersion(String taskId, Integer position, String versionId, User loginUser);

    /**
     * 纭鏍囬锛堢敤鎴烽€夋嫨鍚庯級
     *
     * @param taskId       浠诲姟ID
     * @param mainTitle    閫変腑鐨勪富鏍囬
     * @param subTitle     閫変腑鐨勫壇鏍囬
     * @param userDescription 鐢ㄦ埛琛ュ厖鎻忚堪
     * @param loginUser    褰撳墠鐧诲綍鐢ㄦ埛
     */
    void confirmTitle(String taskId, String mainTitle, String subTitle, String userDescription, User loginUser);

    /**
     * 纭澶х翰锛堢敤鎴风紪杈戝悗锛?
     *
     * @param taskId    浠诲姟ID
     * @param outline   鐢ㄦ埛缂栬緫鍚庣殑澶х翰
     * @param loginUser 褰撳墠鐧诲綍鐢ㄦ埛
     */
    void confirmOutline(String taskId, List<ArticleState.OutlineSection> outline, User loginUser);

    /**
     * 鏇存柊闃舵
     *
     * @param taskId 浠诲姟ID
     * @param phase  闃舵鏋氫妇
     */
    void updatePhase(String taskId, ArticlePhaseEnum phase);

    /**
     * 淇濆瓨鏍囬鏂规
     *
     * @param taskId       浠诲姟ID
     * @param titleOptions 鏍囬鏂规鍒楄〃
     */
    void saveTitleOptions(String taskId, List<ArticleState.TitleOption> titleOptions);

    /**
     * AI 淇敼澶х翰
     *
     * @param taskId           浠诲姟ID
     * @param modifySuggestion 鐢ㄦ埛淇敼寤鸿
     * @param loginUser        褰撳墠鐧诲綍鐢ㄦ埛
     * @return 淇敼鍚庣殑澶х翰
     */
    List<ArticleState.OutlineSection> aiModifyOutline(String taskId, String modifySuggestion, User loginUser);
}
