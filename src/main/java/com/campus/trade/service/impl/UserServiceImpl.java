package com.campus.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.service.UserService;
import com.campus.trade.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public User register(String username, String password, String email) {
        // 1. 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (this.getOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 2. 创建用户（密码直接存储，实际项目应该加密）
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);  // 实际项目应该加密：BCrypt.hashpw(password, BCrypt.gensalt())
        user.setEmail(email);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        
        // 3. 保存到数据库
        this.save(user);
        
        return user;
    }

    @Override
    public String login(String username, String password) {
        // 1. 查询用户（用户名和密码都必须匹配）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("password", password);
        User user = this.getOne(wrapper);
        
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 2. 生成JWT Token
        String token = JwtUtil.generateToken(user.getId());
        
        // 3. Token存入Redis，24小时过期
        redisTemplate.opsForValue().set("user:token:" + user.getId(), token, 24, TimeUnit.HOURS);
        
        return token;
    }

    @Override
    public User getUserInfo(Long userId) {
        return this.getById(userId);
    }

    @Override
    public User getUserByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return this.getOne(wrapper);
    }
}
