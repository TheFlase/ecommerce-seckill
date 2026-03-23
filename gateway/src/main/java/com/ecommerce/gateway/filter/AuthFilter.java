package com.ecommerce.gateway.filter;

import com.ecommerce.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤器
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // 白名单路径（不需要认证）
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/product/list",
            "/product/detail"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径直接放行
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        // 获取token
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            log.warn("请求路径：{} 缺少token", path);
            return unauthorized(exchange.getResponse());
        }

        // 验证token
        try {
            if (JwtUtil.isTokenExpired(token)) {
                log.warn("请求路径：{} token已过期", path);
                return unauthorized(exchange.getResponse());
            }

            // 将用户ID传递给下游服务
            Long userId = JwtUtil.getUserId(token);
            ServerHttpRequest newRequest = request.mutate()
                    .header("userId", userId.toString())
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            log.error("token验证失败：{}", e.getMessage());
            return unauthorized(exchange.getResponse());
        }
    }

    private boolean isWhitePath(String path) {
        return WHITE_LIST.stream().anyMatch(path::contains);
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}



