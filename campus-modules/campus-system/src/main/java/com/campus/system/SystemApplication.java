package com.campus.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 系统服务启动类：用户、角色、菜单、部门、字典、参数、日志、文件
 *
 * @author campus-mart
 */
@SpringBootApplication(scanBasePackages = "com.campus")
@EnableDiscoveryClient
@EnableAsync
@MapperScan("com.campus.system.mapper")
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
        System.out.println("""
                ----------------------------------------------------------
                  CampusMart 系统服务启动成功  端口 9201
                  接口文档 http://localhost:9201/doc.html
                ----------------------------------------------------------""");
    }
}
