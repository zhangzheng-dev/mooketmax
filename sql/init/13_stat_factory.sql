-- =============================================
-- 国家厂号维度统计表
-- =============================================

CREATE TABLE IF NOT EXISTS stat_factory (
    stat_id SERIAL PRIMARY KEY,
    stat_date DATE NOT NULL,                     -- 统计日期
    category VARCHAR(20) NOT NULL,               -- 大类：牛/猪
    country VARCHAR(50) NOT NULL,                 -- 国家
    factory_no VARCHAR(50) NOT NULL,             -- 厂号
    factory_id INT,                              -- 厂号ID
    today_offer_count INT DEFAULT 0,             -- 今日报盘数
    today_inquiry_count INT DEFAULT 0,           -- 今日求购数
    today_merchant_count INT DEFAULT 0,          -- 今日报盘商家数
    price_min DECIMAL(10,2),                     -- 今日最低价
    price_max DECIMAL(10,2),                     -- 今日最高价
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 主键：日期+厂号ID+分类
ALTER TABLE stat_factory ADD CONSTRAINT pk_stat_factory PRIMARY KEY (stat_date, factory_id, category);

-- 索引
CREATE INDEX idx_stat_factory_date ON stat_factory(stat_date);
CREATE INDEX idx_stat_factory_offer_count ON stat_factory(stat_date, today_offer_count DESC);

COMMENT ON TABLE stat_factory IS '国家厂号维度统计表';
COMMENT ON COLUMN stat_factory.stat_date IS '统计日期';
COMMENT ON COLUMN stat_factory.category IS '大类：牛/猪';
COMMENT ON COLUMN stat_factory.country IS '国家';
COMMENT ON COLUMN stat_factory.factory_no IS '厂号';
COMMENT ON COLUMN stat_factory.factory_id IS '厂号ID';
COMMENT ON COLUMN stat_factory.today_offer_count IS '今日报盘数';
COMMENT ON COLUMN stat_factory.today_inquiry_count IS '今日求购数';
COMMENT ON COLUMN stat_factory.today_merchant_count IS '今日报盘商家数';
COMMENT ON COLUMN stat_factory.price_min IS '今日最低价';
COMMENT ON COLUMN stat_factory.price_max IS '今日最高价';
