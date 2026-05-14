-- =============================================
-- 为 stat_brand, stat_merchant, stat_factory_product 表添加 category 字段
-- 用于支持按牛/猪分类过滤
-- =============================================

-- 1. stat_brand 添加 category 字段
ALTER TABLE stat_brand ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT '牛';
ALTER TABLE stat_brand DROP CONSTRAINT IF EXISTS pk_stat_brand;
ALTER TABLE stat_brand ADD CONSTRAINT pk_stat_brand PRIMARY KEY (stat_date, brand_id, category);
DROP INDEX IF EXISTS idx_stat_brand_date;
DROP INDEX IF EXISTS idx_stat_brand_offer_count;
CREATE INDEX idx_stat_brand_date ON stat_brand(stat_date);
CREATE INDEX idx_stat_brand_offer_count ON stat_brand(stat_date, category, today_offer_count DESC);

-- 2. stat_merchant 添加 category 字段
ALTER TABLE stat_merchant ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT '牛';
ALTER TABLE stat_merchant DROP CONSTRAINT IF EXISTS uk_stat_date_merchant;
ALTER TABLE stat_merchant ADD CONSTRAINT uk_stat_date_merchant UNIQUE (stat_date, merchant_id, category);
DROP INDEX IF EXISTS idx_stat_date;
DROP INDEX IF EXISTS idx_stat_merchant;
DROP INDEX IF EXISTS idx_stat_date_merchant;
CREATE INDEX idx_stat_merchant_date ON stat_merchant(stat_date);
CREATE INDEX idx_stat_merchant_merchant ON stat_merchant(merchant_id);
CREATE INDEX idx_stat_merchant_date_merchant ON stat_merchant(stat_date, merchant_id, category);

-- 3. stat_factory_product 添加 category 字段
ALTER TABLE stat_factory_product ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT '牛';
ALTER TABLE stat_factory_product DROP CONSTRAINT IF EXISTS pk_stat_factory_product;
ALTER TABLE stat_factory_product ADD CONSTRAINT pk_stat_factory_product PRIMARY KEY (stat_date, factory_id, product_id, category);
DROP INDEX IF EXISTS idx_stat_factory_product_date;
DROP INDEX IF EXISTS idx_stat_factory_product_offer_count;
CREATE INDEX idx_stat_factory_product_date ON stat_factory_product(stat_date);
CREATE INDEX idx_stat_factory_product_offer_count ON stat_factory_product(stat_date, category, today_offer_count DESC);
