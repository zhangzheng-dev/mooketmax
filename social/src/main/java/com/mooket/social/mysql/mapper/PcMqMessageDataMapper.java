package com.mooket.social.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mooket.social.mysql.entity.PcMqMessageData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * PC MQ消息 Mapper
 */
@Mapper
public interface PcMqMessageDataMapper extends BaseMapper<PcMqMessageData> {

    /**
     * 根据手机号查询最新一条短信验证码
     * @param phone 手机号
     * @return 验证码，如果没有则返回null
     */
    @Select("SELECT message_body FROM pc_mq_message_data " +
            "WHERE message_topic = 'SEND_SMS_TOPIC' " +
            "AND message_body LIKE CONCAT('%', #{phone}, '%') " +
            "ORDER BY created_time DESC LIMIT 1")
    String findLatestSmsMessageBody(@Param("phone") String phone);
}