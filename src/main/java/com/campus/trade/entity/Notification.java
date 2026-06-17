package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;         // 接收用户ID
    private String title;        // 通知标题
    private String content;      // 通知内容
    private String type;         // 类型: system-系统通知, order-订单通知, audit-审核通知
    private Integer isRead;      // 是否已读: 0-未读, 1-已读
    private LocalDateTime createTime;  // 创建时间
}
