package com.qc.template.service.impl;

import com.qc.template.exception.BusinessException;
import com.qc.template.exception.ErrorCode;
import com.qc.template.mapper.UserMapper;
import com.qc.template.model.entity.User;
import com.qc.template.service.QuotaService;
import com.qc.template.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.qc.template.constant.UserConstant.ADMIN_ROLE;
import static com.qc.template.constant.UserConstant.VIP_ROLE;

/**
 * 配额服务实现
 * 
 * 并发安全说明：
 * 1. 使用数据库原子更新（UPDATE ... SET quota = quota - 1 WHERE quota > 0）避免竞态条件。
 * 2. 通过影响行数判断操作是否成功，无需先查询再更新。
 * 3. 使用 @Transactional 确保配额扣减与后续操作的一致性。
 *
 * @author <a href="https://codefather.cn">编程导航学习网</a>
 */
@Service
@Slf4j
public class QuotaServiceImpl implements QuotaService {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean hasQuota(User user) {
        // 管理员和 VIP 用户无限配额
        if (isAdmin(user) || isVip(user)) {
            return true;
        }
        // 从数据库查询朢新配额，避免使用缓存的旧数据
        User freshUser = userService.getById(user.getId());
        if (freshUser == null) {
            return false;
        }
        Integer quota = freshUser.getQuota();
        return quota != null && quota > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeQuota(User user) {
        // Administrators and VIP users do not consume quota.
        if (isAdmin(user) || isVip(user)) {
            return;
        }

        // 使用原子更新：UPDATE user SET quota = quota - 1 WHERE id = ? AND quota > 0
        // Use the affected row count to make the decrement atomic.
        int affectedRows = userMapper.decrementQuota(user.getId());

        if (affectedRows > 0) {
            log.info("用户配额已消费: userId={}", user.getId());
        } else {
            log.warn("用户配额扣减失败（可能配额不足或并发冲突）: userId={}", user.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndConsumeQuota(User user) {
        // Administrators and VIP users skip the check.
        if (isAdmin(user) || isVip(user)) {
            return;
        }

        // 使用原子更新：检查与消费合并为一个原子操作
        // UPDATE user SET quota = quota - 1 WHERE id = ? AND quota > 0
        int affectedRows = userMapper.decrementQuota(user.getId());

        if (affectedRows == 0) {
            // 影响行数为 0，说明配额不足（或已被其他请求消费）
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Insufficient quota");
        }

        log.info("用户配额检查并消费成功: userId={}", user.getId());
    }

    /**
     * 判断是否为管理员
     */
    private boolean isAdmin(User user) {
        return ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 判断是否为 VIP
     */
    private boolean isVip(User user) {
        return VIP_ROLE.equals(user.getUserRole());
    }
}
