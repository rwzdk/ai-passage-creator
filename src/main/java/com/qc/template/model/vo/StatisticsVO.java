package com.qc.template.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 缁熻鏁版嵁 VO
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 浠婃棩鍒涗綔鏁伴噺
     */
    private Long todayCount;

    /**
     * 鏈懆鍒涗綔鏁伴噺
     */
    private Long weekCount;

    /**
     * 鏈湀鍒涗綔鏁伴噺
     */
    private Long monthCount;

    /**
     * 鎬诲垱浣滄暟閲?
     */
    private Long totalCount;

    /**
     * 鎴愬姛鐜囷紙鐧惧垎姣旓級
     */
    private Double successRate;

    /**
     * 平均耗时（毫秒）
     */
    private Integer avgDurationMs;

    /**
     * 娲昏穬鐢ㄦ埛鏁帮紙鏈懆锛?
     */
    private Long activeUserCount;

    /**
     * 鎬荤敤鎴锋暟
     */
    private Long totalUserCount;

    /**
     * VIP 鐢ㄦ埛鏁?
     */
    private Long vipUserCount;

    /** 鏅€氱敤鎴锋暟閲忥紙閰嶉缁熻鍙ｅ緞锛?*/
    private Long normalUserCount;

    /**
     * 閰嶉鎬讳娇鐢ㄩ噺
     */
    private Long quotaUsed;
}
