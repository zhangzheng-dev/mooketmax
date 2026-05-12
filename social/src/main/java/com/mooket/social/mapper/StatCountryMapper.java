package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.StatCountry;
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
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatCountryMapper.class */
public interface StatCountryMapper extends BaseMapper<StatCountry> {

    /* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatCountryMapper$HotCountry.class */
    public static class HotCountry {
        public String country;
        public Integer todayOfferCount;
    }

    @Insert({"<script>INSERT INTO stat_country (stat_date, category, country, today_offer_count, today_inquiry_count, today_factory_count, today_merchant_count, hot_factories, hot_products, update_time) VALUES <foreach collection='list' item='item' separator=','>(#{item.statDate}, #{item.category}, #{item.country}, #{item.todayOfferCount}, #{item.todayInquiryCount}, #{item.todayFactoryCount}, #{item.todayMerchantCount}, #{item.hotFactories}, #{item.hotProducts}, #{item.updateTime})</foreach> ON CONFLICT (stat_date, country, category) DO UPDATE SET today_offer_count = EXCLUDED.today_offer_count, today_inquiry_count = EXCLUDED.today_inquiry_count, today_factory_count = EXCLUDED.today_factory_count, today_merchant_count = EXCLUDED.today_merchant_count, hot_factories = EXCLUDED.hot_factories, hot_products = EXCLUDED.hot_products, update_time = EXCLUDED.update_time</script>"})
    void batchUpsert(@Param("list") List<StatCountry> stats);

    @Delete({"DELETE FROM stat_country WHERE stat_date = #{statDate}"})
    void deleteByDate(@Param("statDate") LocalDate statDate);

    @Delete({"DELETE FROM stat_country WHERE stat_date < CURRENT_DATE - INTERVAL '30 day'"})
    int deleteOldRecords();

    @Select({"SELECT country, today_offer_count FROM stat_country WHERE stat_date = #{statDate} AND category = #{category} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "country", column = "country"), @Result(property = "todayOfferCount", column = "today_offer_count")})
    List<HotCountry> findHotCountries(@Param("statDate") LocalDate statDate, @Param("category") String category, @Param("limit") int limit);

    @Select({"SELECT stat_date, category, country, today_offer_count, today_inquiry_count, today_factory_count, today_merchant_count, hot_factories, hot_products, update_time FROM stat_country WHERE stat_date = #{statDate} AND category = #{category} ORDER BY today_offer_count DESC"})
    @Results({@Result(property = "statDate", column = "stat_date"), @Result(property = "category", column = "category"), @Result(property = "country", column = "country"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "todayMerchantCount", column = "today_merchant_count"), @Result(property = "hotFactories", column = "hot_factories"), @Result(property = "hotProducts", column = "hot_products"), @Result(property = "updateTime", column = "update_time")})
    List<StatCountry> selectByDateAndCategory(@Param("statDate") LocalDate statDate, @Param("category") String category);

    @Select({"SELECT stat_date, category, country, today_offer_count, today_inquiry_count, today_factory_count, today_merchant_count, hot_factories, hot_products, update_time FROM stat_country WHERE stat_date = CURRENT_DATE AND country = #{country} AND category = #{category} ORDER BY stat_date DESC LIMIT 1"})
    @Results({@Result(property = "statDate", column = "stat_date"), @Result(property = "category", column = "category"), @Result(property = "country", column = "country"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "todayMerchantCount", column = "today_merchant_count"), @Result(property = "hotFactories", column = "hot_factories"), @Result(property = "hotProducts", column = "hot_products"), @Result(property = "updateTime", column = "update_time")})
    StatCountry selectByCountryAndCategory(@Param("country") String country, @Param("category") String category);

    @Select({"SELECT stat_date, category, country, today_offer_count, today_inquiry_count, today_factory_count, today_merchant_count, hot_factories, hot_products, update_time FROM stat_country WHERE stat_date = CURRENT_DATE AND country LIKE '%' || #{country} || '%' AND category = #{category} ORDER BY stat_date DESC LIMIT 1"})
    @Results({@Result(property = "statDate", column = "stat_date"), @Result(property = "category", column = "category"), @Result(property = "country", column = "country"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "todayMerchantCount", column = "today_merchant_count"), @Result(property = "hotFactories", column = "hot_factories"), @Result(property = "hotProducts", column = "hot_products"), @Result(property = "updateTime", column = "update_time")})
    StatCountry selectByCountryKeyword(@Param("country") String country, @Param("category") String category);
}