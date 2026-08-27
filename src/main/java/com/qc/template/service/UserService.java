package com.qc.template.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.qc.template.model.dto.user.UserQueryRequest;
import com.qc.template.model.dto.user.UserProfileUpdateRequest;
import com.qc.template.model.dto.user.UserRegisterRequest;
import com.qc.template.model.entity.User;
import com.qc.template.model.vo.LoginUserVO;
import com.qc.template.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 鐢ㄦ埛 鏈嶅姟灞傘€?
 *
 */
public interface UserService extends IService<User> {

    /**
     * 鐢ㄦ埛娉ㄥ唽
     *
     * @param userAccount   鐢ㄦ埛璐︽埛
     * @param userPassword  鐢ㄦ埛瀵嗙爜
     * @param checkPassword 鏍￠獙瀵嗙爜
     * @return 鏂扮敤鎴?id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /** 浣跨敤娉ㄥ唽璧勬枡鍒涘缓鐢ㄦ埛銆?*/
    long userRegister(UserRegisterRequest request);

    /** 鏇存柊褰撳墠鐧诲綍鐢ㄦ埛璧勬枡骞惰繑鍥炴渶鏂拌劚鏁忎俊鎭€?*/
    LoginUserVO updateCurrentProfile(UserProfileUpdateRequest request, User currentUser);

    /**
     * 鑾峰彇鑴辨晱鐨勫凡鐧诲綍鐢ㄦ埛淇℃伅
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 鐢ㄦ埛鐧诲綍
     *
     * @param userAccount  鐢ㄦ埛璐︽埛
     * @param userPassword 鐢ㄦ埛瀵嗙爜
     * @param request
     * @return 鑴辨晱鍚庣殑鐢ㄦ埛淇℃伅
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 鑾峰彇鑴辨晱鍚庣殑鐢ㄦ埛淇℃伅
     *
     * @param user 鐢ㄦ埛淇℃伅
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 鑾峰彇鑴辨晱鍚庣殑鐢ㄦ埛淇℃伅锛堝垎椤碉級
     *
     * @param userList 鐢ㄦ埛鍒楄〃
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 鐢ㄦ埛娉ㄩ攢
     *
     * @param request
     * @return 閫€鍑虹櫥褰曟槸鍚︽垚鍔?
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 鏍规嵁鏌ヨ鏉′欢鏋勯€犳暟鎹煡璇㈠弬鏁?
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 鍔犲瘑
     *
     * @param userPassword 鐢ㄦ埛瀵嗙爜
     * @return 鍔犲瘑鍚庣殑鐢ㄦ埛瀵嗙爜
     */
    String getEncryptPassword(String userPassword);
}
