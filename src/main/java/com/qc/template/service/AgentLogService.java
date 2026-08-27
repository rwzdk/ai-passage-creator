package com.qc.template.service;

import com.mybatisflex.core.service.IService;
import com.qc.template.model.entity.AgentLog;
import com.qc.template.model.vo.AgentExecutionStats;

import java.util.List;

/**
 * 鏅鸿兘浣撴棩蹇楁湇鍔?
 *
 */
public interface AgentLogService extends IService<AgentLog> {

    /**
     * 寮傛淇濆瓨鏃ュ織
     *
     * @param log 日志对象
     */
    void saveLogAsync(AgentLog log);

    /**
     * 鏍规嵁浠诲姟ID鑾峰彇鎵€鏈夋棩蹇?
     *
     * @param taskId 浠诲姟ID
     * @return 鏃ュ織鍒楄〃
     */
    List<AgentLog> getLogsByTaskId(String taskId);

    /**
     * 鑾峰彇浠诲姟鎵ц缁熻淇℃伅
     *
     * @param taskId 浠诲姟ID
     * @return 鎵ц缁熻
     */
    AgentExecutionStats getExecutionStats(String taskId);
}
