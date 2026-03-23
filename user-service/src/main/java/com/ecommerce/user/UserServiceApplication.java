package com.ecommerce.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.ecommerce.user", "com.ecommerce.common"})
@MapperScan("com.ecommerce.user.mapper")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}



