package com.campus.trade.controller;

import com.campus.trade.entity.Category;
import com.campus.trade.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        List<Category> categories = categoryService.getAllCategories();
        result.put("success", true);
        result.put("data", categories);
        return result;
    }
}
