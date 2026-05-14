package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.StatFactory;
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
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatFactoryMapper.class */
public interface StatFactoryMapper extends BaseMapper<StatFactory> {

    /* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatFactoryMapper$HotFactory.class */
    public static class HotFactory {
        public String country;
        public String factoryNo;
        public Integer factoryId;
        public Integer todayOfferCount;
    }

    @Insert({"<script>INSERT INTO stat_factory (stat_date, category, country, factory_no, factory_id, today_offer_count, today_inquiry_count, today_merchant_count, price_min, price_max, update_time) VALUES <foreach collection='list' item='item' separator=','>(#{item.statDate}, #{item.category}, #{item.country}, #{item.factoryNo}, #{item.factoryId}, #{item.todayOfferCount}, #{item.todayInquiryCount}, #{item.todayMerchantCount}, #{item.priceMin}, #{item.priceMax}, #{item.updateTime})</foreach> ON CONFLICT (stat_date, factory_id, category) DO UPDATE SET today_offer_count = EXCLUDED.today_offer_count, today_inquiry_count = EXCLUDED.today_inquiry_count, today_merchant_count = EXCLUDED.today_merchant_count, price_min = EXCLUDED.price_min, price_max = EXCLUDED.price_max, update_time = EXCLUDED.update_time</script>"})
    void batchUpsert(@Param("list") List<StatFactory> stats);

    @Delete({"DELETE FROM stat_factory WHERE stat_date = #{statDate}"})
    void deleteByDate(@Param("statDate") LocalDate statDate);

    @Delete({"DELETE FROM stat_factory WHERE stat_date < CURRENT_DATE - INTERVAL '30 day'"})
    int deleteOldRecords();

    @Select({"SELECT country, factory_no, factory_id, today_offer_count FROM stat_factory WHERE stat_date = #{statDate} AND category = #{category} AND country = #{country} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "country", column = "country"), @Result(property = "factoryNo", column = "factory_no"), @Result(property = "factoryId", column = "factory_id"), @Result(property = "todayOfferCount", column = "today_offer_count")})
    List<HotFactory> findHotFactoriesByCountry(@Param("statDate") LocalDate statDate, @Param("category") String category, @Param("country") String country, @Param("limit") int limit);

    @Select({"SELECT country, factory_no, factory_id, today_offer_count FROM stat_factory WHERE stat_date = #{statDate} AND category = #{category} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "country", column = "country"), @Result(property = "factoryNo", column = "factory_no"), @Result(property = "factoryId", column = "factory_id"), @Result(property = "todayOfferCount", column = "today_offer_count")})
    List<HotFactory> findHotFactories(@Param("statDate") LocalDate statDate, @Param("category") String category, @Param("limit") int limit);

    @Select({"SELECT stat_date, category, country, factory_no, factory_id, today_offer_count, today_inquiry_count, today_merchant_count, price_min, price_max, update_time FROM stat_factory WHERE stat_date = CURRENT_DATE AND factory_no = #{factoryNo} AND category = #{category} ORDER BY stat_date DESC LIMIT 1"})
    @Results({@Result(property = "statDate", column = "stat_date"), @Result(property = "category", column = "category"), @Result(property = "country", column = "country"), @Result(property = "factoryNo", column = "factory_no"), @Result(property = "factoryId", column = "factory_id"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "todayMerchantCount", column = "today_merchant_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max"), @Result(property = "updateTime", column = "update_time")})
    StatFactory selectByFactoryNoAndCategory(@Param("factoryNo") String factoryNo, @Param("category") String category);
}