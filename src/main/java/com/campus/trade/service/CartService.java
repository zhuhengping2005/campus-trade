package com.campus.trade.service;

import com.campus.trade.entity.CartItem;
import java.util.List;

public interface CartService {
    CartItem addToCart(Long userId, Long productId);
    void removeFromCart(Long userId, Long productId);
    List<CartItem> getCartItems(Long userId);
    void clearCart(Long userId);
    void updateQuantity(Long userId, Long productId, Integer quantity);
    
    // 批量创建订单（返回成功的订单列表）
    List<Long> checkout(Long userId, List<Long> cartItemIds);
}
