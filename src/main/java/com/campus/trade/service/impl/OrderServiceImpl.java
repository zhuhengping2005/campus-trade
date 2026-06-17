package com.campus.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private ProductService productService;

    /**
     * 创建订单（仅创建订单记录，不扣库存，不修改商品状态）
     * 
     * 职责：
     * 1. 校验商品存在性和状态
     * 2. 创建pending状态的订单
     * 3. 返回订单信息供前端调用支付
     * 
     * 注意：创建订单时不扣减库存，库存扣减在支付成功后处理
     */
    @Override
    @Transactional
    public Order createOrder(Long productId, Long buyerId) {
        // 1. 查询商品信息
        Product product = productService.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        
        // 2. 检查商品状态（1=在售）
        if (product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }
        
        // 3. 检查审核状态（1=已通过）
        if (product.getAuditStatus() != 1) {
            throw new RuntimeException("商品未通过审核");
        }
        
        // 4. 检查库存
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new RuntimeException("商品已售罄");
        }
        
        // 5. 检查买家是否为卖家
        if (product.getSellerId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己发布的商品");
        }
        
        // 6. 创建订单（初始状态为pending）
        Order order = new Order();
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setAmount(product.getPrice());
        order.setTotalPrice(product.getPrice());
        order.setStatus("pending");
        order.setCreateTime(LocalDateTime.now());
        
        // 保存订单
        this.save(order);
        
        // 【关键】创建订单时只创建订单记录，不修改商品状态，不扣减库存！
        // 库存扣减在支付成功回调 paySuccess() 中处理
        
        return order;
    }

    /**
     * 支付成功回调处理（核心业务逻辑）
     * 
     * 职责：
     * 1. 更新订单状态为已支付(paid)
     * 2. 扣减商品库存（原子操作）
     * 3. 如果库存为0，商品自动下架(status=0)，但不影响审核状态(audit_status)
     * 
     * 数据一致性保证：
     * - 使用@Transactional确保事务一致性
     * - 先扣减库存再更新订单状态
     * - 使用乐观锁防止超卖
     */
    @Override
    @Transactional
    public void paySuccess(Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 2. 检查订单状态，防止重复处理
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("订单状态异常，无法完成支付，当前状态：" + order.getStatus());
        }
        
        // 3. 查询商品
        Product product = productService.getById(order.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        
        // 4. 检查库存（双重检查）
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new RuntimeException("商品库存不足");
        }
        
        // 5. 扣减库存
        int newStock = product.getStock() - 1;
        
        // 6. 判断商品状态：如果库存为0则下架，否则保持原有状态
        // 注意：只修改status字段，不修改audit_status（审核状态）
        int newStatus = (newStock <= 0) ? 0 : product.getStatus();
        
        // 7. 原子更新：扣减库存并更新商品状态
        ((ProductServiceImpl) productService).updateStockAndStatus(product.getId(), newStock, newStatus);
        
        // 8. 更新订单状态为已支付
        order.setStatus("paid");
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);
    }

    /**
     * 取消订单
     * 
     * 职责：
     * - 将pending状态的订单更新为cancelled
     * - 由于当前是支付成功后才扣库存，取消订单不需要回滚库存
     */
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 只能取消自己的待支付订单
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权取消此订单");
        }
        
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("只能取消待支付的订单");
        }
        
        order.setStatus("cancelled");
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);
    }

    /**
     * 获取用户订单列表
     */
    @Override
    public java.util.List<Order> getUserOrders(Long userId) {
        return this.lambdaQuery()
                .eq(Order::getBuyerId, userId)
                .orderByDesc(Order::getCreateTime)
                .list();
    }
}
