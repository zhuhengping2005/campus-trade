package com.campus.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.entity.Product;
import com.campus.trade.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        Page<Product> page = productService.getProductPage(pageNum, pageSize, categoryId, keyword);
        result.put("success", true);
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    @PostMapping
    public Map<String, Object> publish(@RequestBody Product product) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证库存必须大于0
            if (product.getStock() == null || product.getStock() < 1) {
                result.put("success", false);
                result.put("message", "商品余量必须大于0");
                return result;
            }
            
            Product saved = productService.publish(product);
            result.put("success", true);
            result.put("data", saved);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Product product = productService.getById(id);
        result.put("success", product != null);
        result.put("data", product);
        return result;
    }

    /**
     * 图片上传接口
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择图片");
            return result;
        }
        
        // 限制文件类型和大小
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            result.put("success", false);
            result.put("message", "只能上传图片文件");
            return result;
        }
        
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            result.put("success", false);
            result.put("message", "图片大小不能超过5MB");
            return result;
        }
        
        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;
            
            // 创建上传目录
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 保存文件
            String filepath = uploadDir + filename;
            file.transferTo(new File(filepath));
            
            // 返回访问URL
            String url = "/uploads/" + filename;
            result.put("success", true);
            result.put("data", url);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        
        return result;
    }
}
