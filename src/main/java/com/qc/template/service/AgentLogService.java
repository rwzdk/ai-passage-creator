package com.qc.template.service;

import com.mybatisflex.core.service.IService;
import com.qc.template.model.entity.AgentLog;
import com.qc.template.model.vo.AgentExecutionStats;

import java.util.List;

/**
 * 智能体日志服务
 *
 */
public interface AgentLogService extends IService<AgentLog> {

    /**
     * 异步保存日志
     *
     * @param log 日志对象
     */
    void saveLogAsync(AgentLog log);

    /**
     * 根据任务 ID 获取所有日志
     *
     * @param taskId 任务ID
     * @return 日志列表
     */
    List<AgentLog> getLogsByTaskId(String taskId);

    /**
     * 获取任务执行统计信息
     *
     * @param taskId 任务ID
     * @return 执行统计
     */
    AgentExecutionStats getExecutionStats(String taskId);
}
