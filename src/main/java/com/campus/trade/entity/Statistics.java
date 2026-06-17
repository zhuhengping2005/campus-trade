package com.campus.trade.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Statistics {
    private Double todayAmount;      // 今日成交额
    private Double weekAmount;       // 本周成交额
    private Double monthAmount;      // 本月成交额
    private Double totalAmount;      // 总成交额
    private Integer pendingCount;    // 待审核商品数
    private Integer totalUsers;      // 总用户数
    private Integer totalProducts;   // 总商品数
    private Integer totalOrders;     // 总订单数
    
    // 新增：通知统计
    private Integer totalNotifications;   // 总通知数
    private Integer unreadNotifications;  // 未读通知数
    
    // 新增：订单统计
    private Integer todayOrders;         // 今日订单数
    private Integer monthOrders;         // 本月订单数
    private Integer pendingOrders;       // 待处理订单数
}
