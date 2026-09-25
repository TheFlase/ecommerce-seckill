package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.utils.JwtUtil;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.service.UserService;
import com.ecommerce.user.vo.LoginVO;
import com.ecommerce.user.vo.RegisterVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void register(RegisterVO registerVO) {
        User existUser = getUserByUsername(registerVO.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerVO.getUsername());
        user.setPassword(passwordEncoder.encode(registerVO.getPassword()));
        user.setPhone(registerVO.getPhone());
        user.setEmail(registerVO.getEmail());
        user.setNickname(registerVO.getNickname());
        user.setStatus(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("用户注册成功：{}", user.getUsername());
    }

    @Override
    public String login(LoginVO loginVO) {
        User user = getUserByUsername(loginVO.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(loginVO.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        if (user.getStatus() != 0) {
            throw new BusinessException("账号已被禁用");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        String userKey = RedisKeyConstant.USER_INFO_KEY + user.getId();
        stringRedisTemplate.opsForValue().set(userKey, user.getUsername(), 24, TimeUnit.HOURS);

        log.info("用户登录成功：{}", user.getUsername());
        return token;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }
}
