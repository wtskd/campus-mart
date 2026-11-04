package com.campus.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证服务启动类
 *
 * <p>auth 服务不直接访问数据库，用户/菜单数据通过 OpenFeign 调用 campus-system 获取，
 * 因此排除 DataSource 自动配置。</p>
 *
 * @author campus-mart
 */
@SpringBootApplication(scanBasePackages = "com.campus", exclude = DataSourceAutoConfiguration.class)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.campus.api.feign")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("""
                ----------------------------------------------------------
                  CampusMart 认证服务启动成功  端口 9200
                  接口文档 http://localhost:9200/doc.html
                ----------------------------------------------------------""");
    }
}
