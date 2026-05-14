package com.mooket.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 品牌产品维度统计实体
 */
@Data
@TableName("stat_brand_product")
public class StatBrandProduct {

    @TableId(type = IdType.AUTO)
    private Integer statId;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 品牌ID
     */
    private Integer brandId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 产品ID
     */
    private Integer productId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 分类（牛/猪）
     */
    private String category;

    /**
     * 今日报盘工厂数
     */
    private Integer todayFactoryCount;

    /**
     * 今日报盘数
     */
    private Integer todayOfferCount;

    /**
     * 今日最低价
     */
    private BigDecimal priceMin;

    /**
     * 今日最高价
     */
    private BigDecimal priceMax;

    /**
     * 今日均价
     */
    private BigDecimal avgPrice;

    /**
     * 昨日均价
     */
    private BigDecimal avgPriceYesterday;

    /**
     * 涨跌额
     */
    private BigDecimal priceChange;

    /**
     * 涨跌幅（%）
     */
    private BigDecimal priceChangeRate;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
