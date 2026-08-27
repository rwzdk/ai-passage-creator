package com.qc.template.model.vo;

import com.qc.template.model.entity.AgentLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 鏅鸿兘浣撴墽琛岀粺璁?VO
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 浠诲姟ID
     */
    private String taskId;

    /**
     * 鎬昏€楁椂锛堟绉掞級
     */
    private Integer totalDurationMs;

    /**
     * 鏅鸿兘浣撴暟閲?
     */
    private Integer agentCount;

    /**
     * 鍚勬櫤鑳戒綋鑰楁椂锛坘ey: agentName, value: durationMs锛?
     */
    private Map<String, Integer> agentDurations;

    /**
     * 鎬讳綋鐘舵€侊細SUCCESS锛堝叏閮ㄦ垚鍔燂級銆丗AILED锛堝瓨鍦ㄥけ璐ワ級銆丷UNNING锛堟墽琛屼腑锛?
     */
    private String overallStatus;

    /**
     * 璇︾粏鏃ュ織鍒楄〃
     */
    private List<AgentLog> logs;
}
