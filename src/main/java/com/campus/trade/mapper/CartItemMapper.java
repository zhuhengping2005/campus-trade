package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trade.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
    
    @Select("SELECT * FROM cart_items WHERE user_id = #{userId}")
    List<CartItem> findByUserId(Long userId);
    
    @Select("SELECT * FROM cart_items WHERE user_id = #{userId} AND product_id = #{productId}")
    CartItem findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    
    @Update("UPDATE cart_items SET quantity = quantity + 1 WHERE id = #{id}")
    void incrementQuantity(Long id);
}
