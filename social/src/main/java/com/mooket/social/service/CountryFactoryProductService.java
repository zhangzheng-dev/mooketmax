package com.mooket.social.service;

/**
 * 国家+厂号+产品 Service 接口
 */
public interface CountryFactoryProductService {

    /**
     * 获取国家+厂号+产品详情
     *
     * @param country 国家名称
     * @param factoryNo 厂号
     * @param productName 产品名称
     * @param type 类型：offer(报盘) 或 inquiry(求购)
     * @param category 品类
     * @param sortBy 排序：comprehensive(综合) / publish_time(发布时间) / price_asc(价格升序) / price_desc(价格降序)
     * @param page 页码
     * @param pageSize 每页大小
     * @return 国家+厂号+产品详情
     */
    com.mooket.social.dto.CountryFactoryProductDetailDTO getCountryFactoryProductDetail(
            String country,
            String factoryNo,
            String productName,
            String type,
            String category,
            String sortBy,
            int page,
            int pageSize
    );

    String getOfferOriginalText(Long offerId);
}
