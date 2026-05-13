package com.mooket.social.controller;

import com.mooket.social.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 管理接口
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;

    public AdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建索引（用于优化查询性能）
     */
    @PostMapping("/create-indexes")
    public ApiResponse<String> createIndexes() {
        try {
            // 产品详情查询优化索引 - 覆盖主要查询条件
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_biz_offer_product_query " +
                    "ON biz_offer(product_id, category, offer_type, status, data_date)");

            // 聚合查询索引 - 包含分组字段
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_biz_offer_group_agg " +
                    "ON biz_offer(product_id, category, offer_type, status, data_date, country, factory_no)");

            return ApiResponse.success("索引创建成功");
        } catch (Exception e) {
            return ApiResponse.error("创建索引失败: " + e.getMessage());
        }
    }

    /**
     * 回填 stat_brand_product 的 avg_price_yesterday 历史数据
     */
    @PostMapping("/fix-stat-brand-product-yesterday")
    public ApiResponse<String> fixStatBrandProductYesterday() {
        try {
            int updated = jdbcTemplate.update("""
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
                  AND y.avg_price > 0
                """);
            return ApiResponse.success("回填成功，影响行数: " + updated);
        } catch (Exception e) {
            return ApiResponse.error("回填失败: " + e.getMessage());
        }
    }
}
