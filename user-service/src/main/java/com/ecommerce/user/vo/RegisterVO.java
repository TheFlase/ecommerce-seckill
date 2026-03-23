package com.ecommerce.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册VO
 */
@Data
public class RegisterVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String phone;
    private String email;
    private String nickname;
}



