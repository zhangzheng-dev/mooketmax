package com.mooket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.entity.DictMerchant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
/* loaded from: temp_jar_download.jar:BOOT-INF/classes/com/mooket/social/mapper/DictMerchantMapper.class */
public interface DictMerchantMapper extends BaseMapper<DictMerchant> {
    @Select({"SELECT * FROM dict_merchant WHERE contact_phone = #{phone} LIMIT 1"})
    DictMerchant selectByPhone(@Param("phone") String phone);

    @Select({"SELECT * FROM dict_merchant WHERE REPLACE(merchant_name, ' ', '') LIKE CONCAT('%', REPLACE(#{keyword}, ' ', ''), '%') OR REPLACE(merchant_short_name, ' ', '') LIKE CONCAT('%', REPLACE(#{keyword}, ' ', ''), '%') GROUP BY merchant_id ORDER BY   CASE WHEN REPLACE(merchant_name, ' ', '') = REPLACE(#{keyword}, ' ', '') THEN 0        WHEN REPLACE(merchant_short_name, ' ', '') = REPLACE(#{keyword}, ' ', '') THEN 1        WHEN REPLACE(merchant_name, ' ', '') LIKE CONCAT(REPLACE(#{keyword}, ' ', ''), '%') THEN 2        WHEN REPLACE(merchant_short_name, ' ', '') LIKE CONCAT(REPLACE(#{keyword}, ' ', ''), '%') THEN 3        ELSE 4 END,   LENGTH(REPLACE(merchant_name, ' ', '')) ASC LIMIT 20"})
    List<DictMerchant> searchByName(@Param("keyword") String keyword);

    @Select({"SELECT * FROM dict_merchant"})
    List<DictMerchant> selectAll();

    @Select({"<script>SELECT * FROM dict_merchant WHERE contact_phone IN <foreach collection='phones' item='phone' open='(' separator=',' close=')'>#{phone}</foreach></script>"})
    List<DictMerchant> selectByPhones(@Param("phones") List<String> phones);

    @Select({"SELECT * FROM dict_merchant WHERE REPLACE(merchant_name, ' ', '') = REPLACE(#{merchantName}, ' ', '') OR REPLACE(merchant_short_name, ' ', '') = REPLACE(#{merchantName}, ' ', '') LIMIT 1"})
    Optional<DictMerchant> findByName(@Param("merchantName") String merchantName);
}
