package com.mooket.social.controller;

import com.mooket.social.common.ApiResponse;
import com.mooket.social.dto.CountryFactoryProductDetailDTO;
import com.mooket.social.service.CountryFactoryProductService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 国家+厂号+产品 Controller
 */
@RestController
@RequestMapping("/api/v1/country-factory-product")
public class CountryFactoryProductController {

    private final CountryFactoryProductService countryFactoryProductService;

    public CountryFactoryProductController(CountryFactoryProductService countryFactoryProductService) {
        this.countryFactoryProductService = countryFactoryProductService;
    }

    /**
     * 获取国家+厂号+产品详情
     *
     * @param country 国家名称
     * @param factoryNo 厂号
     * @param productName 产品名称
     * @param type 类型：offer(报盘) 或 inquiry(求购)
     * @param category 品类（牛/猪）
     * @param sortBy 排序方式：comprehensive(综合) / publish_time(发布时间) / price_asc(价格升序) / price_desc(价格降序)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GetMapping
    public ApiResponse<CountryFactoryProductDetailDTO> getCountryFactoryProductDetail(
            @RequestParam("country") String country,
            @RequestParam("factoryNo") String factoryNo,
            @RequestParam("productName") String productName,
            @RequestParam(value = "type", defaultValue = "offer") String type,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sortBy", defaultValue = "comprehensive") String sortBy,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        try {
            CountryFactoryProductDetailDTO result = countryFactoryProductService.getCountryFactoryProductDetail(
                    country, factoryNo, productName, type, category, sortBy, page, pageSize);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/offer/{offerId}/original-text")
    public ApiResponse<Map<String, Object>> getOfferOriginalText(@PathVariable("offerId") Long offerId) {
        try {
            String text = countryFactoryProductService.getOfferOriginalText(offerId);
            return ApiResponse.success(Map.of("offerId", offerId, "text", text));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
