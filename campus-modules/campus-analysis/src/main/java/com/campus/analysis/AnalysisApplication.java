package com.campus.analysis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据分析服务启动类
 *
 * @author campus-mart
 */
@SpringBootApplication(scanBasePackages = "com.campus")
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.campus.analysis.mapper")
public class AnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalysisApplication.class, args);
        System.out.println("""
                ----------------------------------------------------------
                  CampusMart 分析服务启动成功  端口 9203
                  接口文档 http://localhost:9203/doc.html
                ----------------------------------------------------------""");
    }
}
