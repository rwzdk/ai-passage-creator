package com.qc.template.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 寮傛浠诲姟閰嶇疆
 *
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 鏂囩珷鐢熸垚寮傛绾跨▼姹?
     */
    @Bean(name = "articleExecutor")
    public Executor articleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 鏍稿績绾跨▼鏁?
        executor.setCorePoolSize(5);
        
        // 鏈€澶х嚎绋嬫暟
        executor.setMaxPoolSize(10);
        
        // 闃熷垪瀹归噺
        executor.setQueueCapacity(100);
        
        // 绾跨▼鍚嶇О鍓嶇紑
        executor.setThreadNamePrefix("article-async-");
        
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 绛夊緟鎵€鏈変换鍔″畬鎴愬悗鍐嶅叧闂嚎绋嬫睜
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 绛夊緟鏃堕棿
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        return executor;
    }
}
