package com.campus.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.trade.entity.Category;
import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> getAllCategories();
}
