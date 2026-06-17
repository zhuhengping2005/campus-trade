package com.campus.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.trade.entity.User;

public interface UserService extends IService<User> {
    User register(String username, String password, String email);
    String login(String username, String password);
    User getUserInfo(Long userId);
    User getUserByUsername(String username);
}
