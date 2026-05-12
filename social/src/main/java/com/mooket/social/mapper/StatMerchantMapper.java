package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.StatMerchant;
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
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatMerchantMapper.class */
public interface StatMerchantMapper extends BaseMapper<StatMerchant> {

    /* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/StatMerchantMapper$HotMerchant.class */
    public static class HotMerchant {
        public Long merchantId;
        public Integer todayOfferCount;
    }

    @Insert({"<script>INSERT INTO stat_merchant (stat_date, merchant_id, today_offer_count, today_inquiry_count, today_product_count, today_factory_count, update_time) VALUES <foreach collection='list' item='item' separator=','>(#{item.statDate}, #{item.merchantId}, #{item.todayOfferCount}, #{item.todayInquiryCount}, #{item.todayProductCount}, #{item.todayFactoryCount}, #{item.updateTime})</foreach> ON CONFLICT (stat_date, merchant_id) DO UPDATE SET today_offer_count = EXCLUDED.today_offer_count, today_inquiry_count = EXCLUDED.today_inquiry_count, today_product_count = EXCLUDED.today_product_count, today_factory_count = EXCLUDED.today_factory_count, update_time = EXCLUDED.update_time</script>"})
    void batchUpsert(@Param("list") List<StatMerchant> stats);

    @Delete({"DELETE FROM stat_merchant WHERE stat_date = #{statDate}"})
    void deleteByDate(@Param("statDate") LocalDate statDate);

    @Delete({"DELETE FROM stat_merchant WHERE stat_date < CURRENT_DATE - INTERVAL '30 day'"})
    int deleteOldRecords();

    @Select({"SELECT merchant_id, today_offer_count FROM stat_merchant WHERE stat_date = #{statDate} AND today_offer_count >= 10 ORDER BY today_offer_count DESC LIMIT #{limit}"})
    @Results({@Result(property = "merchantId", column = "merchant_id"), @Result(property = "todayOfferCount", column = "today_offer_count")})
    List<HotMerchant> findHotMerchants(@Param("statDate") LocalDate statDate, @Param("limit") int limit);

    @Select({"SELECT stat_date, merchant_id, today_offer_count, today_inquiry_count, today_product_count, today_factory_count, update_time FROM stat_merchant WHERE merchant_id = #{merchantId} AND stat_date = #{statDate}"})
    StatMerchant selectByMerchantIdAndDate(@Param("merchantId") Long merchantId, @Param("statDate") LocalDate statDate);
}