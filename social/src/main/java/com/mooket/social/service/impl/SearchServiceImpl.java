package com.mooket.social.service.impl;

import com.mooket.social.dto.SearchSuggestDTO;
import com.mooket.social.entity.DictBrand;
import com.mooket.social.entity.DictFactory;
import com.mooket.social.entity.DictMerchant;
import com.mooket.social.entity.DictProduct;
import com.mooket.social.mapper.BizSearchHistoryMapper;
import com.mooket.social.mapper.DictBrandMapper;
import com.mooket.social.mapper.DictFactoryMapper;
import com.mooket.social.mapper.DictMerchantMapper;
import com.mooket.social.mapper.DictProductMapper;
import com.mooket.social.service.SearchService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务实现 - 按需求文档3.2.1联想逻辑
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final DictProductMapper productMapper;
    private final DictFactoryMapper factoryMapper;
    private final DictBrandMapper brandMapper;
    private final DictMerchantMapper merchantMapper;
    private final BizSearchHistoryMapper searchHistoryMapper;

    // 解析出的实体
    private static class ParsedEntities {
        String country;           // 匹配到的国家标准名
        String countryInput;      // 用户输入的国家名（可能是别名）
        String countryAlias;      // 匹配到的国家别名（如果有）
        String remainingKeywordAfterCountry; // 匹配国家后，关键词剩余部分（用于生成"国家+产品"建议）
        String factoryNo;         // 匹配到的厂号
        String factoryNoInput;    // 用户输入的厂号（可能是纯数字）
        String brandName;         // 匹配到的品牌标准名
        String brandInput;        // 用户输入的品牌名
        String brandAlias;        // 匹配到的品牌别名（如果有）
        String productName;       // 匹配到的产品标准名
        String productInput;      // 用户输入的产品名
        String productAlias;     // 匹配到的产品别名（如果有）
        String merchantName;      // 匹配到的商家标准名
        String merchantInput;     // 用户输入的商家名
        String merchantAlias;     // 匹配到的商家别名（简称）
        DictProduct product;      // 匹配到的产品对象（存储以避免再次按category查询）
        List<DictProduct> matchedProducts; // 所有匹配的产品（用于单实体搜索）
        List<DictFactory> matchedFactories; // 所有匹配的厂号（用于厂号搜索）

        boolean hasFactory() { return factoryNo != null; }
        boolean hasProduct() { return productName != null; }
        boolean hasCountry() { return country != null; }
        boolean hasBrand() { return brandName != null; }
        boolean hasMerchant() { return merchantName != null; }
    }

    public SearchServiceImpl(DictProductMapper productMapper,
                            DictFactoryMapper factoryMapper,
                            DictBrandMapper brandMapper,
                            DictMerchantMapper merchantMapper,
                            BizSearchHistoryMapper searchHistoryMapper) {
        this.productMapper = productMapper;
        this.factoryMapper = factoryMapper;
        this.brandMapper = brandMapper;
        this.merchantMapper = merchantMapper;
        this.searchHistoryMapper = searchHistoryMapper;
    }

    @Override
    public List<SearchSuggestDTO> getSearchSuggestions(String category, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        keyword = keyword.trim();
        String normalizedKeyword = keyword.replace(" ", "");

        // 1. 解析实体
        ParsedEntities parsed = parseEntities(category, normalizedKeyword);

        log.info("[DEBUG] parseEntities后: country={}, factoryNo={}, hasCountry={}, hasFactory={}, matchedFactories大小={}",
                parsed.country, parsed.factoryNo, parsed.hasCountry(), parsed.hasFactory(),
                parsed.matchedFactories != null ? parsed.matchedFactories.size() : 0);

        // 2. 一致性校验：国家与厂号
        if (parsed.hasCountry() && parsed.hasFactory()) {
            if (!checkFactoryCountryMatch(category, parsed.factoryNo, parsed.country)) {
                // 不一致，返回空
                return new ArrayList<>();
            }
        }

        // 3. 按优先级生成联想词
        List<SearchSuggestDTO> suggestions = new ArrayList<>();
        Set<Integer> usedTargetIds = new HashSet<>(); // 用于去重

        // Priority 1: 厂号 + 产品
        if (parsed.hasFactory() && parsed.hasProduct()) {
            suggestions.addAll(generateFactoryProductSuggestions(category, keyword, parsed, usedTargetIds));
        }

        // Priority 2: 厂号独立（无产品）
        if (parsed.hasFactory() && !parsed.hasProduct()) {
            suggestions.addAll(generateFactoryOnlySuggestions(category, keyword, parsed, usedTargetIds));
        }

        // Priority 3: 国家 + 产品（无厂号）
        // 如果有国家+产品组合，或者有国家+剩余关键词（可能是产品名但未匹配到）
        if (parsed.hasCountry() && !parsed.hasFactory() && (parsed.hasProduct() || (parsed.remainingKeywordAfterCountry != null && !parsed.remainingKeywordAfterCountry.isEmpty()))) {
            suggestions.addAll(generateCountryProductSuggestions(category, keyword, parsed, usedTargetIds));
        }

        // Priority 4: 品牌 + 产品
        if (parsed.hasBrand() && parsed.hasProduct()) {
            suggestions.addAll(generateBrandProductSuggestions(category, keyword, parsed, usedTargetIds));
        }

        // Priority 5: 单实体（国家/产品/品牌，未被组合使用）
        suggestions.addAll(generateSingleEntitySuggestions(category, keyword, parsed, usedTargetIds));

        // Priority 6: 品牌（无产品）
        if (parsed.hasBrand() && !parsed.hasProduct()) {
            suggestions.addAll(generateBrandOnlySuggestions(category, keyword, parsed, usedTargetIds));
        }

        // Priority 7: 商家独立
        if (parsed.hasMerchant()) {
            suggestions.addAll(generateMerchantSuggestions(category, keyword, parsed));
        }

        // 按优先级排序
        suggestions.sort(Comparator.comparingInt(SearchSuggestDTO::getPriority));

        log.info("[DEBUG] getSearchSuggestions最终返回: {} 条建议", suggestions.size());
        for (SearchSuggestDTO s : suggestions) {
            log.info("[DEBUG]   返回建议: type={}, text={}, targetId={}", s.getType(), s.getText(), s.getTargetId());
        }
        return suggestions;
    }

    /**
     * 解析实体：国家、厂号、品牌、产品、商家
     */
    private ParsedEntities parseEntities(String category, String keyword) {
        ParsedEntities parsed = new ParsedEntities();

        // 获取所有数据用于匹配
        List<DictFactory> allFactories = factoryMapper.selectByCategory(category);
        // 尝试不同的category值来获取产品
        List<DictProduct> allProducts = productMapper.selectByCategory(category);
        if (allProducts.isEmpty()) {
            allProducts = productMapper.selectAll();
        }
        List<DictBrand> allBrands = brandMapper.selectAll();
        // 使用SQL-level搜索代替全量加载，searchByName已有LIMIT 20优化
        List<DictMerchant> matchedMerchants = merchantMapper.searchByName(keyword);
        List<DictMerchant> allMerchants = matchedMerchants.isEmpty() ? new ArrayList<>() : matchedMerchants;

        // 1. 解析国家（支持别名，双向匹配）
        for (DictFactory factory : allFactories) {
            String countryNoSpace = factory.getCountry().replace(" ", "");
            // 检查标准国家名（双向匹配：keyword包含国家名 或 国家名包含keyword）
            if (keyword.contains(countryNoSpace) || countryNoSpace.contains(keyword)) {
                parsed.country = factory.getCountry();
                parsed.countryInput = factory.getCountry();
                parsed.countryAlias = null; // 用户输入的是标准名，没有别名
                // 计算匹配国家后剩余的关键词部分
                if (keyword.contains(countryNoSpace)) {
                    parsed.remainingKeywordAfterCountry = keyword.replace(countryNoSpace, "");
                }
                break;
            }
            // 检查国家别名（双向匹配）
            if (factory.getCountryAlias() != null && !factory.getCountryAlias().isEmpty()) {
                String[] aliases = factory.getCountryAlias().split("[,，]");
                for (String alias : aliases) {
                    String aliasNoSpace = alias.replace(" ", "");
                    // 精确匹配：keyword等于alias，或 alias以keyword开头（前缀匹配），或 keyword是alias的前缀
                    if (keyword.equals(aliasNoSpace) ||
                        aliasNoSpace.startsWith(keyword) ||
                        (keyword.length() <= aliasNoSpace.length() && aliasNoSpace.contains(keyword))) {
                        parsed.country = factory.getCountry();
                        parsed.countryInput = alias.trim();
                        parsed.countryAlias = alias.trim(); // 记录匹配到的别名
                        // 计算匹配国家后剩余的关键词部分
                        parsed.remainingKeywordAfterCountry = keyword.replace(aliasNoSpace, "");
                        break;
                    }
                }
                if (parsed.country != null) break;
            }
        }

        // 2. 解析厂号（支持纯数字，收集所有匹配）
        parsed.matchedFactories = new ArrayList<>();
        String bestMatchFactoryNo = null;
        String bestMatchInput = null;
        int bestMatchLength = 0;
        // 用于存储每个国家的最佳匹配厂号: country -> {matchLen, factoryNo, input}
        Map<String, int[]> countryBestFactory = new HashMap<>();

        for (DictFactory factory : allFactories) {
            String factoryNoNormalized = factory.getFactoryNo().replace(" ", "").toLowerCase();
            boolean matched = false;

            // 完整厂号匹配（搜索词包含完整厂号）
            if (keyword.contains(factoryNoNormalized)) {
                matched = true;
                int matchLen = factoryNoNormalized.length();
                // 记录每个国家的最佳匹配
                int[] existing = countryBestFactory.get(factory.getCountry());
                if (existing == null || matchLen > existing[0]) {
                    countryBestFactory.put(factory.getCountry(), new int[]{matchLen});
                }
                if (matchLen > bestMatchLength) {
                    bestMatchLength = matchLen;
                    bestMatchFactoryNo = factory.getFactoryNo();
                    bestMatchInput = factory.getFactoryNo();
                }
            }
            // 纯数字匹配（搜索词包含纯数字）
            String pureNumber = extractPureNumber(factory.getFactoryNo());
            if (pureNumber != null) {
                // 检查多种匹配方式
                int matchLen = 0;
                String matchInput = null;

                // 方式1: keyword包含完整pureNumber
                if (keyword.contains(pureNumber)) {
                    matchLen = pureNumber.length();
                    matchInput = pureNumber;
                }
                // 方式2: 已解析出国家时，factory的pureNumber包含keyword的纯数字（任意位置匹配）
                else if (parsed.country != null && factory.getCountry().equals(parsed.country)) {
                    String keywordPure = extractPureNumber(keyword);
                    if (keywordPure != null && pureNumber.contains(keywordPure)) {
                        matchLen = keywordPure.length();
                        matchInput = keywordPure;
                    }
                }
                // 方式3: 国家未解析时，factory的pureNumber以keyword的纯数字部分开头（前缀匹配）或结尾（后缀匹配）
                else if (parsed.country == null) {
                    String keywordPure = extractPureNumber(keyword);
                    if (keywordPure != null) {
                        // 前缀匹配：factory的pureNumber以keyword的pureNumber开头
                        if (pureNumber.startsWith(keywordPure) && keywordPure.length() > matchLen) {
                            matchLen = keywordPure.length();
                            matchInput = keywordPure;
                        }
                        // 后缀匹配：factory的pureNumber以keyword的pureNumber结尾
                        if (pureNumber.endsWith(keywordPure) && keywordPure.length() > matchLen) {
                            matchLen = keywordPure.length();
                            matchInput = keywordPure;
                        }
                    }
                }

                if (matchLen > 0) {
                    matched = true;
                    // 记录每个国家的纯数字匹配
                    int[] existing = countryBestFactory.get(factory.getCountry());
                    if (existing == null || matchLen > existing[0]) {
                        countryBestFactory.put(factory.getCountry(), new int[]{matchLen});
                    }
                    if (matchLen > bestMatchLength) {
                        bestMatchLength = matchLen;
                        bestMatchFactoryNo = factory.getFactoryNo();
                        bestMatchInput = matchInput;
                    }
                }
            }

            if (matched) {
                parsed.matchedFactories.add(factory);
            }
        }
        // 如果已解析出国家，且bestMatchFactoryNo与国家不匹配，则在matchedFactories中查找匹配国家的最佳厂号
        if (parsed.country != null && bestMatchFactoryNo != null) {
            boolean bestFactoryMatchesCountry = false;
            for (DictFactory f : parsed.matchedFactories) {
                if (f.getFactoryNo().equals(bestMatchFactoryNo) && f.getCountry().equals(parsed.country)) {
                    bestFactoryMatchesCountry = true;
                    break;
                }
            }
            if (!bestFactoryMatchesCountry) {
                // 查找匹配国家的厂号
                String countryFactoryNo = null;
                String countryFactoryInput = null;
                int countryMatchLen = 0;
                for (DictFactory f : parsed.matchedFactories) {
                    if (f.getCountry().equals(parsed.country)) {
                        String fNoNorm = f.getFactoryNo().replace(" ", "").toLowerCase();
                        String fPureNum = extractPureNumber(f.getFactoryNo());
                        int matchLen = keyword.contains(fNoNorm) ? fNoNorm.length() :
                                      (fPureNum != null && keyword.contains(fPureNum) ? fPureNum.length() : 0);
                        if (matchLen > countryMatchLen) {
                            countryMatchLen = matchLen;
                            countryFactoryNo = f.getFactoryNo();
                            countryFactoryInput = keyword.contains(fNoNorm) ? f.getFactoryNo() : fPureNum;
                        }
                    }
                }
                if (countryFactoryNo != null) {
                    bestMatchFactoryNo = countryFactoryNo;
                    bestMatchInput = countryFactoryInput;
                }
            }
        }
        if (bestMatchFactoryNo != null) {
            parsed.factoryNo = bestMatchFactoryNo;
            parsed.factoryNoInput = bestMatchInput;
        }

        // 3. 解析品牌（支持别名，大小写不敏感）
        for (DictBrand brand : allBrands) {
            // 检查标准品牌名（双向匹配：keyword包含品牌名 或 品牌名包含keyword）
            String brandNameNoSpace = brand.getBrandName().replace(" ", "");
            String keywordUpper = keyword.toUpperCase();
            String brandNameUpper = brandNameNoSpace.toUpperCase();
            if (keywordUpper.contains(brandNameUpper) || brandNameUpper.contains(keywordUpper)) {
                parsed.brandName = brand.getBrandName();
                parsed.brandInput = brand.getBrandName();
                parsed.brandAlias = null; // 用户输入的是标准名，没有别名
                break;
            }
            // 检查品牌别名
            if (brand.getAliasList() != null && !brand.getAliasList().isEmpty()) {
                String[] aliases = brand.getAliasList().split("[,，、]");
                // 优先精确匹配
                for (String alias : aliases) {
                    String aliasNoSpace = alias.replace(" ", "");
                    if (aliasNoSpace.equalsIgnoreCase(keyword)) {
                        parsed.brandName = brand.getBrandName();
                        parsed.brandInput = keyword; // 用户输入的原始关键词
                        parsed.brandAlias = alias.trim(); // 记录匹配到的别名
                        break;
                    }
                }
                // 如果没有精确匹配，再尝试前缀匹配
                if (parsed.brandName == null) {
                    for (String alias : aliases) {
                        String aliasNoSpace = alias.replace(" ", "");
                        String aliasUpper = aliasNoSpace.toUpperCase();
                        if (aliasUpper.startsWith(keywordUpper) ||
                            (keyword.length() <= aliasNoSpace.length() && aliasUpper.contains(keywordUpper))) {
                            parsed.brandName = brand.getBrandName();
                            parsed.brandInput = keyword; // 用户输入的原始关键词
                            parsed.brandAlias = alias.trim(); // 记录匹配到的别名
                            break;
                        }
                    }
                }
                if (parsed.brandName != null) break;
            }
        }

        // 4. 解析产品（支持别名，双向匹配，收集所有匹配的产品）
        // 重要：先检查别名，再检查产品名，避免"牛肉块"错误匹配到"碎肉"
        // 优先使用剩余关键词进行匹配（国家+产品场景，如"巴西牛前八件套"）
        String keywordForMatch = (parsed.country != null && parsed.remainingKeywordAfterCountry != null && !parsed.remainingKeywordAfterCountry.isEmpty())
            ? parsed.remainingKeywordAfterCountry
            : keyword;
        boolean hasCountryContext = (parsed.country != null);
        parsed.matchedProducts = new ArrayList<>();

        // 用于记录最佳匹配产品（优先选择最长匹配）
        DictProduct bestMatchProduct = null;
        int bestProductMatchLength = 0;
        String bestMatchAlias = null;

        for (DictProduct product : allProducts) {
            String productNameNoSpace = product.getProductName().replace(" ", "");
            String matchedAlias = null;
            int currentMatchLength = 0;

            // 有国家语境时，优先使用产品名匹配，跳过别名包含匹配
            if (!hasCountryContext) {
                // 无国家语境时，正常检查别名（精确匹配优先）
                if (product.getAliasList() != null && !product.getAliasList().isEmpty()) {
                    String[] aliases = product.getAliasList().split("[,，、]");
                    for (String alias : aliases) {
                        String aliasNoSpace = alias.replace(" ", "");
                        // 精确匹配：keywordForMatch等于alias
                        if (keywordForMatch.equals(aliasNoSpace)) {
                            matchedAlias = alias.trim();
                            currentMatchLength = aliasNoSpace.length() + 500;
                            break;
                        }
                    }
                    // 如果没有精确匹配，再尝试前缀匹配
                    if (matchedAlias == null) {
                        for (String alias : aliases) {
                            String aliasNoSpace = alias.replace(" ", "");
                            // alias以keywordForMatch开头，或 keywordForMatch是alias的前缀
                            if (aliasNoSpace.startsWith(keywordForMatch) ||
                                (keywordForMatch.length() <= aliasNoSpace.length() && aliasNoSpace.contains(keywordForMatch))) {
                                matchedAlias = alias.trim();
                                currentMatchLength = aliasNoSpace.length() + 500;
                                break;
                            }
                        }
                    }
                }
            }

            // 如果别名没匹配，检查产品名
            if (matchedAlias == null) {
                // 产品名精确匹配（关键词包含产品名，且长度相等）
                if (keywordForMatch.equals(productNameNoSpace)) {
                    matchedAlias = null;
                    currentMatchLength = productNameNoSpace.length() + 1000; // 最高优先级
                } else if (keywordForMatch.contains(productNameNoSpace) && productNameNoSpace.length() == keywordForMatch.length()) {
                    // 关键词包含产品名且长度相等，也是精确匹配
                    matchedAlias = null;
                    currentMatchLength = productNameNoSpace.length() + 900;
                } else if (keywordForMatch.contains(productNameNoSpace)) {
                    // 关键词包含产品名（前缀匹配）
                    matchedAlias = null;
                    currentMatchLength = productNameNoSpace.length() + 500;
                } else if (productNameNoSpace.contains(keywordForMatch)) {
                    // 产品名包含关键词（反向匹配），质量较低
                    matchedAlias = null;
                    currentMatchLength = productNameNoSpace.length();
                }
            }

            // 如果找到匹配，记录最长匹配，并收集到列表
            if (matchedAlias != null || currentMatchLength > 0) {
                parsed.matchedProducts.add(product);
                // 选择最长匹配作为最佳产品
                if (currentMatchLength > bestProductMatchLength) {
                    bestProductMatchLength = currentMatchLength;
                    bestMatchProduct = product;
                    bestMatchAlias = matchedAlias;
                }
            }
        }

        // 设置最佳匹配产品
        if (bestMatchProduct != null) {
            parsed.product = bestMatchProduct;
            parsed.productName = bestMatchProduct.getProductName();
            parsed.productInput = keywordForMatch;
            parsed.productAlias = bestMatchAlias;
            log.info("[DEBUG] 产品匹配完成: keywordForMatch={}, bestMatchProduct={}, bestMatchLength={}, bestMatchAlias={}",
                keywordForMatch, bestMatchProduct.getProductName(), bestProductMatchLength, bestMatchAlias);
        }

        // 5. 解析商家（支持模糊匹配 - 双向匹配）
        log.info("[DEBUG] 搜索关键词: {}, 商家总数: {}", keyword, allMerchants.size());
        for (DictMerchant merchant : allMerchants) {
            String merchantNameNoSpace = (merchant.getMerchantName() != null ? merchant.getMerchantName() : "").replace(" ", "");
            String merchantShortNameNoSpace = (merchant.getMerchantShortName() != null ? merchant.getMerchantShortName() : "").replace(" ", "");

            // 调试：打印每个商家的匹配情况
            log.info("[DEBUG] 检查商家: id={}, nameNoSpace={}, shortNameNoSpace={}, keyword={}",
                merchant.getMerchantId(), merchantNameNoSpace, merchantShortNameNoSpace, keyword);

            // 优先精确匹配简称
            if (merchantShortNameNoSpace.equals(keyword)) {
                log.info("[DEBUG]   匹配成功: 精确匹配简称, merchantName={}", merchant.getMerchantName());
                parsed.merchantName = merchant.getMerchantName();
                parsed.merchantInput = keyword;
                parsed.merchantAlias = merchant.getMerchantShortName();
                break;
            }
            // 精确匹配全称
            if (merchantNameNoSpace.equals(keyword)) {
                log.info("[DEBUG]   匹配成功: 精确匹配全称, merchantName={}", merchant.getMerchantName());
                parsed.merchantName = merchant.getMerchantName();
                parsed.merchantInput = keyword;
                parsed.merchantAlias = null;
                break;
            }
            // 双向模糊匹配（排除空字符串）
            if ((!merchantNameNoSpace.isEmpty() && merchantNameNoSpace.contains(keyword)) ||
                (!keyword.isEmpty() && keyword.contains(merchantNameNoSpace) && !merchantNameNoSpace.isEmpty()) ||
                (!merchantShortNameNoSpace.isEmpty() && merchantShortNameNoSpace.contains(keyword)) ||
                (!keyword.isEmpty() && keyword.contains(merchantShortNameNoSpace) && !merchantShortNameNoSpace.isEmpty())) {
                log.info("[DEBUG]   匹配成功: 模糊匹配, merchantName={}", merchant.getMerchantName());
                parsed.merchantName = merchant.getMerchantName();
                parsed.merchantInput = keyword;
                // 判断是否通过简称匹配（简称就是别名）
                if (!merchantShortNameNoSpace.isEmpty() && merchantShortNameNoSpace.contains(keyword)) {
                    parsed.merchantAlias = merchant.getMerchantShortName();
                } else if (!merchantNameNoSpace.isEmpty() && merchantNameNoSpace.contains(keyword)) {
                    parsed.merchantAlias = null;
                }
                break;
            }
        }

        return parsed;
    }

    /**
     * 提取纯数字
     */
    private String extractPureNumber(String factoryNo) {
        if (factoryNo == null) return null;
        String num = factoryNo.replaceAll("[^0-9]", "");
        return num.isEmpty() ? null : num;
    }

    /**
     * 校验厂号是否属于指定国家
     */
    private boolean checkFactoryCountryMatch(String category, String factoryNo, String country) {
        List<DictFactory> factories = factoryMapper.searchByFactoryNo(category, factoryNo);
        for (DictFactory factory : factories) {
            if (factory.getCountry().equals(country)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Priority 1: 厂号 + 产品
     */
    private List<SearchSuggestDTO> generateFactoryProductSuggestions(String category, String keyword,
                                                                     ParsedEntities parsed, Set<Integer> usedTargetIds) {
        List<SearchSuggestDTO> suggestions = new ArrayList<>();

        List<DictFactory> factories = factoryMapper.searchByFactoryNo(category, parsed.factoryNo);
        DictProduct product = parsed.product;

        if (product == null) return suggestions;

        for (DictFactory factory : factories) {
            // 再次校验一致性 - 如果parsed.country为null，则不校验
            if (parsed.country != null && !factory.getCountry().equals(parsed.country)) {
                continue;
            }

            SearchSuggestDTO dto = new SearchSuggestDTO();
            dto.setText(buildFactoryProductText(factory, product, parsed));
            dto.setKeyword(keyword);
            dto.setType("国家+厂号+产品");
            dto.setPriority(1);
            dto.setTargetId(factory.getFactoryId().longValue());
            dto.setMatchType("combined");
            dto.setInputKeyword(parsed.factoryNoInput);
            dto.setStandardName(factory.getFactoryNo());

            if (!usedTargetIds.contains(factory.getFactoryId())) {
                suggestions.add(dto);
                usedTargetIds.add(factory.getFactoryId());
            }
        }

        return suggestions;
    }

    /**
     * Priority 2: 厂号独立（无产品）
     */
    private List<SearchSuggestDTO> generateFactoryOnlySuggestions(String category, String keyword,
                                                                  ParsedEntities parsed, Set<Integer> usedTargetIds) {
        List<SearchSuggestDTO> suggestions = new ArrayList<>();

        List<DictFactory> factories;
        if (parsed.hasCountry() && parsed.matchedFactories != null) {
            // 使用解析时收集的所有匹配厂号，并筛选出属于已解析国家的工厂
            factories = new ArrayList<>();
            for (DictFactory f : parsed.matchedFactories) {
                if (f.getCountry().equals(parsed.country)) {
                    factories.add(f);
                }
            }
            // 按匹配度排序：匹配数字越多的排在前面
            final String kw = keyword.toLowerCase();
            factories.sort((f1, f2) -> {
                int len1 = getMatchLength(f1.getFactoryNo(), kw);
                int len2 = getMatchLength(f2.getFactoryNo(), kw);
                return Integer.compare(len2, len1); // 降序
            });
        } else if (parsed.hasCountry()) {
            factories = factoryMapper.findByCountryAndFactoryNo(category, parsed.country, parsed.factoryNo);
        } else {
            // 使用解析时收集的所有匹配厂号，并按匹配度排序
            factories = parsed.matchedFactories != null ? parsed.matchedFactories : new ArrayList<>();
            // 按匹配度排序：匹配数字越多的排在前面
            final String kw = keyword.toLowerCase();
            factories.sort((f1, f2) -> {
                int len1 = getMatchLength(f1.getFactoryNo(), kw);
                int len2 = getMatchLength(f2.getFactoryNo(), kw);
                return Integer.compare(len2, len1); // 降序
            });
        }

        for (DictFactory factory : factories) {
            // 如果同时有国家输入，校验一致性
            if (parsed.hasCountry() && !factory.getCountry().equals(parsed.country)) continue;

            SearchSuggestDTO dto = new SearchSuggestDTO();
            // 没有国家输入时，使用厂号自己的国家
            String displayCountry = parsed.hasCountry() ? buildCountryText(parsed) : factory.getCountry();
            String displayText = displayCountry.isEmpty() ? factory.getFactoryNo() : displayCountry + " " + factory.getFactoryNo();
            dto.setText(displayText);
            dto.setKeyword(keyword);
            dto.setType("国家+厂号");
            dto.setPriority(2);
            dto.setTargetId(factory.getFactoryId().longValue());
            dto.setMatchType("factory");
            dto.setInputKeyword(parsed.factoryNoInput);
            dto.setStandardName(factory.getFactoryNo());

            if (!usedTargetIds.contains(factory.getFactoryId())) {
                suggestions.add(dto);
                usedTargetIds.add(factory.getFactoryId());
            }
        }

        return suggestions;
    }

    /**
     * 计算厂号对搜索词的匹配度（匹配的数字位数）
     */
    private int getMatchLength(String factoryNo, String keyword) {
        String pureNumber = extractPureNumber(factoryNo);
        if (pureNumber != null && keyword.contains(pureNumber.toLowerCase())) {
            return pureNumber.length();
        }
        String normalized = factoryNo.replace(" ", "").toLowerCase();
        if (keyword.contains(normalized)) {
            return normalized.length();
        }
        return 0;
    }

    /**
     * Priority 3: 国家 + 产品（无厂号）
     * 用户期望：搜索"巴西牛腩"时，显示"巴西 牛腩"而不是展开到每个厂号
     */
    private List<SearchSuggestDTO> generateCountryProductSuggestions(String category, String keyword,
                                                                     ParsedEntities parsed, Set<Integer> usedTargetIds) {
        List<SearchSuggestDTO> suggestions = new ArrayList<>();

        DictProduct product = parsed.product;
        String countryText = buildCountryText(parsed);

        if (product == null) return suggestions;

        String productText = buildProductText(parsed);

        // 如果没有厂号（真正的国家+产品搜索），只生成一条简洁的结果
        if (!parsed.hasFactory()) {
            SearchSuggestDTO dto = new SearchSuggestDTO();
            dto.setText(countryText + " " + productText);
            dto.setKeyword(keyword);
            dto.setType("国家+产品");
            dto.setPriority(3);
            dto.setTargetId(0L); // 国家没有特定ID，使用0
            dto.setMatchType("combined");
            dto.setInputKeyword(parsed.productInput);
            dto.setStandardName(parsed.country + " " + product.getProductName());
            suggestions.add(dto);
            return suggestions;
        }

        // 有厂号的情况，为每个厂号生成一条建议（原逻辑）
        List<DictFactory> factories = factoryMapper.searchByCountry(category, parsed.country);
        for (DictFactory factory : factories) {
            SearchSuggestDTO dto = new SearchSuggestDTO();
            dto.setText(countryText + " " + factory.getFactoryNo() + " " + productText);
            dto.setKeyword(keyword);
            dto.setType("国家+厂号+产品");
            dto.setPriority(3);
            dto.setTargetId(factory.getFactoryId().longValue());
            dto.setMatchType("combined");
            dto.setInputKeyword(parsed.productInput);
            dto.setStandardName(product.getProductName());

            if (!usedTargetIds.contains(factory.getFactoryId())) {
                suggestions.add(dto);
                usedTargetIds.add(factory.getFactoryId());
            }
        }

        return suggestions;
    }

    /**
     * Priority 4: 品牌 + 产品
     */
    private List<SearchSuggestDTO> generateBrandProductSuggestions(String category, String keyword,
                                                                    ParsedEntities parsed, Set<Integer> usedTargetIds) {
        List<SearchSuggestDTO> suggestions = new ArrayList<>();

        // 收集所有匹配的品牌记录（一个品牌可能有多条记录，每个厂号一条）
        List<DictBrand> matchedBrands = new ArrayList<>();
        List<DictBrand> brands = brandMapper.searchByKeyword(parsed.brandInput);
        for (DictBrand b : brands) {
            if (b.getBrandName().equals(parsed.brandName)) {
                matchedBrands.add(b);
            }
        }
        if (matchedBrands.isEmpty()) return suggestions;

        DictProduct product = parsed.product;
        if (product == null) return suggestions;

        // 1. 品牌本身词条：品牌 + 产品（使用第一个匹配的品牌）
        DictBrand firstBrand = matchedBrands.get(0);
        SearchSuggestDTO brandDto = new SearchSuggestDTO();
        brandDto.setText(buildBrandProductText(firstBrand, product, parsed));
        brandDto.setKeyword(keyword);
        brandDto.setType("品牌+产品");
        brandDto.setPriority(4);
        brandDto.setTargetId(firstBrand.getBrandId().longValue());
        brandDto.setMatchType("brand");
        brandDto.setInputKeyword(parsed.brandInput);
        brandDto.setStandardName(firstBrand.getBrandName());
        suggestions.add(brandDto);

        // 2. 品牌下所有厂号的展开词条（收集所有匹配品牌的关联厂号）
        Set<Integer> allBrandIds = new HashSet<>();
        for (DictBrand brand : matchedBrands) {
            allBrandIds.add(brand.getBrandId());
        }
        List<DictFactory> allBrandFactories = new ArrayList<>();
        for (Integer brandId : allBrandIds) {
            List<DictFactory> factories = factoryMapper.selectByBrandId(brandId);
            allBrandFactories.addAll(factories);
        }
        DictProduct finalProduct = product;
        String finalBrandName = firstBrand.getBrandName();

        for (DictFactory factory : allBrandFactories) {
            if (!factory.getCategory().equals(category)) continue;
            // 过滤逻辑
            if (parsed.hasCountry() && !factory.getCountry().equals(parsed.country)) continue;
            if (parsed.hasFactory() && !factory.getFactoryNo().equals(parsed.factoryNo)) continue;

            SearchSuggestDTO dto = new SearchSuggestDTO();
            dto.setText(factory.getCountry() + " " + factory.getFactoryNo() + " " + finalProduct.getProductName());
            dto.setKeyword(keyword);
            dto.setType("国家+厂号+产品");
            dto.setPriority(4);
            dto.setTargetId(factory.getFactoryId().longValue());
            dto.setMatchType("combined");
            dto.setInputKeyword(parsed.brandInput);
            dto.setStandardName(finalBrandName);

            if (!usedTargetIds.contains(factory.getFactoryId())) {
                suggestions.add(dto);
                usedTargetIds.add(factory.getFactoryId());
            }
        }

        return suggestions;
    }

    /**
     * Priority 5: 单实体（未被组合使用）
     */
    private List<SearchSuggestDTO> generateSingleEntitySuggestions(String category, String keyword,
                                                                   ParsedEntities parsed, Set<Integer> usedTargetIds) {
        List<SearchSuggestDTO> suggestions = new ArrayList<>();

        // 检查国家是否已用于组合（包括国家+产品情况：有国家且有非空剩余关键词）
        boolean countryUsedInHigher = (parsed.hasCountry() && (parsed.hasFactory() || parsed.hasProduct() || (parsed.remainingKeywordAfterCountry != null && !parsed.remainingKeywordAfterCountry.isEmpty())));

        // 检查产品是否已用于组合（包括country+product和factory+product情况）
        boolean productUsedInHigher = (parsed.hasProduct() && (parsed.hasFactory() || parsed.hasCountry()));

        // 检查品牌是否已用于组合
        boolean brandUsedInHigher = (parsed.hasBrand() && parsed.hasProduct());

        // 单个国家的独立词条
        if (parsed.hasCountry() && !countryUsedInHigher) {
            SearchSuggestDTO dto = new SearchSuggestDTO();
            dto.setText(buildCountryText(parsed));
            dto.setKeyword(keyword);
            dto.setType("国家");
            dto.setPriority(5);
            dto.setTargetId(0L); // 国家没有特定ID
            dto.setMatchType("country");
            dto.setInputKeyword(parsed.countryInput);
            dto.setStandardName(parsed.country);
            suggestions.add(dto);
        }

        // 单个产品的独立词条（显示所有匹配的产品）
        if (parsed.hasProduct() && !productUsedInHigher && !brandUsedInHigher && parsed.matchedProducts != null) {
            for (DictProduct product : parsed.matchedProducts) {
                // 计算该产品的别名（如果有）
                String productAlias = null;
                if (product.getAliasList() != null && !product.getAliasList().isEmpty()) {
                    String[] aliases = product.getAliasList().split("[,，、]");
                    for (String alias : aliases) {
                        String aliasNoSpace = alias.replace(" ", "");
                        if (aliasNoSpace.startsWith(keyword) ||
                            (keyword.length() <= aliasNoSpace.length() && aliasNoSpace.contains(keyword))) {
                            productAlias = alias.trim();
                            break;
                        }
                    }
                }

                String displayName = productAlias != null ? product.getProductName() + "(别名：" + productAlias + ")" : product.getProductName();

                SearchSuggestDTO dto = new SearchSuggestDTO();
                dto.setText(displayName);
                dto.setKeyword(keyword);
                dto.setType("产品");
                dto.setPriority(5);
                dto.setTargetId(product.getProductId().longValue());
                dto.setMatchType("product");
                dto.setInputKeyword(parsed.productInput);
                dto.setStandardName(product.getProductName());
                dto.setAliasName(productAlias);
                suggestions.add(dto);
            }
        }

        // 单个品牌的独立词条（无产品）
        if (parsed.hasBrand() && !brandUsedInHigher) {
            DictBrand brand = null;
            List<DictBrand> brands = brandMapper.searchByKeyword(parsed.brandInput);
            for (DictBrand b : brands) {
                if (b.getBrandName().equals(parsed.brandName)) {
                    brand = b;
                    break;
                }
            }
            if (brand != null) {
                SearchSuggestDTO dto = new SearchSuggestDTO();
                dto.setText(buildBrandText(parsed));
                dto.setKeyword(keyword);
                dto.setType("品牌");
                dto.setPriority(5);
                dto.setTargetId(brand.getBrandId().longValue());
                dto.setMatchType("brand");
                dto.setInputKeyword(parsed.brandInput);
                dto.setStandardName(parsed.brandName);
                dto.setAliasName(parsed.brandAlias);
                suggestions.add(dto);
            }
        }

        return suggestions;
    }

    /**
     * Priority 6: 品牌（无产品）
     */
    private List<SearchSuggestDTO> generateBrandOnlySuggestions(String category, String keyword,
                                                                 ParsedEntities parsed, Set<Integer> usedTargetIds) {
        List<SearchSuggestDTO> suggestions = new ArrayList<>();

        // 收集所有匹配的品牌记录（一个品牌可能有多条记录，每个厂号一条）
        List<DictBrand> matchedBrands = new ArrayList<>();
        List<DictBrand> allBrands = brandMapper.searchByKeyword(parsed.brandInput);
        for (DictBrand b : allBrands) {
            if (b.getBrandName().equals(parsed.brandName)) {
                matchedBrands.add(b);
            }
        }
        if (matchedBrands.isEmpty()) return suggestions;

        // 收集所有关联的厂号（通过所有匹配品牌的 brandId 查询）
        Set<Integer> allBrandIds = new HashSet<>();
        List<DictFactory> allBrandFactories = new ArrayList<>();
        for (DictBrand brand : matchedBrands) {
            allBrandIds.add(brand.getBrandId());
        }
        // 通过 brand_ids 批量查询厂号
        for (Integer brandId : allBrandIds) {
            List<DictFactory> factories = factoryMapper.selectByBrandId(brandId);
            allBrandFactories.addAll(factories);
        }

        String finalBrandName2 = matchedBrands.get(0).getBrandName();
        String countryText = buildCountryText(parsed);

        for (DictFactory factory : allBrandFactories) {
            if (!factory.getCategory().equals(category)) continue;
            // 过滤逻辑
            if (parsed.hasCountry() && !factory.getCountry().equals(parsed.country)) continue;
            if (parsed.hasFactory() && !factory.getFactoryNo().equals(parsed.factoryNo)) continue;

            // 去重：优先级2已生成的厂号不再生成
            if (parsed.hasFactory() && factory.getFactoryNo().equals(parsed.factoryNo)) continue;

            SearchSuggestDTO dto = new SearchSuggestDTO();
            dto.setText(factory.getCountry() + " " + factory.getFactoryNo());
            dto.setKeyword(keyword);
            dto.setType("国家+厂号");
            dto.setPriority(6);
            dto.setTargetId(factory.getFactoryId().longValue());
            dto.setMatchType("factory");
            dto.setInputKeyword(parsed.brandInput);
            dto.setStandardName(finalBrandName2);

            if (!usedTargetIds.contains(factory.getFactoryId())) {
                suggestions.add(dto);
                usedTargetIds.add(factory.getFactoryId());
            }
        }

        return suggestions;
    }

    /**
     * Priority 7: 商家独立
     */
    private List<SearchSuggestDTO> generateMerchantSuggestions(String category, String keyword, ParsedEntities parsed) {
        List<SearchSuggestDTO> suggestions = new ArrayList<>();

        log.info("[DEBUG] generateMerchantSuggestions: parsed.merchantName={}, keyword={}", parsed.merchantName, keyword);
        // 使用原始keyword搜索，而不是parsed.merchantName（第一个匹配的商家全称）
        List<DictMerchant> merchants = merchantMapper.searchByName(keyword);
        log.info("[DEBUG] generateMerchantSuggestions: searchByName返回 {} 个商家", merchants.size());
        for (DictMerchant m : merchants) {
            log.info("[DEBUG]   商家: id={}, name={}", m.getMerchantId(), m.getMerchantName());
        }

        for (DictMerchant merchant : merchants) {
            // 为每个商家单独构建显示文本（使用该商家的实际名称，而非parsed.merchantName）
            String merchantDisplayName = buildMerchantDisplayName(merchant, parsed);
            SearchSuggestDTO dto = new SearchSuggestDTO();
            dto.setText(merchantDisplayName);
            dto.setKeyword(keyword);
            dto.setType("商家");
            dto.setPriority(7);
            log.info("[DEBUG]   设置targetId: merchantId={}", merchant.getMerchantId());
            dto.setTargetId(merchant.getMerchantId());
            dto.setMatchType("merchant");
            dto.setInputKeyword(parsed.merchantInput);
            dto.setStandardName(parsed.merchantName);
            dto.setAliasName(parsed.merchantAlias);
            log.info("[DEBUG]   DTO设置完成: targetId={}", dto.getTargetId());
            suggestions.add(dto);
        }
        log.info("[DEBUG] generateMerchantSuggestions返回: {} 条建议", suggestions.size());

        return suggestions;
    }

    /**
     * 构建厂号+产品的显示文本（处理别名显示）
     */
    private String buildFactoryProductText(DictFactory factory, DictProduct product, ParsedEntities parsed) {
        String countryText = parsed.hasCountry() ? buildCountryText(parsed) : factory.getCountry();
        String productText = buildProductText(parsed);
        return countryText + " " + factory.getFactoryNo() + " " + productText;
    }

    /**
     * 构建国家显示文本（处理别名）
     */
    private String buildCountryText(ParsedEntities parsed) {
        if (parsed.country == null) return "";
        if (parsed.countryAlias != null) {
            // 用户输入的是别名，显示"标准名(别名：别名)"
            return parsed.country + "(别名：" + parsed.countryAlias + ")";
        }
        return parsed.country;
    }

    /**
     * 构建产品显示文本（处理别名）
     */
    private String buildProductText(ParsedEntities parsed) {
        if (parsed.productAlias != null) {
            // 用户输入的是别名，显示"标准名(别名：别名)"
            return parsed.productName + "(别名：" + parsed.productAlias + ")";
        }
        return parsed.productName;
    }

    /**
     * 构建品牌显示文本（处理别名）
     */
    private String buildBrandText(ParsedEntities parsed) {
        if (parsed.brandAlias != null) {
            // 用户输入的是别名，显示"标准名(别名：别名)"
            return parsed.brandName + "(别名：" + parsed.brandAlias + ")";
        }
        return parsed.brandName;
    }

    /**
     * 构建商家显示文本（处理别名）
     */
    private String buildMerchantText(ParsedEntities parsed) {
        if (parsed.merchantAlias != null) {
            // 用户输入的是简称，显示"标准名(别名：简称)"
            return parsed.merchantName + "(别名：" + parsed.merchantAlias + ")";
        }
        return parsed.merchantName;
    }

    /**
     * 为单个商家构建显示文本（使用商家的实际名称，而非parsed.merchantName）
     */
    private String buildMerchantDisplayName(DictMerchant merchant, ParsedEntities parsed) {
        // 如果用户输入匹配了商家简称，则显示"标准名(别名：简称)"
        if (parsed.merchantAlias != null && merchant.getMerchantShortName() != null
                && parsed.merchantAlias.equals(merchant.getMerchantShortName())) {
            return merchant.getMerchantName() + "(别名：" + merchant.getMerchantShortName() + ")";
        }
        // 否则直接显示商家名称
        return merchant.getMerchantName();
    }

    /**
     * 构建品牌+产品的显示文本（处理别名显示）
     */
    private String buildBrandProductText(DictBrand brand, DictProduct product, ParsedEntities parsed) {
        String brandText = buildBrandText(parsed);
        String productText = buildProductText(parsed);
        return brandText + " " + productText;
    }

    @Override
    @CacheEvict(value = {"recentSearchCards", "selfSelectCards"}, allEntries = true)
    public void saveSearchHistory(Long userId, String searchWord, String searchType, Integer isSelfSelect,
                                   Long productId, String productName, String country, String factoryNo,
                                   Long brandId, Long merchantId) {
        // 如果 userId 为 null，使用默认用户ID 1
        if (userId == null) {
            userId = 1L;
        }
        // 如果 searchWord 为空，不保存
        if (searchWord == null || searchWord.trim().isEmpty()) {
            return;
        }
        // 如果 searchType 为空，设置为"未知"
        if (searchType == null || searchType.trim().isEmpty()) {
            searchType = "未知";
        }
        // 如果 isSelfSelect 为 null，设置为 0
        if (isSelfSelect == null) {
            isSelfSelect = 0;
        }

        try {
            // 先检查记录是否已存在（同时检查 userId, searchWord, searchType）
            Long existingId = searchHistoryMapper.findExistingHistory(userId, searchWord, searchType);
            if (existingId != null) {
                // 记录已存在，更新时间和详情
                searchHistoryMapper.updateCreateTime(existingId);
                log.info("[搜索历史] 更新记录: historyId={}, userId={}, searchWord={}, type={}",
                        existingId, userId, searchWord, searchType);
            } else {
                // 记录不存在，插入新记录
                searchHistoryMapper.insertOrUpdateFull(userId, searchWord, searchType, isSelfSelect,
                        productId, productName, country, factoryNo, brandId, merchantId);
                log.info("[搜索历史] 保存记录: userId={}, searchWord={}, type={}, isSelfSelect={}, productId={}, country={}, factoryNo={}",
                        userId, searchWord, searchType, isSelfSelect, productId, country, factoryNo);
            }
        } catch (Exception e) {
            log.error("[搜索历史] 保存失败: userId={}, searchWord={}, error={}", userId, searchWord, e.getMessage());
        }
    }
}
