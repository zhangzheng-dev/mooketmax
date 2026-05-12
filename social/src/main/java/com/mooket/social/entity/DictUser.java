package com.mooket.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户字典实体
 */
@Data
@TableName("dict_user")
public class DictUser {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String phone;              // 手机号（唯一）

    private String nickname;           // 昵称（2-20字符）

    private String identityTags;       // 身份标签，逗号分隔：海外服务商,贸易商,加工厂/商超,其它

    private String wechat;             // 微信号

    private String wechatNickname;    // 微信昵称

    private String realName;           // 姓名

    private String avatarUrl;          // 头像URL

    private String realNameStatus;     // 实名状态：pending/verified/rejected

    private String cancellationStatus; // 注销状态：active/cancelled

    private String mooketId;            // Mooket ID（来自UAC）

    private String mooketNo;            // Mooket No（来自UAC user_muji_no）

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
