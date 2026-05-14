package com.mooket.social.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 品牌产品维度统计DTO（用于SQL聚合结果）
 */
@Data
public class BrandProductStatDTO {
    private Integer brandId;
    private String brandName;
    private Integer productId;
    private String productName;
    private String category;
    private Integer todayOfferCount;
    private Integer todayFactoryCount;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private BigDecimal avgPrice;
    private BigDecimal avgPriceYesterday;
}
