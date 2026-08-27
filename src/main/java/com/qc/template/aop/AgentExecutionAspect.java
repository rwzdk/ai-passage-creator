package com.qc.template.aop;

import com.qc.template.annotation.AgentExecution;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.qc.template.model.dto.article.ArticleState;
import com.qc.template.model.entity.AgentLog;
import com.qc.template.service.AgentLogService;
import com.qc.template.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 鏅鸿兘浣撴墽琛?AOP 鍒囬潰
 * 鑷姩璁板綍鏅鸿兘浣撴墽琛屾棩蹇楀拰鎬ц兘鏁版嵁
 *
 */
@Aspect
@Component
@Slf4j
public class AgentExecutionAspect {

    @Resource
    private AgentLogService agentLogService;

    @Around("@annotation(agentExecution)")
    public Object aroundAgentExecution(ProceedingJoinPoint pjp, AgentExecution agentExecution) throws Throwable {
        long startTime = System.currentTimeMillis();
        LocalDateTime startDateTime = LocalDateTime.now();
        
        // 鎻愬彇 taskId 鍜岃緭鍏ユ暟鎹?
        String taskId = extractTaskId(pjp);
        String inputData = extractInputData(pjp);
        String prompt = extractPrompt(pjp);
        
        // 创建日志对象
        AgentLog agentLog = AgentLog.builder()
                .taskId(taskId)
                .agentName(agentExecution.value())
                .startTime(startDateTime)
                .status("RUNNING")
                .prompt(prompt)
                .inputData(inputData)
                .build();

        Object result = null;
        try {
            // 鎵ц鐩爣鏂规硶
            result = pjp.proceed();
            
            // 璁板綍鎴愬姛鐘舵€?
            agentLog.setStatus("SUCCESS");
            agentLog.setEndTime(LocalDateTime.now());
            agentLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            agentLog.setOutputData(extractOutputData(result));
            
            log.info("鏅鸿兘浣撴墽琛屾垚鍔? {}, taskId={}, 鑰楁椂={}ms", 
                    agentExecution.value(), taskId, agentLog.getDurationMs());
            
        } catch (Throwable e) {
            // 璁板綍澶辫触鐘舵€?
            agentLog.setStatus("FAILED");
            agentLog.setEndTime(LocalDateTime.now());
            agentLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            agentLog.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            
            log.error("鏅鸿兘浣撴墽琛屽け璐? {}, taskId={}, 閿欒={}", 
                    agentExecution.value(), taskId, e.getMessage(), e);
            
            throw e;
        } finally {
            // 寮傛淇濆瓨鏃ュ織
            agentLogService.saveLogAsync(agentLog);
        }

        return result;
    }

    /**
     * 浠庢柟娉曞弬鏁颁腑鎻愬彇 taskId
     */
    private String extractTaskId(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) {
            return "unknown";
        }

        // 浼樺厛浠?ArticleState 涓幏鍙?
        for (Object arg : args) {
            if (arg instanceof ArticleState) {
                return ((ArticleState) arg).getTaskId();
            }
            if (arg instanceof OverAllState state) {
                return state.value("taskId").map(Object::toString).orElse("unknown");
            }
        }

        // 灏濊瘯浠庣涓€涓?String 鍙傛暟鑾峰彇锛堝彲鑳芥槸 taskId锛?
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }

        return "unknown";
    }

    /**
     * 鎻愬彇杈撳叆鏁版嵁锛堢畝鍖栫増锛屽彧璁板綍鍏抽敭淇℃伅锛?
     */
    private String extractInputData(ProceedingJoinPoint pjp) {
        try {
            Object[] args = pjp.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }

            Map<String, Object> inputMap = new HashMap<>();
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            String[] paramNames = signature.getParameterNames();

            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                Object arg = args[i];
                // 鍙褰曞熀鏈被鍨嬪拰绠€鍗曞璞★紝閬垮厤鏁版嵁杩囧ぇ
                if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                    inputMap.put(paramNames[i], arg);
                } else if (arg instanceof ArticleState) {
                    ArticleState state = (ArticleState) arg;
                    inputMap.put("taskId", state.getTaskId());
                    if (state.getTitle() != null) {
                        inputMap.put("mainTitle", state.getTitle().getMainTitle());
                    }
                }
            }

            return inputMap.isEmpty() ? null : GsonUtils.toJson(inputMap);
        } catch (Exception e) {
            log.warn("鎻愬彇杈撳叆鏁版嵁澶辫触", e);
            return null;
        }
    }

    /**
     * 鎻愬彇杈撳嚭鏁版嵁锛堢畝鍖栫増锛?
     */
    private String extractOutputData(Object result) {
        try {
            if (result == null) {
                return null;
            }

            // 鍙褰曠畝鍗曠被鍨嬶紝閬垮厤鏁版嵁杩囧ぇ
            if (result instanceof String || result instanceof Number || result instanceof Boolean) {
                return String.valueOf(result);
            }

            // 对于集合类型，只记录数量
            if (result instanceof java.util.List) {
                return "{\"listSize\": " + ((java.util.List<?>) result).size() + "}";
            }

            return "{\"type\": \"" + result.getClass().getSimpleName() + "\"}";
        } catch (Exception e) {
            log.warn("鎻愬彇杈撳嚭鏁版嵁澶辫触", e);
            return null;
        }
    }

    /**
     * 鎻愬彇浣跨敤鐨?Prompt锛堝皾璇曚粠鏂规硶鍙傛暟鎴?ArticleState 鑾峰彇锛?
     */
    private String extractPrompt(ProceedingJoinPoint pjp) {
        try {
            // 鍙互鏍规嵁鏂规硶鍚嶇О鎺ㄦ柇浣跨敤鐨?Prompt
            // 鎴栦粠鍙傛暟涓彁鍙栵紝杩欓噷绠€鍖栧鐞?
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        } catch (Exception e) {
            return null;
        }
    }
}
