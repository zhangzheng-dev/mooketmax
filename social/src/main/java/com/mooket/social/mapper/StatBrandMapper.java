package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.StatBrand;
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
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatBrandMapper.class */
public interface StatBrandMapper extends BaseMapper<StatBrand> {

    /* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatBrandMapper$HotBrand.class */
    public static class HotBrand {
        public Integer brandId;
        public String brandName;
        public Integer todayOfferCount;
        public Integer factoryCount;
        public Integer productCount;
    }

    @Insert({"<script>INSERT INTO stat_brand (stat_date, brand_id, brand_name, today_offer_count, today_factory_count, today_product_count, price_min, price_max, update_time) VALUES <foreach collection='list' item='item' separator=','>(#{item.statDate}, #{item.brandId}, #{item.brandName}, #{item.todayOfferCount}, #{item.todayFactoryCount}, #{item.todayProductCount}, #{item.priceMin}, #{item.priceMax}, #{item.updateTime})</foreach> ON CONFLICT (stat_date, brand_id) DO UPDATE SET today_offer_count = EXCLUDED.today_offer_count, today_factory_count = EXCLUDED.today_factory_count, today_product_count = EXCLUDED.today_product_count, price_min = EXCLUDED.price_min, price_max = EXCLUDED.price_max, update_time = EXCLUDED.update_time</script>"})
    void batchUpsert(@Param("list") List<StatBrand> stats);

    @Delete({"DELETE FROM stat_brand WHERE stat_date = #{statDate}"})
    void deleteByDate(@Param("statDate") LocalDate statDate);

    @Delete({"DELETE FROM stat_brand WHERE stat_date < CURRENT_DATE - INTERVAL '30 day'"})
    int deleteOldRecords();

    @Select({"SELECT brand_id, brand_name, today_offer_count, today_factory_count, today_product_count FROM stat_brand WHERE stat_date = #{statDate} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "brandId", column = "brand_id"), @Result(property = "brandName", column = "brand_name"), @Result(property = "todayOfferCount", column = "today_offer_count"), @Result(property = "factoryCount", column = "today_factory_count"), @Result(property = "productCount", column = "today_product_count")})
    List<HotBrand> findHotBrands(@Param("statDate") LocalDate statDate, @Param("limit") int limit);
}