package com.mooket.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 品牌维度统计实体
 */
@Data
@TableName("stat_brand")
public class StatBrand {

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
     * 分类（牛/猪）
     */
    private String category;

    /**
     * 今日报盘数
     */
    private Integer todayOfferCount;

    /**
     * 今日报盘工厂数
     */
    private Integer todayFactoryCount;

    /**
     * 今日报盘产品种类数
     */
    private Integer todayProductCount;

    /**
     * 今日最低价
     */
    private BigDecimal priceMin;

    /**
     * 今日最高价
     */
    private BigDecimal priceMax;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
