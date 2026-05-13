-- =============================================
-- 修复 stat_brand_product 的 avg_price_yesterday 历史数据
-- 根因：之前 aggregateByBrandProduct 昨日无数据时 avgPriceYesterday=null，
--        但 selectAggregatedByBrandNameAndProductName 用 MAX() 聚合后变成 0
-- 修复：按 (brand_name, product_name, stat_date-1) 聚合 biz_offer 重新计算昨日均价，回填
-- =============================================

-- 1. 先看看 stat_brand_product 中有多少条 avg_price_yesterday = 0 且今日有报价
SELECT count(*) FROM stat_brand_product
WHERE stat_date = CURRENT_DATE
  AND avg_price_yesterday = 0
  AND today_offer_count > 0;

-- 2. 修复：更新今日记录，用昨日的 stat_brand_product.avg_price 回填
--    前提：昨日有对应 brand_name+product_name 的记录
UPDATE stat_brand_product t
SET avg_price_yesterday = y.avg_price,
    price_change = CASE
        WHEN y.avg_price > 0 THEN ROUND(t.avg_price - y.avg_price, 4)
        ELSE 0
    END,
    price_change_rate = CASE
        WHEN y.avg_price > 0 THEN ROUND((t.avg_price - y.avg_price) / y.avg_price * 100, 2)
        ELSE 0
    END,
    update_time = CURRENT_TIMESTAMP
FROM stat_brand_product y
WHERE t.stat_date = CURRENT_DATE
  AND y.stat_date = CURRENT_DATE - 1
  AND REPLACE(t.brand_name, ' ', '') = REPLACE(y.brand_name, ' ', '')
  AND REPLACE(t.product_name, ' ', '') = REPLACE(y.product_name, ' ', '')
  AND t.avg_price_yesterday = 0
  AND t.today_offer_count > 0
  AND y.avg_price > 0;

-- 3. 检查修复结果
SELECT stat_date, brand_name, product_name,
       avg_price, avg_price_yesterday, price_change, price_change_rate, today_offer_count
FROM stat_brand_product
WHERE stat_date = CURRENT_DATE
  AND avg_price_yesterday != 0
ORDER BY today_offer_count DESC
LIMIT 10;

-- 4. 对于 stat_date = CURRENT_DATE - 1 的记录（昨天的数据，作为今日的昨日均价来源）
--    也要确保 avg_price_yesterday 正确（如果昨日无报价应为0或不更新）
--    这类记录本身就是昨日均价，不需要修复
