package com.hn.yuan.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商品订单详情表
 * </p>
 *
 * @author XIAOCAO
 * @since 2022-09-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Goods implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单表id
     */
    private Long orderId;

    /**
     * 商品详情表id
     */
    private Long goodsId;

    /**
     * 购买商品数量
     */
    private Long goodsNumber;

    /**
     * 商品价格
     */
    private BigDecimal goodsPrice;

    /**
     * 商品总价格
     */
    private BigDecimal goodsTotalPrice;


}
