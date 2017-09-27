package cn.medcn.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by Liuchangling on 2017/9/22.
 */
@NoArgsConstructor
@Data
public class CspUserInfoDTO  {
    // 用户id
    protected String uid;
    // 昵称
    protected String nickName;
    // 性别
    protected String gender;
    // 国家
    protected String country;
    // 省份
    protected String province;
    // 城市
    protected String city;
    // 地区
    protected String district;
    // 第三方平台唯一id
    protected String uniqueId;
    // 头像
    protected String avatar;

    // 第三方平台id
    protected Integer thirdPartyId;




}