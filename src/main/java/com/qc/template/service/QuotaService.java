package com.qc.template.service;

import com.qc.template.model.entity.User;

/**
 * 配额服务接口
 *
 */
public interface QuotaService {

    /**
     * 检查用户是否有足够的配额
     *
     * @param user 用户
     * @return 是否有配额
     */
    boolean hasQuota(User user);

    /**
     * 消配额（扣减1娆★級
     *
     * @param user 用户
     */
    void consumeQuota(User user);

    /**
     * 检查并消费配额（原子操作）
     * 如果配额不足会抛出异常
     *
     * @param user 用户
     */
    void checkAndConsumeQuota(User user);
}
