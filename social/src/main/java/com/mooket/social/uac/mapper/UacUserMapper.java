package com.mooket.social.uac.mapper;

import com.mooket.social.entity.uac.UacUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MySQL uac_user 表 Mapper (mallee_muji_uac)
 */
@Mapper
public interface UacUserMapper {

    /**
     * 根据手机号查询有效的UAC用户
     * 条件：is_cancel=0 AND enable=1 AND is_deleted=0
     */
    @Select("SELECT id, user_no, user_muji_no, user_type, user_category, mobile_no, nick_name, " +
            "user_name, anonymous_name, face_url, anonymous_face_url, is_identification, is_cancel, enable, is_deleted, created_time, update_time " +
            "FROM uac_user WHERE mobile_no = #{phone} AND is_cancel = 0 AND enable = 1 AND is_deleted = 0 " +
            "ORDER BY id DESC LIMIT 1")
    UacUser selectByPhone(@Param("phone") String phone);
}
