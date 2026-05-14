package com.mooket.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

import javax.sql.DataSource;

/**
 * 牧集 APP 后端服务启动类
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
@ConfigurationPropertiesScan
public class SocialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }

    /**
     * PostgreSQL 主数据源 - 作为默认数据源
     */
    @Bean(name = "primaryDataSource")
    @Primary
    public DataSource primaryDataSource(
            @Value("${mooket.datasource.primary.jdbc-url}") String jdbcUrl,
            @Value("${mooket.datasource.primary.username}") String username,
            @Value("${mooket.datasource.primary.password}") String password) {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    /**
     * 数据库初始化：stat_brand/stat_merchant/stat_factory_product 表添加 category 字段
     * 支持按牛/猪分类过滤（一次性迁移，幂等执行）
     */
    @Bean
    public org.springframework.boot.ApplicationRunner categoryMigrationRunner(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            migrateStatBrand(jdbc);
            migrateStatMerchant(jdbc);
            migrateStatFactoryProduct(jdbc);
        };
    }

    private void migrateStatBrand(JdbcTemplate jdbc) {
        try {
            // 检查列是否存在
            boolean colExists = jdbc.queryForList(
                "SELECT 1 FROM information_schema.columns WHERE table_name='stat_brand' AND column_name='category'").size() > 0;
            if (!colExists) {
                jdbc.execute("ALTER TABLE stat_brand ADD COLUMN category VARCHAR(20)");
                jdbc.execute("UPDATE stat_brand SET category = '牛' WHERE category IS NULL");
                jdbc.execute("ALTER TABLE stat_brand ALTER COLUMN category SET NOT NULL");
                jdbc.execute("ALTER TABLE stat_brand ALTER COLUMN category SET DEFAULT '牛'");
                System.out.println("[Migration] stat_brand.category added (nullable -> NOT NULL)");
            } else {
                System.out.println("[Migration] stat_brand.category already exists, skipping");
            }
            // 更新主键和索引
            try {
                jdbc.execute("ALTER TABLE stat_brand DROP CONSTRAINT IF EXISTS pk_stat_brand");
                jdbc.execute("ALTER TABLE stat_brand ADD CONSTRAINT pk_stat_brand PRIMARY KEY (stat_date, brand_id, category)");
            } catch (Exception e) {
                System.out.println("[Migration] stat_brand PK: " + e.getMessage());
            }
            try {
                jdbc.execute("DROP INDEX IF EXISTS idx_stat_brand_date");
                jdbc.execute("DROP INDEX IF EXISTS idx_stat_brand_offer_count");
                jdbc.execute("CREATE INDEX idx_stat_brand_date ON stat_brand(stat_date)");
                jdbc.execute("CREATE INDEX idx_stat_brand_offer_count ON stat_brand(stat_date, category, today_offer_count DESC)");
            } catch (Exception e) {
                System.out.println("[Migration] stat_brand indexes: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[Migration] stat_brand FAILED: " + e.getMessage());
        }
    }

    private void migrateStatMerchant(JdbcTemplate jdbc) {
        try {
            boolean colExists = jdbc.queryForList(
                "SELECT 1 FROM information_schema.columns WHERE table_name='stat_merchant' AND column_name='category'").size() > 0;
            if (!colExists) {
                jdbc.execute("ALTER TABLE stat_merchant ADD COLUMN category VARCHAR(20)");
                jdbc.execute("UPDATE stat_merchant SET category = '牛' WHERE category IS NULL");
                jdbc.execute("ALTER TABLE stat_merchant ALTER COLUMN category SET NOT NULL");
                jdbc.execute("ALTER TABLE stat_merchant ALTER COLUMN category SET DEFAULT '牛'");
                System.out.println("[Migration] stat_merchant.category added (nullable -> NOT NULL)");
            } else {
                System.out.println("[Migration] stat_merchant.category already exists, skipping");
            }
            try {
                jdbc.execute("ALTER TABLE stat_merchant DROP CONSTRAINT IF EXISTS uk_stat_date_merchant");
                jdbc.execute("ALTER TABLE stat_merchant ADD CONSTRAINT uk_stat_date_merchant UNIQUE (stat_date, merchant_id, category)");
            } catch (Exception e) {
                System.out.println("[Migration] stat_merchant UK: " + e.getMessage());
            }
            try {
                jdbc.execute("DROP INDEX IF EXISTS idx_stat_merchant_date");
                jdbc.execute("DROP INDEX IF EXISTS idx_stat_merchant");
                jdbc.execute("DROP INDEX IF EXISTS idx_stat_date_merchant");
                jdbc.execute("CREATE INDEX idx_stat_merchant_date ON stat_merchant(stat_date)");
                jdbc.execute("CREATE INDEX idx_stat_merchant_merchant ON stat_merchant(merchant_id)");
                jdbc.execute("CREATE INDEX idx_stat_merchant_date_merchant ON stat_merchant(stat_date, merchant_id, category)");
            } catch (Exception e) {
                System.out.println("[Migration] stat_merchant indexes: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[Migration] stat_merchant FAILED: " + e.getMessage());
        }
    }

    private void migrateStatFactoryProduct(JdbcTemplate jdbc) {
        try {
            boolean colExists = jdbc.queryForList(
                "SELECT 1 FROM information_schema.columns WHERE table_name='stat_factory_product' AND column_name='category'").size() > 0;
            if (!colExists) {
                jdbc.execute("ALTER TABLE stat_factory_product ADD COLUMN category VARCHAR(20)");
                jdbc.execute("UPDATE stat_factory_product SET category = '牛' WHERE category IS NULL");
                jdbc.execute("ALTER TABLE stat_factory_product ALTER COLUMN category SET NOT NULL");
                jdbc.execute("ALTER TABLE stat_factory_product ALTER COLUMN category SET DEFAULT '牛'");
                System.out.println("[Migration] stat_factory_product.category added (nullable -> NOT NULL)");
            } else {
                System.out.println("[Migration] stat_factory_product.category already exists, skipping");
            }
            try {
                jdbc.execute("ALTER TABLE stat_factory_product DROP CONSTRAINT IF EXISTS pk_stat_factory_product");
                jdbc.execute("ALTER TABLE stat_factory_product ADD CONSTRAINT pk_stat_factory_product PRIMARY KEY (stat_date, factory_id, product_id, category)");
            } catch (Exception e) {
                System.out.println("[Migration] stat_factory_product PK: " + e.getMessage());
            }
            try {
                jdbc.execute("DROP INDEX IF EXISTS idx_stat_factory_product_date");
                jdbc.execute("DROP INDEX IF EXISTS idx_stat_factory_product_offer_count");
                jdbc.execute("CREATE INDEX idx_stat_factory_product_date ON stat_factory_product(stat_date)");
                jdbc.execute("CREATE INDEX idx_stat_factory_product_offer_count ON stat_factory_product(stat_date, category, today_offer_count DESC)");
            } catch (Exception e) {
                System.out.println("[Migration] stat_factory_product indexes: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[Migration] stat_factory_product FAILED: " + e.getMessage());
        }
    }
}
