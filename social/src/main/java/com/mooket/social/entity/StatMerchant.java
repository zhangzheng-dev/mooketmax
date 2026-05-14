package com.mooket.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商家统计实体
 */
@Data
@TableName("stat_merchant")
public class StatMerchant {

    @TableId(type = IdType.AUTO)
    private Integer statId;

    private LocalDate statDate;

    private Long merchantId;

    private String category;

    private Integer todayOfferCount;

    private Integer todayInquiryCount;

    private Integer todayProductCount;

    private Integer todayFactoryCount;

    private LocalDateTime updateTime;
}
