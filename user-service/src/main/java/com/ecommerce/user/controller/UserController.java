package com.ecommerce.user.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import com.ecommerce.user.vo.LoginVO;
import com.ecommerce.user.vo.RegisterVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterVO registerVO) {
        userService.register(registerVO);
        return Result.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginVO loginVO) {
        String token = userService.login(loginVO);
        return Result.success(token);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{userId}")
    public Result<User> getUserInfo(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        // 清除敏感信息
        user.setPassword(null);
        return Result.success(user);
    }
}



