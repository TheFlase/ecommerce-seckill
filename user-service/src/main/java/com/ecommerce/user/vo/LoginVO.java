package com.ecommerce.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录VO
 */
@Data
public class LoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
}



