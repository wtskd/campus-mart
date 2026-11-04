package com.campus.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 服务启动类：文案生成、智能客服、推荐、内容审核
 *
 * @author campus-mart
 */
@SpringBootApplication(scanBasePackages = "com.campus")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.campus.api.feign")
@MapperScan("com.campus.ai.mapper")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
        System.out.println("""
                ----------------------------------------------------------
                  CampusMart AI 服务启动成功  端口 9204
                  接口文档 http://localhost:9204/doc.html
                ----------------------------------------------------------""");
    }
}
