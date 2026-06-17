package com.campus.trade.controller;

import com.campus.trade.entity.Order;
import com.campus.trade.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderService.createOrder(params.get("productId"), params.get("buyerId"));
            result.put("success", true);
            result.put("data", order);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 支付成功回调 - 核心修复点
     * 前端支付完成后调用此接口
     */
    @PostMapping("/paySuccess")
    public Map<String, Object> paySuccess(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long orderId = params.get("orderId");
            orderService.paySuccess(orderId);
            result.put("success", true);
            result.put("message", "支付成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 支付回调 - 兼容第三方支付回调
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从回调参数中获取订单ID
            Long orderId = Long.valueOf(params.get("orderId").toString());
            orderService.paySuccess(orderId);
            result.put("success", true);
            result.put("message", "回调处理成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public Map<String, Object> cancel(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long orderId = params.get("orderId");
            Long userId = params.get("userId");
            orderService.cancelOrder(orderId, userId);
            result.put("success", true);
            result.put("message", "订单已取消");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("data", orderService.getUserOrders(userId));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderService.getById(id);
            result.put("success", order != null);
            result.put("data", order);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
