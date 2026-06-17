package com.campus.trade.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.entity.Notification;
import com.campus.trade.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 获取用户通知列表
     */
    @GetMapping("/notifications")
    public Map<String, Object> getNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        
        Page<Notification> page = new Page<>(pageNum, pageSize);
        Page<Notification> pageResult = notificationMapper.selectPage(page, wrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("pages", pageResult.getPages());
        data.put("current", pageResult.getCurrent());
        data.put("size", pageResult.getSize());
        
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    /**
     * 获取用户未读通知数量
     */
    @GetMapping("/notifications/unread-count")
    public Map<String, Object> getUnreadCount(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("is_read", 0);
        long count = notificationMapper.selectCount(wrapper);
        
        result.put("success", true);
        result.put("data", count);
        return result;
    }

    /**
     * 标记通知已读
     */
    @PostMapping("/notification/read")
    public Map<String, Object> markAsRead(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        Long notificationId = Long.valueOf(params.get("notificationId").toString());
        Long userId = Long.valueOf(params.get("userId").toString());
        
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            result.put("success", false);
            result.put("message", "通知不存在");
            return result;
        }
        
        // 验证通知属于该用户
        if (!notification.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "无权操作");
            return result;
        }
        
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
        
        result.put("success", true);
        result.put("message", "已标记为已读");
        return result;
    }

    /**
     * 标记所有通知已读
     */
    @PostMapping("/notifications/read-all")
    public Map<String, Object> markAllAsRead(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        Long userId = Long.valueOf(params.get("userId").toString());
        
        Notification notification = new Notification();
        notification.setIsRead(1);
        
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("is_read", 0);
        
        notificationMapper.update(notification, wrapper);
        
        result.put("success", true);
        result.put("message", "已全部标记为已读");
        return result;
    }
}
