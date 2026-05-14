package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.StatProduct;
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
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatProductMapper.class */
public interface StatProductMapper extends BaseMapper<StatProduct> {

    /* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatProductMapper$HotProduct.class */
    public static class HotProduct {
        public Integer productId;
        public String productName;
        public Integer todayOfferCount;
    }

    @Insert({"<script>INSERT INTO stat_product (stat_date, category, product_id, product_name, today_offer_count, today_inquiry_count, today_merchant_count, today_factory_count, price_min, price_max, update_time) VALUES <foreach collection='list' item='item' separator=','>(#{item.statDate}, #{item.category}, #{item.productId}, #{item.productName}, #{item.todayOfferCount}, #{item.todayInquiryCount}, #{item.todayMerchantCount}, #{item.todayFactoryCount}, #{item.priceMin}, #{item.priceMax}, #{item.updateTime})</foreach> ON CONFLICT (stat_date, product_id) DO UPDATE SET category = EXCLUDED.category, today_offer_count = EXCLUDED.today_offer_count, today_inquiry_count = EXCLUDED.today_inquiry_count, today_merchant_count = EXCLUDED.today_merchant_count, today_factory_count = EXCLUDED.today_factory_count, price_min = EXCLUDED.price_min, price_max = EXCLUDED.price_max, update_time = EXCLUDED.update_time</script>"})
    void batchUpsert(@Param("list") List<StatProduct> stats);

    @Delete({"DELETE FROM stat_product WHERE stat_date = #{statDate}"})
    void deleteByDate(@Param("statDate") LocalDate statDate);

    @Delete({"DELETE FROM stat_product WHERE stat_date < CURRENT_DATE - INTERVAL '30 day'"})
    int deleteOldRecords();

    @Select({"SELECT product_id, product_name, today_offer_count FROM stat_product WHERE stat_date = #{statDate} AND category = #{category} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count")})
    List<HotProduct> findHotProducts(@Param("statDate") LocalDate statDate, @Param("category") String category, @Param("limit") int limit);

    @Select({"SELECT product_id, product_name, today_offer_count, today_inquiry_count, today_merchant_count, today_factory_count, price_min, price_max FROM stat_product WHERE stat_date = #{statDate} AND category = #{category} ORDER BY today_offer_count DESC"})
    @Results({@Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "todayMerchantCount", column = "today_merchant_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max")})
    List<StatProduct> selectByDateAndCategory(@Param("statDate") LocalDate statDate, @Param("category") String category);

    @Select({"SELECT product_id, product_name, today_offer_count, today_inquiry_count, today_merchant_count, today_factory_count, price_min, price_max FROM stat_product WHERE stat_date = CURRENT_DATE AND product_id = #{productId} AND category = #{category} ORDER BY stat_date DESC LIMIT 1"})
    @Results({@Result(property = "productId", column = "product_id"), @Result(property = "productName", column = "product_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "todayInquiryCount", column = "today_inquiry_count"), @Result(property = "todayMerchantCount", column = "today_merchant_count"), @Result(property = "todayFactoryCount", column = "today_factory_count"), @Result(property = "priceMin", column = "price_min"), @Result(property = "priceMax", column = "price_max")})
    StatProduct selectByProductIdAndCategory(@Param("productId") Integer productId, @Param("category") String category);
}