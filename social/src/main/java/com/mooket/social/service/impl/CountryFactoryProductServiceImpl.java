package com.mooket.social.service.impl;

import com.mooket.social.dto.CountryFactoryProductDetailDTO;
import com.mooket.social.dto.CountryFactoryProductDetailDTO.DailyPrice;
import com.mooket.social.dto.CountryFactoryProductDetailDTO.EmployeeOfferDTO;
import com.mooket.social.dto.CountryFactoryProductDetailDTO.MerchantOfferGroup;
import com.mooket.social.entity.BizOffer;
import com.mooket.social.entity.DictMerchant;
import com.mooket.social.entity.DictProduct;
import com.mooket.social.entity.FactoryTier;
import com.mooket.social.mapper.BizOfferMapper;
import com.mooket.social.mapper.DictMerchantMapper;
import com.mooket.social.mapper.DictProductMapper;
import com.mooket.social.mapper.FactoryTierMapper;
import com.mooket.social.mapper.StatPriceTrendMapper;
import com.mooket.social.service.CountryFactoryProductService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 国家+厂号+产品服务实现
 */
@Service
public class CountryFactoryProductServiceImpl implements CountryFactoryProductService {

private final BizOfferMapper offerMapper;
    private final StatPriceTrendMapper trendMapper;
    private final DictProductMapper productMapper;
    private final DictMerchantMapper merchantMapper;
    private final FactoryTierMapper factoryTierMapper;

        public CountryFactoryProductServiceImpl(BizOfferMapper offerMapper, StatPriceTrendMapper trendMapper, DictProductMapper productMapper, DictMerchantMapper merchantMapper, FactoryTierMapper factoryTierMapper) {
        this.offerMapper = offerMapper;
        this.trendMapper = trendMapper;
        this.productMapper = productMapper;
        this.merchantMapper = merchantMapper;
        this.factoryTierMapper = factoryTierMapper;
    }

    @Override
    @Cacheable(value = "countryFactoryProductDetail", key = "#country + '_' + #factoryNo + '_' + #productName + '_' + #type + '_' + #category + '_' + #sortBy + '_' + #page + '_' + #pageSize")
    public CountryFactoryProductDetailDTO getCountryFactoryProductDetail(String country, String factoryNo,
                                                                          String productName, String type,
                                                                          String category, String sortBy,
                                                                          int page, int pageSize) {
        return buildCountryFactoryProductDetail(country, factoryNo, productName, type, category, sortBy, page, pageSize);
    }

    @Override
    public String getOfferOriginalText(Long offerId) {
        if (offerId == null) {
            return "";
        }
        String text = offerMapper.selectOfferOriginalText(offerId);
        return text != null ? text : "";
    }

