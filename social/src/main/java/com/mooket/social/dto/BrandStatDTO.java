package com.mooket.social.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 品牌维度统计DTO（用于SQL聚合结果）
 */
@Data
public class BrandStatDTO {
    private Integer brandId;
    private String brandName;
    private String category;
    private Integer todayOfferCount;
    private Integer todayFactoryCount;
    private Integer todayProductCount;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
}
