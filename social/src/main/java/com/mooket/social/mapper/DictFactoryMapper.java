package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.DictFactory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/DictFactoryMapper.class */
public interface DictFactoryMapper extends BaseMapper<DictFactory> {
    @Select({"SELECT * FROM dict_factory WHERE category = #{category}"})
    List<DictFactory> selectByCategory(@Param("category") String category);

    @Select({"SELECT * FROM dict_factory WHERE brand_id = #{brandId}"})
    List<DictFactory> selectByBrandId(@Param("brandId") Integer brandId);

    @Select({"SELECT * FROM dict_factory WHERE brand_id = #{brandId}"})
    List<DictFactory> findByBrandId(@Param("brandId") Integer brandId);

    @Select({"SELECT * FROM dict_factory WHERE category = #{category} AND (REPLACE(factory_no, ' ', '') LIKE CONCAT('%', REPLACE(#{keyword}, ' ', ''), '%') OR REPLACE(country, ' ', '') LIKE CONCAT('%', REPLACE(#{keyword}, ' ', ''), '%'))"})
    List<DictFactory> searchByKeyword(@Param("category") String category, @Param("keyword") String keyword);

    @Select({"SELECT * FROM dict_factory WHERE category = #{category} AND REPLACE(country, ' ', '') LIKE CONCAT('%', REPLACE(#{country}, ' ', ''), '%')"})
    List<DictFactory> searchByCountry(@Param("category") String category, @Param("country") String country);

    @Select({"SELECT * FROM dict_factory WHERE category = #{category} AND REPLACE(factory_no, ' ', '') LIKE CONCAT('%', REPLACE(#{factoryNo}, ' ', ''), '%')"})
    List<DictFactory> searchByFactoryNo(@Param("category") String category, @Param("factoryNo") String factoryNo);

    @Select({"SELECT * FROM dict_factory WHERE REPLACE(factory_no, ' ', '') = REPLACE(#{factoryNo}, ' ', '')"})
    List<DictFactory> selectByFactoryNo(@Param("factoryNo") String factoryNo);

    @Select({"SELECT * FROM dict_factory WHERE REPLACE(factory_no, ' ', '') = REPLACE(#{factoryNo}, ' ', '') AND category = #{category}"})
    List<DictFactory> selectByFactoryNoWithCategory(@Param("factoryNo") String factoryNo, @Param("category") String category);

    @Select({"SELECT * FROM dict_factory WHERE category = #{category} AND REPLACE(country, ' ', '') LIKE CONCAT('%', REPLACE(#{country}, ' ', ''), '%') AND REPLACE(factory_no, ' ', '') LIKE CONCAT('%', REPLACE(#{factoryNo}, ' ', ''), '%')"})
    List<DictFactory> findByCountryAndFactoryNo(@Param("category") String category, @Param("country") String country, @Param("factoryNo") String factoryNo);
}