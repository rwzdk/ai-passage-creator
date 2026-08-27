package com.qc.template.service;

import com.qc.template.model.entity.User;

/**
 * 閰嶉鏈嶅姟鎺ュ彛
 *
 */
public interface QuotaService {

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁瓒冲鐨勯厤棰?
     *
     * @param user 鐢ㄦ埛
     * @return 鏄惁鏈夐厤棰?
     */
    boolean hasQuota(User user);

    /**
     * 娑堣€楅厤棰濓紙鎵ｅ噺1娆★級
     *
     * @param user 鐢ㄦ埛
     */
    void consumeQuota(User user);

    /**
     * 妫€鏌ュ苟娑堣€楅厤棰濓紙鍘熷瓙鎿嶄綔锛?
     * 濡傛灉閰嶉涓嶈冻浼氭姏鍑哄紓甯?
     *
     * @param user 鐢ㄦ埛
     */
    void checkAndConsumeQuota(User user);
}
