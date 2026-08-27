package com.qc.template.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.qc.template.mapper.AgentLogMapper;
import com.qc.template.model.entity.AgentLog;
import com.qc.template.model.vo.AgentExecutionStats;
import com.qc.template.service.AgentLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 鏅鸿兘浣撴棩蹇楁湇鍔″疄鐜?
 *
 */
@Service
@Slf4j
public class AgentLogServiceImpl extends ServiceImpl<AgentLogMapper, AgentLog> implements AgentLogService {

    @Override
    @Async
    public void saveLogAsync(AgentLog agentLog) {
        try {
            this.save(agentLog);
            log.info("鏅鸿兘浣撴棩蹇楀凡淇濆瓨, taskId={}, agentName={}, status={}, durationMs={}", 
                    agentLog.getTaskId(), agentLog.getAgentName(), agentLog.getStatus(), agentLog.getDurationMs());
        } catch (Exception e) {
            log.error("淇濆瓨鏅鸿兘浣撴棩蹇楀け璐? taskId={}, agentName={}", 
                    agentLog.getTaskId(), agentLog.getAgentName(), e);
        }
    }

    @Override
    public List<AgentLog> getLogsByTaskId(String taskId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("taskId", taskId)
                .orderBy("createTime", true);
        return this.list(queryWrapper);
    }

    @Override
    public AgentExecutionStats getExecutionStats(String taskId) {
        List<AgentLog> logs = getLogsByTaskId(taskId);
        
        if (logs == null || logs.isEmpty()) {
            return AgentExecutionStats.builder()
                    .taskId(taskId)
                    .agentCount(0)
                    .totalDurationMs(0)
                    .overallStatus("NOT_FOUND")
                    .build();
        }

        // 璁＄畻缁熻鏁版嵁
        int totalDuration = 0;
        Map<String, Integer> agentDurations = new HashMap<>();
        String overallStatus = "SUCCESS";

        for (AgentLog log : logs) {
            // SSE 浜嬩欢鏃ュ織鐢ㄤ簬鍘嗗彶鍥炴斁锛屼笉璁″叆闃舵缁熻銆?            if (log.getAgentName() != null && log.getAgentName().startsWith("__event_")) {
                continue;
            }
            // 绱姞鎬昏€楁椂
            if (log.getDurationMs() != null) {
                totalDuration += log.getDurationMs();
                agentDurations.put(log.getAgentName(), log.getDurationMs());
            }

            // 鍒ゆ柇鎬讳綋鐘舵€?
            if ("FAILED".equals(log.getStatus())) {
                overallStatus = "FAILED";
            } else if ("RUNNING".equals(log.getStatus()) && !"FAILED".equals(overallStatus)) {
                overallStatus = "RUNNING";
            }
        }

        return AgentExecutionStats.builder()
                .taskId(taskId)
                .totalDurationMs(totalDuration)
                .agentCount(logs.size())
                .agentDurations(agentDurations)
                .overallStatus(overallStatus)
                .logs(logs)
                .build();
    }
}
