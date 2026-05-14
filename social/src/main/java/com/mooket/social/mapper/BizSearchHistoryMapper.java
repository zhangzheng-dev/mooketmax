package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.BizSearchHistory;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/BizSearchHistoryMapper.class */
public interface BizSearchHistoryMapper extends BaseMapper<BizSearchHistory> {
    @Select({"SELECT * FROM biz_search_history WHERE user_id = #{userId} AND is_self_select = 0 ORDER BY create_time DESC LIMIT #{limit}"})
    List<BizSearchHistory> findRecentSearches(@Param("userId") Long userId, @Param("limit") int limit);

    @Select({"SELECT * FROM biz_search_history WHERE user_id = #{userId} AND is_self_select = 1 ORDER BY create_time DESC LIMIT #{limit}"})
    List<BizSearchHistory> findSelfSelectSearches(@Param("userId") Long userId, @Param("limit") int limit);

    @Select({"SELECT history_id FROM biz_search_history WHERE user_id = #{userId} AND search_word = #{searchWord} AND search_type = #{searchType} LIMIT 1"})
    Long findExistingHistory(@Param("userId") Long userId, @Param("searchWord") String searchWord, @Param("searchType") String searchType);

    @Update({"UPDATE biz_search_history SET create_time = CURRENT_TIMESTAMP WHERE history_id = #{historyId}"})
    void updateCreateTime(@Param("historyId") Long historyId);

    @Update({"UPDATE biz_search_history SET create_time = CURRENT_TIMESTAMP, ",
            "is_self_select = #{isSelfSelect}, ",
            "product_id = COALESCE(#{productId}, product_id), ",
            "product_name = COALESCE(#{productName}, product_name), ",
            "country = COALESCE(#{country}, country), ",
            "factory_no = COALESCE(#{factoryNo}, factory_no), ",
            "brand_id = COALESCE(#{brandId}, brand_id), ",
            "merchant_id = COALESCE(#{merchantId}, merchant_id) ",
            "WHERE history_id = #{historyId}"})
    void updateCreateTimeAndDetails(@Param("historyId") Long historyId,
                                    @Param("isSelfSelect") Integer isSelfSelect,
                                    @Param("productId") Long productId,
                                    @Param("productName") String productName,
                                    @Param("country") String country,
                                    @Param("factoryNo") String factoryNo,
                                    @Param("brandId") Long brandId,
                                    @Param("merchantId") Long merchantId);

    @Delete({"DELETE FROM biz_search_history WHERE history_id = #{historyId}"})
    void deleteById(@Param("historyId") Long historyId);

    @Delete({"DELETE FROM biz_search_history WHERE user_id = #{userId} AND is_self_select = 1"})
    void deleteAllSelfSelect(@Param("userId") Long userId);

    @Delete({"<script>DELETE FROM biz_search_history WHERE history_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>"})
    void batchDelete(@Param("ids") List<Long> ids);

    @Insert({"INSERT INTO biz_search_history (user_id, search_word, search_type, is_self_select, create_time) VALUES (#{userId}, #{searchWord}, #{searchType}, #{isSelfSelect}, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING"})
    void insertOrIgnore(@Param("userId") Long userId, @Param("searchWord") String searchWord, @Param("searchType") String searchType, @Param("isSelfSelect") Integer isSelfSelect);

    @Insert({"INSERT INTO biz_search_history ",
            "(user_id, search_word, search_type, is_self_select, create_time, product_id, product_name, country, factory_no, brand_id, merchant_id) ",
            "VALUES (#{userId}, #{searchWord}, #{searchType}, #{isSelfSelect}, CURRENT_TIMESTAMP, ",
            "#{productId}, #{productName}, #{country}, #{factoryNo}, #{brandId}, #{merchantId})"})
    void insertOrUpdateFull(@Param("userId") Long userId, @Param("searchWord") String searchWord, @Param("searchType") String searchType, @Param("isSelfSelect") Integer isSelfSelect, @Param("productId") Long productId, @Param("productName") String productName, @Param("country") String country, @Param("factoryNo") String factoryNo, @Param("brandId") Long brandId, @Param("merchantId") Long merchantId);
}
