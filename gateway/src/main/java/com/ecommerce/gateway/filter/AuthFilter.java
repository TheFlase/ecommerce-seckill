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

@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/product/list",
            "/product/detail",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            log.warn("请求路径：{} 缺少token", path);
            return unauthorized(exchange.getResponse());
        }

        try {
            if (JwtUtil.isTokenExpired(token)) {
                log.warn("请求路径：{} token已过期", path);
                return unauthorized(exchange.getResponse());
            }

            Long userId = JwtUtil.getUserId(token);
            String username = JwtUtil.getUsername(token);

            // 预热接口仅管理员
            if (path.contains("/seckill/warm-up") && !"admin".equals(username)) {
                log.warn("非管理员尝试预热：{}", username);
                return forbidden(exchange.getResponse());
            }

            ServerHttpRequest newRequest = request.mutate()
                    .header("userId", userId.toString())
                    .header("username", username)
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

    private Mono<Void> forbidden(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
