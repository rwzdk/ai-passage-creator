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
 * 鏅鸿兘浣撴墽琛屾棩蹇楀疄浣撶被
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "agent_log", camelToUnderline = false)
public class AgentLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 浠诲姟ID
     */
    private String taskId;

    /**
     * 鏅鸿兘浣撳悕绉?
     */
    private String agentName;

    /**
     * 寮€濮嬫椂闂?
     */
    private LocalDateTime startTime;

    /**
     * 缁撴潫鏃堕棿
     */
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 鐘舵€侊細SUCCESS/FAILED
     */
    private String status;

    /**
     * 閿欒淇℃伅
     */
    private String errorMessage;

    /**
     * 浣跨敤鐨凱rompt
     */
    private String prompt;

    /**
     * 杈撳叆鏁版嵁锛圝SON鏍煎紡锛?
     */
    private String inputData;

    /**
     * 杈撳嚭鏁版嵁锛圝SON鏍煎紡锛?
     */
    private String outputData;

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
