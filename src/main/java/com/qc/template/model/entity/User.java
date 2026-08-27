package com.qc.template.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 鐢ㄦ埛瀹炰綋绫?
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "user", camelToUnderline = false)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 璐﹀彿
     */
    private String userAccount;

    /**
     * 瀵嗙爜
     */
    private String userPassword;

    /**
     * 鐢ㄦ埛鏄电О
     */
    private String userName;

    /**
     * 鐢ㄦ埛澶村儚
     */
    private String userAvatar;

    /**
     * 鐢ㄦ埛绠€浠?
     */
    private String userProfile;

    /** QQ 閭 */
    private String userEmail;

    /** 鑱旂郴鐢佃瘽 */
    private String userPhone;

    /** 涓汉鍗氬鍦板潃 */
    private String userBlog;

    /** GitHub 鍦板潃 */
    private String userGithub;

    /**
     * 鐢ㄦ埛瑙掕壊锛歶ser/admin
     */
    private String userRole;

    /**
     * 鍓╀綑閰嶉
     */
    private Integer quota;

    /**
     * 鎴愪负浼氬憳鏃堕棿
     */
    private LocalDateTime vipTime;

    /**
     * 缂栬緫鏃堕棿
     */
    private LocalDateTime editTime;

    /**
     * 鍒涘缓鏃堕棿
     */
    private LocalDateTime createTime;

    /**
     * 鏇存柊鏃堕棿
     */
    private LocalDateTime updateTime;

    /**
     * 鏄惁鍒犻櫎
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

}
