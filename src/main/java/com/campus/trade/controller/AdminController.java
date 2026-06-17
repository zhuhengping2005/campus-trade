package com.campus.trade.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.entity.*;
import com.campus.trade.mapper.*;
import com.campus.trade.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminMapper adminMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        
        String username = params.get("username");
        String password = params.get("password");
        
        if (username == null || password == null) {
            result.put("success", false);
            result.put("message", "用户名和密码不能为空");
            return result;
        }
        
        QueryWrapper<Admin> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        wrapper.eq("status", 1);
        Admin admin = adminMapper.selectOne(wrapper);
        
        if (admin == null) {
            result.put("success", false);
            result.put("message", "管理员不存在");
            return result;
        }
        
        // 简单密码验证
        if (!password.equals(admin.getPassword())) {
            result.put("success", false);
            result.put("message", "密码错误");
            return result;
        }
        
        // 生成token
        String token = JwtUtil.generateToken(admin.getId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        data.put("token", token);
        
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        Statistics stats = new Statistics();
        
        // 待审核商品数
        QueryWrapper<Product> auditWrapper = new QueryWrapper<>();
        auditWrapper.eq("audit_status", 0);
        stats.setPendingCount(productMapper.selectCount(auditWrapper).intValue());
        
        // 总数
        stats.setTotalUsers(userMapper.selectCount(null).intValue());
        stats.setTotalProducts(productMapper.selectCount(null).intValue());
        stats.setTotalOrders(orderMapper.selectCount(null).intValue());
        
        // 成交额统计
        List<Order> allOrders = orderMapper.selectList(null);
        double total = 0;
        double today = 0;
        double week = 0;
        double month = 0;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDate todayDate = now.toLocalDate();
        LocalDate weekStart = todayDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate monthStart = todayDate.withDayOfMonth(1);
        
        for (Order order : allOrders) {
            BigDecimal price = order.getTotalPrice();
            if (price == null) {
                price = order.getAmount();
            }
            double orderPrice = price != null ? price.doubleValue() : 0;
            total += orderPrice;
            
            if (order.getCreateTime() != null) {
                LocalDate orderDate = order.getCreateTime().toLocalDate();
                if (orderDate.equals(todayDate)) today += orderPrice;
                if (!orderDate.isBefore(weekStart)) week += orderPrice;
                if (!orderDate.isBefore(monthStart)) month += orderPrice;
            }
        }
        
        stats.setTotalAmount(total);
        stats.setTodayAmount(today);
        stats.setWeekAmount(week);
        stats.setMonthAmount(month);
        
        // 通知统计
        stats.setTotalNotifications(notificationMapper.selectCount(null).intValue());
        QueryWrapper<Notification> unreadWrapper = new QueryWrapper<>();
        unreadWrapper.eq("is_read", 0);
        stats.setUnreadNotifications(notificationMapper.selectCount(unreadWrapper).intValue());
        
        // 订单数量统计
        stats.setTodayOrders(countOrdersByDate(todayDate, allOrders));
        stats.setMonthOrders(countOrdersByDateRange(monthStart, todayDate, allOrders));
        
        // 待处理订单数（假设状态为pending的为待处理）
        stats.setPendingOrders(countPendingOrders());
        
        result.put("success", true);
        result.put("data", stats);
        return result;
    }
    
    private int countOrdersByDate(LocalDate date, List<Order> allOrders) {
        int count = 0;
        for (Order order : allOrders) {
            if (order.getCreateTime() != null && order.getCreateTime().toLocalDate().equals(date)) {
                count++;
            }
        }
        return count;
    }
    
    private int countOrdersByDateRange(LocalDate start, LocalDate end, List<Order> allOrders) {
        int count = 0;
        for (Order order : allOrders) {
            if (order.getCreateTime() != null) {
                LocalDate orderDate = order.getCreateTime().toLocalDate();
                if (!orderDate.isBefore(start) && !orderDate.isAfter(end)) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private int countPendingOrders() {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "pending");
        return orderMapper.selectCount(wrapper).intValue();
    }

    /**
     * 发送通知给用户
     */
    @PostMapping("/notification/send")
    public Map<String, Object> sendNotification(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        Long userId = Long.valueOf(params.get("userId").toString());
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String type = (String) params.getOrDefault("type", "system");
        
        if (userId == null || title == null || title.isEmpty()) {
            result.put("success", false);
            result.put("message", "用户ID和通知标题不能为空");
            return result;
        }
        
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content != null ? content : "");
        notification.setType(type);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        
        notificationMapper.insert(notification);
        
        result.put("success", true);
        result.put("message", "通知发送成功");
        return result;
    }

    /**
     * 获取所有通知记录
     */
    @GetMapping("/notifications")
    public Map<String, Object> getAllNotifications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer isRead) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        
        if (userId != null) wrapper.eq("user_id", userId);
        if (isRead != null) wrapper.eq("is_read", isRead);
        
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
     * 待审核商品列表
     */
    @GetMapping("/products/pending")
    public Map<String, Object> getPendingProducts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("audit_status", 0);
        wrapper.orderByDesc("create_time");
        
        Page<Product> page = new Page<>(pageNum, pageSize);
        Page<Product> pageResult = productMapper.selectPage(page, wrapper);
        
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
     * 所有商品列表（管理员视角）
     */
    @GetMapping("/products")
    public Map<String, Object> getAllProducts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditStatus) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("priority", "create_time");
        
        if (status != null) { if (status == 0) { wrapper.in("status", Arrays.asList(0, -1)); wrapper.ne("audit_status", 0); } else { wrapper.eq("status", status); } }
        if (auditStatus != null) wrapper.eq("audit_status", auditStatus);
        
        Page<Product> page = new Page<>(pageNum, pageSize);
        Page<Product> pageResult = productMapper.selectPage(page, wrapper);
        
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
     * 审核商品
     */
    @PostMapping("/product/audit")
    public Map<String, Object> auditProduct(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = (String) params.getOrDefault("remark", "");
        
        Product product = productMapper.selectById(productId);
        if (product == null) {
            result.put("success", false);
            result.put("message", "商品不存在");
            return result;
        }
        
        product.setAuditStatus(status);
        product.setAuditRemark(remark);
        product.setAuditTime(LocalDateTime.now());
        
        // 审核通过才设置上架状态
        if (status == 1) {
            product.setStatus(1);
        } else {
            product.setStatus(-1);
        }
        
        productMapper.updateById(product);
        
        // 发送审核通知给用户
        String title = status == 1 ? "商品审核通过" : "商品审核未通过";
        String content = status == 1 
            ? "您发布的商品「" + product.getTitle() + "」已审核通过，现在可以在平台上展示了。" 
            : "您发布的商品「" + product.getTitle() + "」未通过审核，原因：" + remark;
        
        Notification notification = new Notification();
        notification.setUserId(product.getSellerId());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("audit");
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        
        result.put("success", true);
        result.put("message", "审核完成");
        return result;
    }

    /**
     * 置顶商品
     */
    @PostMapping("/product/top")
    public Map<String, Object> topProduct(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        Long productId = Long.valueOf(params.get("productId").toString());
        Integer priority = Integer.valueOf(params.get("priority").toString());
        
        Product product = productMapper.selectById(productId);
        if (product == null) {
            result.put("success", false);
            result.put("message", "商品不存在");
            return result;
        }
        
        product.setPriority(priority);
        productMapper.updateById(product);
        result.put("success", true);
        result.put("message", "置顶成功");
        return result;
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/product/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        productMapper.deleteById(id);
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public Map<String, Object> getUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> pageResult = userMapper.selectPage(page, null);
        
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
     * 获取用户发布的商品
     */
    @GetMapping("/user/{userId}/products")
    public Map<String, Object> getUserProducts(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("seller_id", userId);
        wrapper.orderByDesc("create_time");
        List<Product> products = productMapper.selectList(wrapper);
        
        result.put("success", true);
        result.put("data", products);
        return result;
    }

    /**
     * 禁用/启用用户
     */
    @PostMapping("/user/status")
    public Map<String, Object> updateUserStatus(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        
        user.setStatus(status);
        userMapper.updateById(user);
        result.put("success", true);
        result.put("message", status == 1 ? "已启用" : "已禁用");
        return result;
    }
}
