package com.campus.trade.controller;

import com.campus.trade.entity.User;
import com.campus.trade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.register(
                params.get("username"),
                params.get("password"),
                params.get("email")
            );
            result.put("success", true);
            result.put("data", user);
            // 不返回密码
            user.setPassword(null);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String token = userService.login(params.get("username"), params.get("password"));
            
            // 获取用户信息
            User user = userService.getUserByUsername(params.get("username"));
            user.setPassword(null);  // 不返回密码
            
            result.put("success", true);
            result.put("token", token);
            result.put("data", user);  // 同时返回用户信息
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // 根据ID获取用户信息
    @GetMapping("/user/{id}")
    public Map<String, Object> getUserInfo(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserInfo(id);
            if (user != null) {
                user.setPassword(null);
            }
            result.put("success", true);
            result.put("data", user);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