    /**
     * 构建国家+厂号+产品详情
     */
    private CountryFactoryProductDetailDTO buildCountryFactoryProductDetail(String country, String factoryNo,
                                                                             String productName, String type,
                                                                             String category, String sortBy,
                                                                             int page, int pageSize) {
        CountryFactoryProductDetailDTO dto = new CountryFactoryProductDetailDTO();
        dto.setCountry(country);
        dto.setFactoryNo(factoryNo);
        dto.setProductName(productName);

// 获取 productId
        DictProduct product = productMapper.findByName(category, productName);
        if (product != null && product.getProductId() != null) {
            dto.setProductId(product.getProductId());
        }

        // 查询是否有平替产品
        // 判断标准：factory_tier 表中同一 category + product_name 下有多个不同 tier 的厂号
        String tier = factoryTierMapper.selectTierByFactoryNo(category, productName, factoryNo);
        boolean hasSubstitute = false;
        if (tier != null && !tier.isEmpty()) {
            List<String> sameTierFactories = factoryTierMapper.selectFactoryNosByTier(category, productName, tier);
            if (sameTierFactories != null && sameTierFactories.size() > 1) {
                hasSubstitute = true;
            }
        }
        dto.setHasSubstitute(hasSubstitute);

        // 1. 获取看板统计数据（报盘数、求购数、商家数）
        BizOfferMapper.CountryFactoryProductStats stats = offerMapper.selectCountryFactoryProductStats(
                country, factoryNo, productName, category);
        dto.setOfferCount(stats != null && stats.totalOfferCount != null ? stats.totalOfferCount : 0L);
        dto.setInquiryCount(stats != null && stats.totalInquiryCount != null ? stats.totalInquiryCount : 0L);
        dto.setMerchantCount(stats != null && stats.merchantCount != null ? stats.merchantCount : 0);

        // 2. 获取过滤后的价格区间（按type分开计算IQR）
        String priceOfferType = "offer".equalsIgnoreCase(type) ? "报盘" : ("inquiry".equalsIgnoreCase(type) ? "求购" : null);
        BizOfferMapper.PriceRange priceRange = offerMapper.selectFilteredPriceRangeByCountryFactoryProduct(
                country, factoryNo, productName, category, priceOfferType);
        dto.setPriceMin(priceRange != null ? priceRange.priceMin : null);
        dto.setPriceMax(priceRange != null ? priceRange.priceMax : null);

        // 3. 计算日均价涨跌（今日 vs 昨日）
        calculatePriceChange(country, factoryNo, dto.getProductId(), productName, category, priceOfferType, dto);

        // 4. 获取近7日价格走势
        List<DailyPrice> priceHistory7Days = getPriceHistory7Days(country, factoryNo, dto.getProductId(), productName, category, priceOfferType);
        dto.setPriceHistory7Days(priceHistory7Days);

        // 5. 获取近30日价格趋势
        List<DailyPrice> priceHistory30Days = getPriceHistory30Days(country, factoryNo, dto.getProductId(), productName, category, priceOfferType);
        dto.setPriceHistory30Days(priceHistory30Days);

        // 6. 获取报盘列表（先获取所有记录，分组后再分页，避免分页位置不同导致分组数量不一致）
        String offerType = "offer".equalsIgnoreCase(type) ? "报盘" : ("inquiry".equalsIgnoreCase(type) ? "求购" : null);

        // 价格排序需要在分组后按聚合的priceMin/priceMax排序，不在数据库层排序
        String dbSortBy = "price_asc".equals(sortBy) || "price_desc".equals(sortBy) ? "comprehensive" : sortBy;

        // 获取足够多的记录用于分组（一次性获取，分组后再分页）
        int fetchLimit = 1000;
        List<BizOffer> offers = offerMapper.selectOfferListByCountryFactoryProduct(
                country, factoryNo, productName, category, offerType, dbSortBy, fetchLimit, 0);

        // 按商家分组
        List<MerchantOfferGroup> merchantGroups = groupOffersByMerchant(offers);

        // 应用内存排序（分组后按聚合的价格排序）
        if ("price_asc".equals(sortBy)) {
            // 升序：取每个产品近两日报盘价格区间的最小值，升序排列
            merchantGroups.sort((a, b) -> {
                BigDecimal priceA = getMinPrice(a.getEmployeeOffers());
                BigDecimal priceB = getMinPrice(b.getEmployeeOffers());
                return priceA.compareTo(priceB);
            });
        } else if ("price_desc".equals(sortBy)) {
            // 降序：取每个产品近两日报盘价格区间的最大值，降序排列
            merchantGroups.sort((a, b) -> {
                BigDecimal priceA = getMaxPrice(a.getEmployeeOffers());
                BigDecimal priceB = getMaxPrice(b.getEmployeeOffers());
                return priceB.compareTo(priceA);
            });
        } else if ("publish_time".equals(sortBy)) {
            // 发布时间排序：按最新发布排序
            merchantGroups.sort((a, b) -> {
                String timeA = a.getEmployeeOffers().stream()
                    .map(EmployeeOfferDTO::getPublishTime)
                    .filter(Objects::nonNull)
                    .max(String::compareTo)
                    .orElse("");
                String timeB = b.getEmployeeOffers().stream()
                    .map(EmployeeOfferDTO::getPublishTime)
                    .filter(Objects::nonNull)
                    .max(String::compareTo)
                    .orElse("");
                return timeB.compareTo(timeA);
            });
        } else {
            // 综合排序：按报盘数降序
            merchantGroups.sort((a, b) -> b.getOfferCount().compareTo(a.getOfferCount()));
        }

        // 内存分页
        int totalCount = merchantGroups.size();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, merchantGroups.size());
        List<MerchantOfferGroup> pagedGroups = fromIndex < merchantGroups.size()
            ? merchantGroups.subList(fromIndex, toIndex)
            : Collections.emptyList();

        dto.setMerchantOffers(pagedGroups);
        dto.setTotalCount(totalCount);
        dto.setPage(page);
        dto.setPageSize(pageSize);
        dto.setTotalPages(totalPages);

