package com.campus.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动类
 *
 * @author campus-mart
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("""
                ----------------------------------------------------------
                  CampusMart 网关启动成功  http://localhost:8080
                  注册中心 Nacos          http://localhost:8848/nacos
                ----------------------------------------------------------""");
    }
}
