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
 * 閰嶉鏈嶅姟瀹炵幇
 * 
 * 骞跺彂瀹夊叏璇存槑锛?
 * 1. 浣跨敤鏁版嵁搴撳師瀛愭洿鏂帮紙UPDATE ... SET quota = quota - 1 WHERE quota > 0锛夐伩鍏嶇珵鎬佹潯浠?
 * 2. 閫氳繃褰卞搷琛屾暟鍒ゆ柇鎿嶄綔鏄惁鎴愬姛锛屾棤闇€鍏堟煡璇㈠啀鏇存柊
 * 3. 浣跨敤 @Transactional 纭繚閰嶉鎵ｅ噺涓庡悗缁搷浣滅殑涓€鑷存€?
 *
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
        // 绠＄悊鍛樺拰 VIP 鐢ㄦ埛鏃犻檺閰嶉
        if (isAdmin(user) || isVip(user)) {
            return true;
        }
        // 浠庢暟鎹簱鏌ヨ鏈€鏂伴厤棰濓紝閬垮厤浣跨敤缂撳瓨鐨勬棫鏁版嵁
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
        // 绠＄悊鍛樺拰 VIP 鐢ㄦ埛涓嶆秷鑰楅厤棰?
        if (isAdmin(user) || isVip(user)) {
            return;
        }

        // 使用原子更新：UPDATE user SET quota = quota - 1 WHERE id = ? AND quota > 0
        // 閫氳繃褰卞搷琛屾暟鍒ゆ柇鏄惁鎴愬姛锛岄伩鍏嶅苟鍙戦棶棰?
        int affectedRows = userMapper.decrementQuota(user.getId());

        if (affectedRows > 0) {
            log.info("鐢ㄦ埛閰嶉宸叉秷鑰? userId={}", user.getId());
        } else {
            log.warn("鐢ㄦ埛閰嶉鎵ｅ噺澶辫触锛堝彲鑳介厤棰濅笉瓒虫垨骞跺彂鍐茬獊锛? userId={}", user.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndConsumeQuota(User user) {
        // 绠＄悊鍛樺拰 VIP 鐢ㄦ埛璺宠繃妫€鏌?
        if (isAdmin(user) || isVip(user)) {
            return;
        }

        // 浣跨敤鍘熷瓙鏇存柊锛氭鏌ヤ笌娑堣垂鍚堝苟涓轰竴涓師瀛愭搷浣?
        // UPDATE user SET quota = quota - 1 WHERE id = ? AND quota > 0
        int affectedRows = userMapper.decrementQuota(user.getId());

        if (affectedRows == 0) {
            // 褰卞搷琛屾暟涓?锛岃鏄庨厤棰濅笉瓒筹紙宸茶鍏朵粬璇锋眰娑堣€楋級
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "閰嶉涓嶈冻锛屾棤娉曞垱寤烘枃绔?);
        }

        log.info("鐢ㄦ埛閰嶉妫€鏌ュ苟娑堣€楁垚鍔? userId={}", user.getId());
    }

    /**
     * 鍒ゆ柇鏄惁涓虹鐞嗗憳
     */
    private boolean isAdmin(User user) {
        return ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 鍒ゆ柇鏄惁涓?VIP
     */
    private boolean isVip(User user) {
        return VIP_ROLE.equals(user.getUserRole());
    }
}
