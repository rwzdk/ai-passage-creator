package com.qc.template.model.enums;

import lombok.Getter;

/**
 * SSE 娑堟伅绫诲瀷鏋氫妇
 *
 */
@Getter
public enum SseMessageTypeEnum {

    /**
     * 鏅鸿兘浣?瀹屾垚锛堢敓鎴愭爣棰樻柟妗堬級
     */
    AGENT1_COMPLETE("AGENT1_COMPLETE", "鏍囬鏂规鐢熸垚瀹屾垚"),

    AGENT1_STREAMING("AGENT1_STREAMING", "标题方案流式输出"),
    
    /**
     * 鏍囬鏂规鐢熸垚瀹屾垚锛堢瓑寰呯敤鎴烽€夋嫨锛?
     */
    TITLES_GENERATED("TITLES_GENERATED", "鏍囬鏂规宸茬敓鎴?),

    /**
     * 鏅鸿兘浣?娴佸紡杈撳嚭锛堝ぇ绾诧級
     */
    AGENT2_STREAMING("AGENT2_STREAMING", "大纲流式输出"),

    /**
     * 鏅鸿兘浣?瀹屾垚锛堢敓鎴愬ぇ绾诧級
     */
    AGENT2_COMPLETE("AGENT2_COMPLETE", "澶х翰鐢熸垚瀹屾垚"),
    
    /**
     * 大纲生成完成（等待用户编辑）
     */
    OUTLINE_GENERATED("OUTLINE_GENERATED", "澶х翰宸茬敓鎴?),

    /**
     * 鏅鸿兘浣?娴佸紡杈撳嚭锛堟鏂囷級
     */
    AGENT3_STREAMING("AGENT3_STREAMING", "正文流式输出"),

    /**
     * 鏅鸿兘浣?瀹屾垚锛堢敓鎴愭鏂囷級
     */
    AGENT3_COMPLETE("AGENT3_COMPLETE", "姝ｆ枃鐢熸垚瀹屾垚"),

    AGENT4_START("AGENT4_START", "寮€濮嬪垎鏋愰厤鍥鹃渶姹?),

    /**
     * 鏅鸿兘浣?瀹屾垚锛堝垎鏋愰厤鍥鹃渶姹傦級
     */
    AGENT4_COMPLETE("AGENT4_COMPLETE", "閰嶅浘闇€姹傚垎鏋愬畬鎴?),

    /**
     * 鍗曞紶閰嶅浘瀹屾垚
     */
    IMAGE_COMPLETE("IMAGE_COMPLETE", "鍗曞紶閰嶅浘瀹屾垚"),

    IMAGE_START("IMAGE_START", "寮€濮嬬敓鎴愬崟寮犻厤鍥?),

    IMAGE_FAILED("IMAGE_FAILED", "鍗曞紶閰嶅浘鐢熸垚澶辫触"),

    IMAGE_SKIPPED("IMAGE_SKIPPED", "鍗曞紶閰嶅浘鏈彃鍏ユ鏂?),

    /**
     * 鏅鸿兘浣?瀹屾垚锛堢敓鎴愰厤鍥撅級
     */
    AGENT5_COMPLETE("AGENT5_COMPLETE", "閰嶅浘鐢熸垚瀹屾垚"),

    MERGE_START("MERGE_START", "寮€濮嬪浘鏂囧悎鎴?),

    /**
     * 鍥炬枃鍚堟垚瀹屾垚
     */
    MERGE_COMPLETE("MERGE_COMPLETE", "鍥炬枃鍚堟垚瀹屾垚"),

    /**
     * 鍏ㄩ儴瀹屾垚
     */
    ALL_COMPLETE("ALL_COMPLETE", "鍏ㄩ儴瀹屾垚"),

    /**
     * 閿欒
     */
    ERROR("ERROR", "閿欒");

    /**
     * 娑堟伅绫诲瀷鍊?
     */
    private final String value;

    /**
     * 娑堟伅绫诲瀷鎻忚堪
     */
    private final String description;

    SseMessageTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 获取流式输出消息前缀
     * 用于构建带数据的流式消息，如 "AGENT2_STREAMING:内容"
     *
     * @return 消息前缀（带冒号）
     */
    public String getStreamingPrefix() {
        return this.value + ":";
    }
}
