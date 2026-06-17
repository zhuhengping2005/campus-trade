package com.campus.trade.service;

import com.campus.trade.entity.Order;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderService extends IService<Order> {
    /**
     * 创建订单（仅创建，不扣库存）
     */
    Order createOrder(Long productId, Long buyerId);
    
    /**
     * 支付成功回调处理
     */
    void paySuccess(Long orderId);
    
    /**
     * 取消订单
     */
    void cancelOrder(Long orderId, Long userId);
    
    /**
     * 获取用户订单列表
     */
    List<Order> getUserOrders(Long userId);
}
