package com.mooket.social.service.impl;

import com.mooket.social.dto.FactoryDetailDTO;
import com.mooket.social.entity.DictFactory;
import com.mooket.social.mapper.BizOfferMapper;
import com.mooket.social.mapper.DictFactoryMapper;
import com.mooket.social.service.FactoryService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 厂号 Service 实现
 */
@Service
public class FactoryServiceImpl implements FactoryService {

    private final BizOfferMapper offerMapper;
    private final DictFactoryMapper factoryMapper;

    public FactoryServiceImpl(BizOfferMapper offerMapper, DictFactoryMapper factoryMapper) {
        this.offerMapper = offerMapper;
        this.factoryMapper = factoryMapper;
    }

    @Override
    @Cacheable(value = "factoryDetail", key = "#country + '_' + #factoryNo + '_' + #category + '_' + #offerType + '_' + #sortBy + '_' + #page + '_' + #pageSize")
    public FactoryDetailDTO getFactoryDetail(String country, String factoryNo, String category,
                                            String offerType, String sortBy, int page, int pageSize) {
        // 1. 获取厂号信息（用于获取国家别名）
        List<DictFactory> factories = factoryMapper.selectByFactoryNo(factoryNo);
        String countryAlias = null;
        for (DictFactory f : factories) {
            if (f.getCountry().equals(country)) {
                countryAlias = f.getCountryAlias();
                break;
            }
        }

        // 2. 获取看板统计数据（时间窗口：近1天；按 offerType 过滤确保 recentOfferCount 只统计对应类型）
        String dbOfferType = "offer".equalsIgnoreCase(offerType) || "报盘".equals(offerType) ? "报盘" : "求购";
        BizOfferMapper.FactoryDashboardAgg dashboardStats =
                offerMapper.selectFactoryDashboardStats(country, factoryNo, category, dbOfferType);

        FactoryDetailDTO dto = new FactoryDetailDTO();
        dto.setCountry(country);
        dto.setFactoryNo(factoryNo);
        dto.setCountryAlias(countryAlias);
        dto.setProductCount(dashboardStats.productCount != null ? dashboardStats.productCount : 0);
        dto.setInquiryCount(dashboardStats.inquiryCount != null ? dashboardStats.inquiryCount : 0);
        dto.setRecentOfferCount(dashboardStats.recentOfferCount != null ? dashboardStats.recentOfferCount : 0);

        // 3. 获取产品全量数据（用于排序）
        int offset = (page - 1) * pageSize;
        boolean fastPath = "comprehensive".equalsIgnoreCase(sortBy);
        List<BizOfferMapper.FactoryProductAgg> aggList = fastPath
                ? offerMapper.selectFactoryProductAggFast(country, factoryNo, category, dbOfferType, pageSize, offset)
                : offerMapper.selectFactoryProductAgg(country, factoryNo, category, dbOfferType, 1000, 0);

        // 4. 获取总数
        int totalCount = fastPath
                ? offerMapper.countFactoryProductAggFast(country, factoryNo, category, dbOfferType)
                : offerMapper.countFactoryProductAgg(country, factoryNo, category, dbOfferType);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        // 5. 排序（全量排序后再分页，保证跨页一致性）
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            aggList.sort((a, b) -> {
                BigDecimal aPrice = a.priceMin != null ? a.priceMin : BigDecimal.ZERO;
                BigDecimal bPrice = b.priceMin != null ? b.priceMin : BigDecimal.ZERO;
                return aPrice.compareTo(bPrice);
            });
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            aggList.sort((a, b) -> {
                BigDecimal aPrice = a.priceMax != null ? a.priceMax : BigDecimal.ZERO;
                BigDecimal bPrice = b.priceMax != null ? b.priceMax : BigDecimal.ZERO;
                return bPrice.compareTo(aPrice);
            });
        }
        // else: 默认按报盘数降序已在SQL中处理

        // 6. 分页
        List<BizOfferMapper.FactoryProductAgg> pagedAgg;
        if (fastPath) {
            pagedAgg = aggList;
        } else {
            int endIndex = Math.min(offset + pageSize, aggList.size());
            pagedAgg = offset < aggList.size() ? aggList.subList(offset, endIndex) : Collections.emptyList();
        }

        // 7. 转换聚合数据为DTO
        List<FactoryDetailDTO.FactoryProductDTO> products = pagedAgg.stream()
                .map(agg -> {
                    FactoryDetailDTO.FactoryProductDTO product = new FactoryDetailDTO.FactoryProductDTO();
                    product.setProductId(agg.productId);
                    product.setProductName(agg.productName);
                    product.setPriceMin(agg.priceMin != null ? agg.priceMin.doubleValue() : null);
                    product.setPriceMax(agg.priceMax != null ? agg.priceMax.doubleValue() : null);
                    product.setMerchantCount(agg.merchantCount);
                    product.setOfferCount(agg.offerCount);
                    // 解析商家名称列表（shortName|fullName格式，取前3个）
                    if (agg.merchantNames != null && !agg.merchantNames.isEmpty()) {
                        List<String> merchantList = Arrays.stream(agg.merchantNames.split(","))
                                .filter(n -> n != null && !n.isEmpty())
                                .distinct()
                                .limit(3)
                                .map(raw -> {
                                    int sep = raw.indexOf('|');
                                    if (sep > 0) {
                                        String shortName = raw.substring(0, sep);
                                        String fullName = raw.substring(sep + 1);
                                        return (!shortName.isEmpty() && !"NULL".equalsIgnoreCase(shortName)) ? shortName : fullName;
                                    }
                                    return raw;
                                })
                                .collect(Collectors.toList());
                        product.setMerchantNames(merchantList);
                    } else {
                        product.setMerchantNames(Collections.emptyList());
                    }
                    return product;
                })
                .collect(Collectors.toList());

        // 8. 设置返回结果
        dto.setProducts(products);
        dto.setTotalCount(totalCount);
        dto.setPage(page);
        dto.setPageSize(pageSize);
        dto.setTotalPages(totalPages);

        return dto;
    }
}
