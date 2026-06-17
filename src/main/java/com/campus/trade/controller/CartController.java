package com.campus.trade.controller;

import com.campus.trade.entity.CartItem;
import com.campus.trade.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<CartItem> items = cartService.getCartItems(userId);
        result.put("success", true);
        result.put("data", items);
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            CartItem item = cartService.addToCart(params.get("userId"), params.get("productId"));
            result.put("success", true);
            result.put("data", item);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/remove")
    public Map<String, Object> remove(@RequestParam Long userId, @RequestParam Long productId) {
        Map<String, Object> result = new HashMap<>();
        try {
            cartService.removeFromCart(userId, productId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            cartService.updateQuantity(
                params.get("userId"), 
                params.get("productId"),
                params.get("quantity") != null ? params.get("quantity").intValue() : 0
            );
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clear(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            cartService.clearCart(userId);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    // 批量结算
    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            @SuppressWarnings("unchecked")
            List<Long> cartItemIds = (List<Long>) params.get("cartItemIds");
            
            List<Long> successIds = cartService.checkout(userId, cartItemIds);
            
            result.put("success", true);
            result.put("data", successIds);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
