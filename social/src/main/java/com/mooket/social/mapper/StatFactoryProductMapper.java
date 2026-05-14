package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.StatFactoryProduct;
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
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatFactoryProductMapper.class */
public interface StatFactoryProductMapper extends BaseMapper<StatFactoryProduct> {
    @Insert({"<script>INSERT INTO stat_factory_product (stat_date, factory_id, factory_no, country, product_id, product_name, category, today_offer_count, today_inquiry_count, price_min, price_max, avg_price, avg_price_yesterday, price_change, price_change_rate, update_time) VALUES <foreach collection='list' item='item' separator=','>(#{item.statDate}, #{item.factoryId}, #{item.factoryNo}, #{item.country}, #{item.productId}, #{item.productName}, #{item.category}, #{item.todayOfferCount}, #{item.todayInquiryCount}, #{item.priceMin}, #{item.priceMax}, #{item.avgPrice}, #{item.avgPriceYesterday}, #{item.priceChange}, #{item.priceChangeRate}, #{item.updateTime})</foreach> ON CONFLICT (stat_date, factory_id, product_id, category) DO UPDATE SET today_offer_count = EXCLUDED.today_offer_count, today_inquiry_count = EXCLUDED.today_inquiry_count, price_min = EXCLUDED.price_min, price_max = EXCLUDED.price_max, avg_price = EXCLUDED.avg_price, avg_price_yesterday = EXCLUDED.avg_price_yesterday, price_change = EXCLUDED.price_change, price_change_rate = EXCLUDED.price_change_rate, update_time = EXCLUDED.update_time</script>"})
    void batchUpsert(@Param("list") List<StatFactoryProduct> stats);

    @Delete({"DELETE FROM stat_factory_product WHERE stat_date = #{statDate}"})
    void deleteByDate(@Param("statDate") LocalDate statDate);

    @Delete({"DELETE FROM stat_factory_product WHERE stat_date < CURRENT_DATE - INTERVAL '30 day'"})
    int deleteOldRecords();

    @Select({"SELECT factory_id, factory_no, country, product_id, product_name, today_offer_count, today_inquiry_count, price_min, price_max, avg_price, avg_price_yesterday, price_change, price_change_rate FROM stat_factory_product WHERE stat_date = #{statDate} AND category = #{category} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "factoryId", column = "factory_id"), @Result(property = "factoryNo", column = "factory_no"), @Result(property = "country", column = "country"), @Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max"), @Result(property = "avgPrice", column = "avg_price"), @Result(property = "avgPriceYesterday", column = "avg_price_yesterday"), @Result(property = "priceChange", column = "price_change"), @Result(property = "priceChangeRate", column = "price_change_rate")})
    List<StatFactoryProduct> findHotFactoryProducts(@Param("statDate") LocalDate statDate, @Param("category") String category, @Param("limit") int limit);

    @Select({"SELECT factory_id, factory_no, country, product_id, product_name, today_offer_count, price_min, price_max FROM stat_factory_product WHERE stat_date = #{statDate} AND factory_id = #{factoryId} AND category = #{category} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "factoryId", column = "factory_id"), @Result(property = "factoryNo", column = "factory_no"), @Result(property = "country", column = "country"), @Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max")})
    List<StatFactoryProduct> findHotProductsByFactory(@Param("statDate") LocalDate statDate, @Param("factoryId") Integer factoryId, @Param("category") String category, @Param("limit") int limit);

    @Select({"SELECT factory_id, factory_no, country, product_id, product_name, today_offer_count, today_inquiry_count, price_min, price_max, avg_price, avg_price_yesterday, price_change, price_change_rate FROM stat_factory_product WHERE stat_date = CURRENT_DATE AND factory_no = #{factoryNo} AND product_id = #{productId} AND category = #{category} ORDER BY stat_date DESC LIMIT 1"})
    @Results({@Result(property = "factoryId", column = "factory_id"), @Result(property = "factoryNo", column = "factory_no"), @Result(property = "country", column = "country"), @Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max"), @Result(property = "avgPrice", column = "avg_price"), @Result(property = "avgPriceYesterday", column = "avg_price_yesterday"), @Result(property = "priceChange", column = "price_change"), @Result(property = "priceChangeRate", column = "price_change_rate")})
    StatFactoryProduct selectByFactoryNoAndProductId(@Param("factoryNo") String factoryNo, @Param("productId") Integer productId, @Param("category") String category);
}
