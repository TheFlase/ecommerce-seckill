package com.ecommerce.user.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.vo.LoginVO;
import com.ecommerce.user.vo.RegisterVO;

/**
 * 用户服务
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    void register(RegisterVO registerVO);

    /**
     * 用户登录
     */
    String login(LoginVO loginVO);

    /**
     * 根据ID查询用户
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);
}



