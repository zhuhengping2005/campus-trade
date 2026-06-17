package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String image;
    private Long categoryId;
    private Long sellerId;
    
    @TableField("`condition`")
    private String condition;
    
    private Integer stock;  // 库存数量
    private Integer status; // 1在售 0已售 -1下架
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 新增字段
    private Integer priority = 0;      // 优先级，数字越大越靠前
    private Integer auditStatus = 1;    // 审核状态：0待审核，1已通过，-1已拒绝
    private LocalDateTime auditTime;    // 审核时间
    private String auditRemark;         // 审核备注
}
