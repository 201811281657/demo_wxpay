package com.hn.yuan.entity;

import lombok.Data;

import java.io.Serializable;


@Data
public class WxPayDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private String partyOrg;
    private String duesDate;
    private String money;   //缴费金额
    private String name;
    private String idCard;
    private String post;
    private String phone;
    private String orderId;
    private String receiverInfo;
    private String code;//获取openid临时凭证
    private String agyCode;
    private String agyName;

}

