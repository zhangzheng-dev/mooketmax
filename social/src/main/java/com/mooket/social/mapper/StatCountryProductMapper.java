package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.StatCountryProduct;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatCountryProductMapper.class */
public interface StatCountryProductMapper extends BaseMapper<StatCountryProduct> {
    @Insert({"<script>INSERT INTO stat_country_product (stat_date, category, country, product_id, product_name, today_offer_count, today_inquiry_count, today_factory_count, price_min, price_max, avg_price, avg_price_yesterday, price_change, price_change_rate, update_time) VALUES <foreach collection='list' item='item' separator=','>(#{item.statDate}, #{item.category}, #{item.country}, #{item.productId}, #{item.productName}, #{item.todayOfferCount}, #{item.todayInquiryCount}, #{item.todayFactoryCount}, #{item.priceMin}, #{item.priceMax}, #{item.avgPrice}, #{item.avgPriceYesterday}, #{item.priceChange}, #{item.priceChangeRate}, #{item.updateTime})</foreach> ON CONFLICT (stat_date, country, product_id) DO UPDATE SET today_offer_count = EXCLUDED.today_offer_count, today_inquiry_count = EXCLUDED.today_inquiry_count, today_factory_count = EXCLUDED.today_factory_count, price_min = EXCLUDED.price_min, price_max = EXCLUDED.price_max, avg_price = EXCLUDED.avg_price, avg_price_yesterday = EXCLUDED.avg_price_yesterday, price_change = EXCLUDED.price_change, price_change_rate = EXCLUDED.price_change_rate, update_time = EXCLUDED.update_time</script>"})
    void batchUpsert(@Param("list") List<StatCountryProduct> stats);

    @Delete({"DELETE FROM stat_country_product WHERE stat_date = #{statDate}"})
    void deleteByDate(@Param("statDate") LocalDate statDate);

    @Delete({"DELETE FROM stat_country_product WHERE stat_date < CURRENT_DATE - INTERVAL '30 day'"})
    int deleteOldRecords();

    @Select({"SELECT country, product_id, product_name, today_offer_count, today_factory_count, today_inquiry_count, price_min, price_max, avg_price, avg_price_yesterday, price_change, price_change_rate FROM stat_country_product WHERE stat_date = #{statDate} AND category = #{category} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "country", column = "country"), @Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max"), @Result(property = "avgPrice", column = "avg_price"), @Result(property = "avgPriceYesterday", column = "avg_price_yesterday"), @Result(property = "priceChange", column = "price_change"), @Result(property = "priceChangeRate", column = "price_change_rate")})
    List<StatCountryProduct> findHotCountryProducts(@Param("statDate") LocalDate statDate, @Param("category") String category, @Param("limit") int limit);

    @Select({"SELECT country, product_id, product_name, today_offer_count, today_factory_count, price_min, price_max FROM stat_country_product WHERE stat_date = #{statDate} AND category = #{category} AND country = #{country} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "country", column = "country"), @Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max")})
    List<StatCountryProduct> findHotProductsByCountry(@Param("statDate") LocalDate statDate, @Param("category") String category, @Param("country") String country, @Param("limit") int limit);

    @Select({"SELECT country, product_id, product_name, today_offer_count, today_inquiry_count, today_factory_count, price_min, price_max, avg_price, avg_price_yesterday, price_change, price_change_rate FROM stat_country_product WHERE stat_date = CURRENT_DATE AND country = #{country} AND product_id = #{productId} AND category = #{category} ORDER BY stat_date DESC LIMIT 1"})
    @Results({@Result(property = "country", column = "country"), @Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max"), @Result(property = "avgPrice", column = "avg_price"), @Result(property = "avgPriceYesterday", column = "avg_price_yesterday"), @Result(property = "priceChange", column = "price_change"), @Result(property = "priceChangeRate", column = "price_change_rate")})
    StatCountryProduct selectByCountryAndProductId(@Param("country") String country, @Param("productId") Integer productId, @Param("category") String category);
}