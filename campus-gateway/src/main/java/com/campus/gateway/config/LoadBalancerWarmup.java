package com.campus.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 负载均衡子上下文预热。
 *
 * <p>背景：Spring Cloud Gateway 4.1.2 中，第一次命中 {@code lb://} 路由时才懒创建
 * LoadBalancer 的 {@code NamedContextFactory} 子上下文；子上下文 refresh 会发布路由刷新事件，
 * 触发 {@code WeightCalculatorWebFilter#onApplicationEvent} 里的 {@code Flux.blockLast()}。
 * 若这次懒创建发生在请求线程上，线程是 reactor-http-nio，于是抛
 * {@code IllegalStateException: block()/blockFirst()/blockLast() are blocking}，接口直接 500。</p>
 *
 * <p>解决办法：在所有单例初始化完成、Netty 开始接收请求之前（SmartInitializingSingleton 阶段）
 * 就把 5 个服务的 LB 子上下文建好，让那次阻塞事件发生在 main 线程上。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoadBalancerWarmup implements SmartInitializingSingleton {

    private static final List<String> SERVICE_IDS = List.of(
            "campus-auth", "campus-system", "campus-item", "campus-analysis", "campus-ai");

    private final LoadBalancerClientFactory loadBalancerClientFactory;

    @Override
    public void afterSingletonsInstantiated() {
        for (String serviceId : SERVICE_IDS) {
            try {
                // getInstance 会触发 NamedContextFactory.createContext（在 main 线程完成）
                loadBalancerClientFactory.getInstance(serviceId);
                log.info("[LB-WARMUP] 已预热负载均衡子上下文: {}", serviceId);
            } catch (Exception e) {
                log.warn("[LB-WARMUP] 预热失败 {}: {}", serviceId, e.getMessage());
            }
        }
    }
}
