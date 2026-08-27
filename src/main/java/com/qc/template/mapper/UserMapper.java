package com.qc.template.mapper;

import com.mybatisflex.core.BaseMapper;
import com.qc.template.model.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 鐢ㄦ埛 鏄犲皠灞傘€?
 *
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 鍘熷瓙鎵ｅ噺鐢ㄦ埛閰嶉
     * 浣跨敤 quota > 0 鏉′欢纭繚骞跺彂瀹夊叏锛岄伩鍏嶈秴鎵?
     *
     * @param userId 鐢ㄦ埛ID
     * @return 褰卞搷琛屾暟锛?琛ㄧず鎴愬姛锛?琛ㄧず閰嶉涓嶈冻
     */
    @Update("UPDATE user SET quota = quota - 1 WHERE id = #{userId} AND quota > 0")
    int decrementQuota(@Param("userId") Long userId);

}
