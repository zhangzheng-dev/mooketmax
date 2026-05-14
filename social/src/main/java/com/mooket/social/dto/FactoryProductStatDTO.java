package com.mooket.social.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 厂号产品维度统计DTO（用于SQL聚合结果）
 */
@Data
public class FactoryProductStatDTO {
    private String country;
    private String factoryNo;
    private Integer factoryId;
    private Integer productId;
    private String productName;
    private String category;
    private Integer todayOfferCount;
    private Integer todayInquiryCount;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private BigDecimal avgPrice;
    private BigDecimal avgPriceYesterday;
}
