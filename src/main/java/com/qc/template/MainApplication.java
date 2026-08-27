package com.qc.template;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 主类（项目启动入口）
 *
 */
@SpringBootApplication
@MapperScan("com.qc.template.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class MainApplication {

    public static void main(String[] args) {
        System.out.println();
        SpringApplication.run(MainApplication.class, args);
    }
}
