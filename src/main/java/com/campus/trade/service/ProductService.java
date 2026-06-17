package com.campus.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.trade.entity.Product;

public interface ProductService extends IService<Product> {
    Page<Product> getProductPage(int pageNum, int pageSize, Long categoryId, String keyword);
    Product publish(Product product);
}
