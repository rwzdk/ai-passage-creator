package com.qc.template.annotation;

import java.lang.annotation.*;

/**
 * 鏅鸿兘浣撴墽琛屾敞瑙?
 * 鐢ㄤ簬鏍囪鏅鸿兘浣撴柟娉曪紝鑷姩璁板綍鎵ц鏃ュ織鍜屾€ц兘鏁版嵁
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentExecution {
    
    /**
     * 鏅鸿兘浣撳悕绉?
     * 渚嬪: "agent1_generate_titles", "agent2_generate_outline"
     */
    String value();
    
    /**
     * 鏅鸿兘浣撴弿杩?
     */
    String description() default "";
}
