package com.campus.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.ProductService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public Page<Product> getProductPage(int pageNum, int pageSize, Long categoryId, String keyword) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        
        // 只查已审核且在售且有库存的商品
        wrapper.eq("status", 1);
        wrapper.eq("audit_status", 1);
        wrapper.gt("stock", 0);
        
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("title", keyword);
        }
        // 按优先级和创建时间排序
        wrapper.orderByDesc("priority", "create_time");
        return this.page(page, wrapper);
    }

    @Override
    public Product publish(Product product) {
        // 新发布的商品默认状态为待审核
        product.setAuditStatus(0);
        // 审核通过才上架，暂时设置为0
        product.setStatus(0);
        // 默认库存为1，如果没设置的话
        if (product.getStock() == null || product.getStock() < 1) {
            product.setStock(1);
        }
        product.setCreateTime(LocalDateTime.now());
        this.save(product);
        return product;
    }

    /**
     * 安全更新库存和状态 - 只更新指定字段，不影响其他字段
     * @param productId 商品ID
     * @param stock 库存数量
     * @param status 状态（1在售 0已售 -1下架）
     */
    public void updateStockAndStatus(Long productId, Integer stock, Integer status) {
        UpdateWrapper<Product> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", productId);
        wrapper.set("stock", stock);
        wrapper.set("status", status);
        wrapper.set("update_time", LocalDateTime.now());
        this.update(wrapper);
    }
}