        return dto;
    }

    /**
     * 按商家分组报盘数据
     */
    private List<MerchantOfferGroup> groupOffersByMerchant(List<BizOffer> offers) {
        // 使用 Map 来分组：key = merchantId 或 "NO_MERCHANT"
        Map<String, List<BizOffer>> groupedByKey = new LinkedHashMap<>();
        Set<Long> merchantIds = offers.stream()
                .map(BizOffer::getMerchantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DictMerchant> merchantMap = merchantIds.isEmpty()
                ? Collections.emptyMap()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                    .collect(Collectors.toMap(DictMerchant::getMerchantId, m -> m, (left, right) -> left));

        for (BizOffer offer : offers) {
            String groupKey;
            if (offer.getMerchantId() != null) {
                groupKey = "merchant_" + offer.getMerchantId();
            } else {
                // 没有商家ID的员工报价，统一归为一组
                groupKey = "NO_MERCHANT";
            }

            groupedByKey.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(offer);
        }

        // 转换为 MerchantOfferGroup 列表
        List<MerchantOfferGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<BizOffer>> entry : groupedByKey.entrySet()) {
            List<BizOffer> groupOffers = entry.getValue();
            if (groupOffers.isEmpty()) continue;

            BizOffer firstOffer = groupOffers.get(0);
            MerchantOfferGroup group = new MerchantOfferGroup();

            // 设置商家信息
            if (entry.getKey().startsWith("merchant_")) {
                // 有商家ID的，查询商家名称
                Long merchantId = firstOffer.getMerchantId();
                group.setMerchantId(merchantId);
                group.setMerchantPhone(firstOffer.getContactPhone());

                // 通过 merchantId 查询商家名称
                DictMerchant merchant = merchantMap.get(merchantId);
                if (merchant != null) {
                    group.setMerchantName(merchant.getMerchantName());
                    // 检查是否为知名商家（通过 merchantTags 判断）
                    boolean isFamous = merchant.getMerchantTags() != null &&
                            merchant.getMerchantTags().contains("知名商家");
                    group.setIsFamousMerchant(isFamous);
                } else {
                    group.setMerchantName(firstOffer.getContactPhone());
                    group.setIsFamousMerchant(false);
                }
            } else {
                // 没有商家ID的，显示"暂未关联行业商家"
                group.setMerchantId(null);
                group.setMerchantName("暂未关联行业商家");
                group.setMerchantPhone(firstOffer.getContactPhone());
                group.setIsFamousMerchant(false);
            }

            group.setOfferCount(groupOffers.size());

            // 转换员工报价列表
            List<EmployeeOfferDTO> employeeOfferDTOs = groupOffers.stream()
                    .map(this::convertToEmployeeOfferDTO)
                    .collect(Collectors.toList());
            group.setEmployeeOffers(employeeOfferDTOs);

            groups.add(group);
        }

        // 按报盘数排序（综合推荐）
        groups.sort((a, b) -> b.getOfferCount().compareTo(a.getOfferCount()));

        return groups;
    }

    /**
     * 转换为员工报价DTO
     */
    private EmployeeOfferDTO convertToEmployeeOfferDTO(BizOffer offer) {
        EmployeeOfferDTO dto = new EmployeeOfferDTO();
        dto.setOfferId(offer.getOfferId());
        dto.setUserNickname(offer.getUserNickname());
        dto.setWeight(offer.getWeight());
        dto.setGoodsLocation(offer.getGoodsLocation());
        dto.setGoodsType(offer.getGoodsType());
        dto.setTags(offer.getTags());
        dto.setOfferType(offer.getOfferType());
        dto.setOfferOriginalText(null);

        // 发布时间格式化（返回完整日期时间，前端判断今天/昨天）
        if (offer.getPublishTime() != null) {
            dto.setPublishTime(offer.getPublishTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }

        // 处理价格显示
        if (offer.getPrice() != null) {
            if (offer.getPriceMax() != null && !offer.getPriceMax().equals(offer.getPrice())) {
                dto.setPrice(String.format("%.1f-%.1f", offer.getPrice(), offer.getPriceMax()));
            } else {
                dto.setPrice(String.format("%.1f", offer.getPrice()));
            }
        } else {
            dto.setPrice("协商报价");
        }

        return dto;
    }

    /**
     * 获取员工报价列表中的最低价格
     */
    private BigDecimal getMinPrice(List<EmployeeOfferDTO> offers) {
        return offers.stream()
                .map(EmployeeOfferDTO::getPrice)
                .filter(p -> p != null && !p.equals("协商报价"))
                .map(p -> {
                    // 处理范围价格如 "54.8-55.2"，取最小值
                    if (p.contains("-")) {
                        return new BigDecimal(p.split("-")[0]);
                    }
                    return new BigDecimal(p);
                })
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getMaxPrice(List<EmployeeOfferDTO> offers) {
        return offers.stream()
                .map(EmployeeOfferDTO::getPrice)
                .filter(p -> p != null && !p.equals("协商报价"))
                .map(p -> {
                    // 处理范围价格如 "54.8-55.2"，取最大值
                    if (p.contains("-")) {
                        String[] parts = p.split("-");
                        return new BigDecimal(parts[parts.length - 1]);
                    }
                    return new BigDecimal(p);
                })
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * 计算日均价涨跌
     */
    private void calculatePriceChange(String country, String factoryNo, Integer productId,
                                      String productName, String category, String offerType,
                                      CountryFactoryProductDetailDTO dto) {
        List<StatPriceTrendMapper.PriceTrendPoint> trendPoints = trendMapper.selectTrendPointsByCountryFactoryProduct(
                StatPriceTrendMapper.DIMENSION_COUNTRY_FACTORY_PRODUCT,
                country,
                productId,
                factoryNo,
                offerType
        );

        if (trendPoints == null || trendPoints.isEmpty()) {
            dto.setPriceChange(null);
            dto.setPriceChangeRate(null);
            return;
        }

        trendPoints.sort(Comparator.comparing(p -> p.date));

        BigDecimal todayPrice = null;
        BigDecimal yesterdayPrice = null;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        for (StatPriceTrendMapper.PriceTrendPoint point : trendPoints) {
            if (point.date.equals(today)) {
                todayPrice = point.avgPrice;
            } else if (point.date.equals(yesterday)) {
                yesterdayPrice = point.avgPrice;
            }
        }

        if (todayPrice != null && yesterdayPrice != null && yesterdayPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = todayPrice.subtract(yesterdayPrice).setScale(1, RoundingMode.HALF_UP);
            BigDecimal changeRate = change.multiply(new BigDecimal("100"))
                    .divide(yesterdayPrice, 1, RoundingMode.HALF_UP);

            dto.setPriceChange(change);
            dto.setPriceChangeRate(changeRate);
        } else {
            dto.setPriceChange(null);
            dto.setPriceChangeRate(null);
        }
    }

    /**
     * 获取近7日价格走势
     */
    private List<DailyPrice> getPriceHistory7Days(String country, String factoryNo, Integer productId,
                                                   String productName, String category, String offerType) {
        if (productId == null) {
            return Collections.emptyList();
        }

        List<StatPriceTrendMapper.PriceTrendPoint> trendPoints = trendMapper.selectTrendPointsByCountryFactoryProduct(
                StatPriceTrendMapper.DIMENSION_COUNTRY_FACTORY_PRODUCT,
                country,
                productId,
                factoryNo,
                offerType
        );

        if (trendPoints == null || trendPoints.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
        List<StatPriceTrendMapper.PriceTrendPoint> recentPoints = trendPoints.stream()
                .filter(p -> p.date != null && !p.date.isBefore(sevenDaysAgo))
                .collect(Collectors.toList());

        if (recentPoints.isEmpty()) {
            return Collections.emptyList();
        }

        DateTimeFormatter shortFormatter = DateTimeFormatter.ofPattern("MM-dd");
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return recentPoints.stream()
                .map(point -> {
                    DailyPrice dp = new DailyPrice();
                    dp.setDate(point.date.format(shortFormatter));
                    dp.setFullDate(point.date.format(fullFormatter));
                    dp.setAvgPrice(point.avgPrice != null ? point.avgPrice.setScale(1, RoundingMode.HALF_UP) : null);
                    dp.setPriceUnit("元/kg");
                    dp.setOfferCount(null);
                    return dp;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取近30日价格趋势
     */
    private List<DailyPrice> getPriceHistory30Days(String country, String factoryNo, Integer productId,
                                                    String productName, String category, String offerType) {
        if (productId == null) {
            return Collections.emptyList();
        }

        List<StatPriceTrendMapper.PriceTrendPoint> trendPoints = trendMapper.selectTrendPointsByCountryFactoryProduct(
                StatPriceTrendMapper.DIMENSION_COUNTRY_FACTORY_PRODUCT,
                country,
                productId,
                factoryNo,
                offerType
        );

        if (trendPoints == null || trendPoints.isEmpty()) {
            return Collections.emptyList();
        }

        DateTimeFormatter shortFormatter = DateTimeFormatter.ofPattern("MM-dd");
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return trendPoints.stream()
                .map(point -> {
                    DailyPrice dp = new DailyPrice();
                    dp.setDate(point.date.format(shortFormatter));
                    dp.setFullDate(point.date.format(fullFormatter));
                    dp.setAvgPrice(point.avgPrice != null ? point.avgPrice.setScale(1, RoundingMode.HALF_UP) : null);
                    dp.setPriceUnit("元/kg");
                    dp.setOfferCount(null);
                    return dp;
                })
                .collect(Collectors.toList());
    }
}
