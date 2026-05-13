package com.mooket.social.entity.uac;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * MySQL uac_user 表实体 (mallee_muji_uac)
 */
@Data
public class UacUser {

    private Long id;

    private String userNo;

    private String userMujiNo;

    private Integer userType;

    private Integer userCategory;

    private String mobileNo;

    private String nickName;

    private String userName;

    private String anonymousName;

    private String anonymousFaceUrl;

    private Integer isIdentification;

    private Integer isCancel;

    private Integer enable;

    private Integer isDeleted;

    private LocalDateTime createdTime;

    private LocalDateTime updateTime;
}
