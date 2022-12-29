package com.hn.yuan.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hn.yuan.entity.WxPayDto;
import com.hn.yuan.wxinterface.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static com.hn.yuan.wxinterface.WXPayConstants.RETURN_CODE;
import static com.hn.yuan.wxinterface.WechatConfig.APP_SECRET;

@CrossOrigin
@RestController
@RequestMapping("/wechatPay")
public class WxpayController {
    private static Logger logger = LoggerFactory.getLogger(WxpayController.class);

    @Autowired
    private HttpServletRequest request;

    @Resource
    private HttpServletResponse response;


    @PostMapping("/wxPay")
    public String wxPay(@RequestBody WxPayDto dto) {
        System.out.println("进来方法了");
        Object result = new Object();
        try {
            //获取客户端的ip地址
            String spbill_create_ip = getIpAddr(request);
            System.out.println("客户端Ip地址" + spbill_create_ip);
            System.out.println("code是什么样式的" + dto.getCode());
            //获取openid
            String openid = getOpenId(dto.getCode());
            //订单号  uuid  随机生成
            String outTradeNo = WXPayUtil.generateUUID();
            //支付业务
            result = wxPay(spbill_create_ip, openid, outTradeNo, dto);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PayUtil.toJson(result);
    }


    //获取IP
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = ip.indexOf(",");
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 缴纳费用
     **/
    public Map wxPay(String spbill_create_ip, String openId, String orderNumber, WxPayDto dto) {
        Map<String, String> payMap = new HashMap<String, String>();//返回给小程序端需要的参数
        try {
            logger.info("【小程序支付】 统一下单开始, 订单编号=" + orderNumber);
            //商品名称
            String body = dto.getAgyName() + "-" + dto.getName() + "-测试费用";
            //获取客户端的ip地址
            BigDecimal money = new BigDecimal(dto.getMoney());
            //组装参数，用户生成统一下单接口的签名
            logger.info("----------下单接口签名-------");
            Map<String, String> packageParams = new HashMap<>();
            //微信分配的小程序ID
            packageParams.put("appid", WechatConfig.appid);
            //微信支付分配的商户号
            packageParams.put("mch_id", WechatConfig.mch_id);
            //随机字符串
            packageParams.put("nonce_str", System.currentTimeMillis() / 1000 + "");
            //签名类型
            packageParams.put("sign_type", "MD5");
            //充值订单 商品描述
            packageParams.put("body", body);
            //商户订单号
            packageParams.put("out_trade_no", orderNumber);
            //订单总金额，单位为分
            packageParams.put("total_fee", money.multiply(BigDecimal.valueOf(100)).intValue() + "");
            //终端IP
            packageParams.put("spbill_create_ip", spbill_create_ip);
            //通知回调地址
            packageParams.put("notify_url", WechatConfig.notify_url);
            //交易类型
            packageParams.put("trade_type", WechatConfig.TRADETYPE);
            //用户标识
            packageParams.put("openid", openId);
            //第一次签名
            String sign = WXPayUtil.generateSignature(packageParams, WechatConfig.key);
            System.out.println("第一签名打印的是个啥" + sign);
            packageParams.put("sign", sign);
            System.out.println("字符串是啥" + PaymentKit.toXml(packageParams));
            //调用支付定义下单API,返回预付单信息 prepay_id
            String result = HttpKit.post(WechatConfig.pay_url, PaymentKit.toXml(packageParams));
            logger.info("调试模式_统一下单接口 返回XML数据：" + result);
            // 将解析结果存储在HashMap中
            Map<String, String> map = PaymentKit.xmlToMap(result);
            String return_code = map.get("return_code");//返回状态码
            String result_code = map.get("result_code");//返回状态码
            if (return_code.equals("SUCCESS") || return_code.equals(result_code)) {
                //返回的预付单信息
                String prepay_id = map.get("prepay_id");
                payMap.put("appId", WechatConfig.appid);
                payMap.put("timeStamp", System.currentTimeMillis() / 1000 + "");
                payMap.put("nonceStr", System.currentTimeMillis() + "");
                payMap.put("package", "prepay_id=" + prepay_id);
                payMap.put("signType", "MD5");

                //再次签名，这个签名用于小程序端调用wx.requesetPayment方法
                String paySign = WXPayUtil.generateSignature(payMap, WechatConfig.key);
                System.out.println("第二签名打印的是个啥" + sign);
                logger.info("=======================第二次签名：", paySign + "============ ======");
                payMap.put("paySign", paySign);
                payMap.put("status", "success");
                //更新订单信息
            } else {
                logger.info("----------统一下单失败-------");
                payMap.put("status", "error");
                return payMap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return payMap;
    }

    /**
     * 小程序获取openid
     *
     * @return
     */
    public static String getOpenId(String code) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + WechatConfig.appid + "&secret=" + APP_SECRET + "&js_code=" + code + "&grant_type=authorization_code";
        ResponseEntity<String> responseEntity =
                restTemplate.getForEntity(url, String.class);
        String body = responseEntity.getBody();
        JSONObject object = JSON.parseObject(body);
        String openId = object.getString("openid");// 获取openId
        System.out.println("该用户的openid——>" + openId);
        return openId;
    }


    /**
     * 功能描述: <小程序回调>
     *
     * @return:
     **/
    @GetMapping("/wxProPayNotify")
    public void wxProPayNotify() throws Exception {
        logger.info("进入微信小程序支付回调");
        String xmlMsg = HttpKit.readData(request);
        logger.info("微信小程序通知信息" + xmlMsg);
        Map<String, String> resultMap = PaymentKit.xmlToMap(xmlMsg);
        if (resultMap.get(RETURN_CODE).equals("SUCCESS")) {
            String orderNo = resultMap.get("out_trade_no");
            logger.info("微信小程序支付成功,订单号{}", orderNo);
            //通过订单号 修改数据库中的记录,业务操作
        }
        String result = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
