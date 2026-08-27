package com.qc.template.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.qc.template.constant.UserConstant;
import com.qc.template.mapper.ArticleMapper;
import com.qc.template.mapper.UserMapper;
import com.qc.template.model.entity.Article;
import com.qc.template.model.entity.User;
import com.qc.template.model.enums.ArticleStatusEnum;
import com.qc.template.model.vo.StatisticsVO;
import com.qc.template.service.AgentLogService;
import com.qc.template.service.StatisticsService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缁熻鏈嶅姟瀹炵幇
 *
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private static final String STATISTICS_CACHE_KEY = "statistics:overview";
    private static final long CACHE_EXPIRE_HOURS = 1L;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AgentLogService agentLogService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public StatisticsVO getStatistics(boolean forceRefresh) {
        // 鍏堜粠缂撳瓨鑾峰彇
        if (!forceRefresh) {
            StatisticsVO cachedStats = (StatisticsVO) redisTemplate.opsForValue().get(STATISTICS_CACHE_KEY);
            if (cachedStats != null) {
                log.info("浠庣紦瀛樿幏鍙栫粺璁℃暟鎹?);
                return cachedStats;
            }
        }

        // 缂撳瓨涓嶅瓨鍦紝閲嶆柊璁＄畻
        // 浠婃棩鍒涗綔鏁伴噺
        Long todayCount = countArticlesByDateRange(getTodayStart(), LocalDateTime.now());

        // 鏈懆鍒涗綔鏁伴噺
        Long weekCount = countArticlesByDateRange(getWeekStart(), LocalDateTime.now());

        // 鏈湀鍒涗綔鏁伴噺
        Long monthCount = countArticlesByDateRange(getMonthStart(), LocalDateTime.now());

        // 鎬诲垱浣滄暟閲?
        Long totalCount = countTotalArticles();

        // 鎴愬姛鐜囩粺璁?
        Double successRate = calculateSuccessRate();

        // 骞冲潎鑰楁椂缁熻
        Integer avgDurationMs = calculateAvgDuration();

        // 活跃用户统计（本周有创作的用户）
        Long activeUserCount = countActiveUsers(getWeekStart());

        // 鎬荤敤鎴锋暟
        Long totalUserCount = countTotalUsers();

        // VIP 鐢ㄦ埛鏁?
        Long vipUserCount = countVipUsers();

        Long normalUserCount = countNormalUsers();

        // 閰嶉浣跨敤鎯呭喌锛堟€婚厤棰?- 鍓╀綑閰嶉锛?
        Long quotaUsed = calculateQuotaUsed();

        StatisticsVO statistics = StatisticsVO.builder()
                .todayCount(todayCount)
                .weekCount(weekCount)
                .monthCount(monthCount)
                .totalCount(totalCount)
                .successRate(successRate)
                .avgDurationMs(avgDurationMs)
                .activeUserCount(activeUserCount)
                .totalUserCount(totalUserCount)
                .vipUserCount(vipUserCount)
                .normalUserCount(normalUserCount)
                .quotaUsed(quotaUsed)
                .build();

        // 瀛樺叆缂撳瓨锛? 灏忔椂杩囨湡
        redisTemplate.opsForValue().set(STATISTICS_CACHE_KEY, statistics, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.info("缁熻鏁版嵁宸茬紦瀛橈紝杩囨湡鏃堕棿: {} 灏忔椂", CACHE_EXPIRE_HOURS);

        return statistics;
    }

    /**
     * 统计指定时间范围内的文章数量
     */
    private Long countArticlesByDateRange(LocalDateTime start, LocalDateTime end) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .ge("createTime", start)
                .le("createTime", end);
        return articleMapper.selectCountByQuery(queryWrapper);
    }

    /**
     * 缁熻鎬绘枃绔犳暟閲?
     */
    private Long countTotalArticles() {
        return articleMapper.selectCountByQuery(QueryWrapper.create());
    }

    /**
     * 璁＄畻鎴愬姛鐜?
     */
    private Double calculateSuccessRate() {
        Long totalCount = countTotalArticles();
        if (totalCount == 0) {
            return 0.0;
        }

        QueryWrapper successWrapper = QueryWrapper.create()
                .eq("status", ArticleStatusEnum.COMPLETED.getValue());
        Long successCount = articleMapper.selectCountByQuery(successWrapper);

        return (successCount.doubleValue() / totalCount.doubleValue()) * 100;
    }

    /**
     * 璁＄畻骞冲潎鑰楁椂锛堜粠鍒涘缓鍒板畬鎴愮殑骞冲潎鏃堕棿锛?
     */
    private Integer calculateAvgDuration() {
        // 鏌ヨ鎵€鏈夊凡瀹屾垚鐨勬枃绔狅紝璁＄畻 createTime 鍒?completedTime 鐨勫钩鍧囪€楁椂
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", ArticleStatusEnum.COMPLETED.getValue())
                .isNotNull("completedTime");
        
        try {
            List<Article> completedArticles = articleMapper.selectListByQuery(queryWrapper);
            if (completedArticles == null || completedArticles.isEmpty()) {
                return 0;
            }

            // 璁＄畻姣忕瘒鏂囩珷鐨勮€楁椂锛堟绉掞級
            double avgDuration = completedArticles.stream()
                    .filter(article -> article.getCreateTime() != null && article.getCompletedTime() != null)
                    .mapToLong(article -> {
                        long createMillis = java.sql.Timestamp.valueOf(article.getCreateTime()).getTime();
                        long completedMillis = java.sql.Timestamp.valueOf(article.getCompletedTime()).getTime();
                        return completedMillis - createMillis;
                    })
                    .average()
                    .orElse(0.0);

            return (int) avgDuration;
        } catch (Exception e) {
            log.warn("璁＄畻骞冲潎鑰楁椂澶辫触", e);
        }
        
        return 0;
    }

    /**
     * 缁熻娲昏穬鐢ㄦ埛鏁帮紙鏈懆鏈夊垱浣滅殑鐢ㄦ埛锛?
     */
    private Long countActiveUsers(LocalDateTime weekStart) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .ge("createTime", weekStart);
        
        try {
            List<Article> articles = articleMapper.selectListByQuery(queryWrapper);
            // 缁熻鍘婚噸鍚庣殑鐢ㄦ埛鏁?
            return articles.stream()
                    .map(Article::getUserId)
                    .distinct()
                    .count();
        } catch (Exception e) {
            log.warn("缁熻娲昏穬鐢ㄦ埛澶辫触", e);
        }
        
        return 0L;
    }

    /**
     * 缁熻鎬荤敤鎴锋暟
     */
    private Long countTotalUsers() {
        return userMapper.selectCountByQuery(QueryWrapper.create());
    }

    /**
     * 缁熻 VIP 鐢ㄦ埛鏁?
     */
    private Long countVipUsers() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userRole", UserConstant.VIP_ROLE);
        return userMapper.selectCountByQuery(queryWrapper);
    }

    /** 缁熻鏅€氱敤鎴锋暟閲忥紙閰嶉缁熻鍙ｅ緞锛?*/
    private Long countNormalUsers() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userRole", UserConstant.DEFAULT_ROLE);
        return userMapper.selectCountByQuery(queryWrapper);
    }

    /**
     * 璁＄畻閰嶉浣跨敤閲?
     */
    private Long calculateQuotaUsed() {
        // 閰嶉浣跨敤閲?= (鏅€氱敤鎴锋暟 * 鍒濆閰嶉) - 褰撳墠鍓╀綑閰嶉鎬诲拰
        QueryWrapper normalUserWrapper = QueryWrapper.create()
                .eq("userRole", UserConstant.DEFAULT_ROLE);
        
        try {
            List<User> normalUsers = userMapper.selectListByQuery(normalUserWrapper);
            Long normalUserCount = (long) normalUsers.size();
            
            // 缁熻鍓╀綑閰嶉鎬诲拰
            long remainingQuota = normalUsers.stream()
                    .mapToInt(user -> user.getQuota() != null ? user.getQuota() : 0)
                    .sum();
            
            return (normalUserCount * UserConstant.DEFAULT_QUOTA) - remainingQuota;
        } catch (Exception e) {
            log.warn("璁＄畻閰嶉浣跨敤閲忓け璐?, e);
        }
        
        return 0L;
    }

    /**
     * 鑾峰彇浠婂ぉ寮€濮嬫椂闂?
     */
    private LocalDateTime getTodayStart() {
        return LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
    }

    /**
     * 鑾峰彇鏈懆寮€濮嬫椂闂达紙鍛ㄤ竴锛?
     */
    private LocalDateTime getWeekStart() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return LocalDateTime.of(monday, LocalTime.MIN);
    }

    /**
     * 鑾峰彇鏈湀寮€濮嬫椂闂?
     */
    private LocalDateTime getMonthStart() {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        return LocalDateTime.of(firstDay, LocalTime.MIN);
    }
}
