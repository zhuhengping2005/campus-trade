package com.campus.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.entity.CartItem;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.CartItemMapper;
import com.campus.trade.service.CartService;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl extends ServiceImpl<CartItemMapper, CartItem> implements CartService {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;

    @Override
    public CartItem addToCart(Long userId, Long productId) {
        Product product = productService.getById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new RuntimeException("商品不存在或已下架");
        }
        
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new RuntimeException("商品已售罄");
        }
        
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        CartItem existItem = this.getOne(wrapper);
        
        if (existItem != null) {
            // 检查是否超过库存
            if (existItem.getQuantity() >= product.getStock()) {
                throw new RuntimeException("已达库存上限");
            }
            existItem.setQuantity(existItem.getQuantity() + 1);
            this.updateById(existItem);
            existItem.setProduct(product);
            return existItem;
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(1);
            cartItem.setCreateTime(LocalDateTime.now());
            this.save(cartItem);
            cartItem.setProduct(product);
            return cartItem;
        }
    }

    @Override
    public void removeFromCart(Long userId, Long productId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        this.remove(wrapper);
    }

    @Override
    public List<CartItem> getCartItems(Long userId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        List<CartItem> items = this.list(wrapper);
        
        for (CartItem item : items) {
            Product product = productService.getById(item.getProductId());
            item.setProduct(product);
        }
        
        items.removeIf(item -> item.getProduct() == null || item.getProduct().getStatus() != 1);
        
        return items;
    }

    @Override
    public void clearCart(Long userId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        this.remove(wrapper);
    }

    @Override
    public void updateQuantity(Long userId, Long productId, Integer quantity) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        CartItem item = this.getOne(wrapper);
        if (item != null) {
            if (quantity <= 0) {
                this.removeById(item.getId());
            } else {
                item.setQuantity(quantity);
                this.updateById(item);
            }
        }
    }

    @Override
    @Transactional
    public List<Long> checkout(Long userId, List<Long> cartItemIds) {
        List<Long> successIds = new ArrayList<>();
        
        for (Long cartItemId : cartItemIds) {
            CartItem cartItem = this.getById(cartItemId);
            if (cartItem == null || !cartItem.getUserId().equals(userId)) {
                continue;
            }
            
            Product product = productService.getById(cartItem.getProductId());
            if (product == null || product.getStatus() != 1) {
                continue;
            }
            
            if (product.getStock() < cartItem.getQuantity()) {
                // 库存不足，只买能买的数量
                if (product.getStock() > 0) {
                    for (int i = 0; i < product.getStock(); i++) {
                        try {
                            orderService.createOrder(product.getId(), userId);
                        } catch (Exception e) {
                            break;
                        }
                    }
                    successIds.add(cartItemId);
                }
            } else {
                // 库存足够，买全部
                for (int i = 0; i < cartItem.getQuantity(); i++) {
                    try {
                        orderService.createOrder(product.getId(), userId);
                    } catch (Exception e) {
                        break;
                    }
                }
                successIds.add(cartItemId);
            }
        }
        
        return successIds;
    }
}
