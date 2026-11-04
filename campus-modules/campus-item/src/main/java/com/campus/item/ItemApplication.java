package com.campus.item;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 商品交易服务启动类：商品、分类、订单、收藏、评论、举报、消息
 *
 * @author campus-mart
 */
@SpringBootApplication(scanBasePackages = "com.campus")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.campus.api.feign")
@MapperScan("com.campus.item.mapper")
public class ItemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItemApplication.class, args);
        System.out.println("""
                ----------------------------------------------------------
                  CampusMart 商品服务启动成功  端口 9202
                  接口文档 http://localhost:9202/doc.html
                ----------------------------------------------------------""");
    }
}
