/** 
 * @(#)AlipayService.java 1.0.0 2016年11月12日 下午2:25:30  
 *  
 * Copyright © 2016 善林金融.  All rights reserved.  
 */

package com.slfinance.redpack.core.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.slfinance.redpack.core.constants.AlipayServiceEnvConstants;
import com.slfinance.redpack.core.entities.Order;
import com.slfinance.redpack.core.exception.SLException;
import com.slfinance.redpack.core.response.ResponseCode;
import com.slfinance.redpack.core.services.base.AbstractService;

/**
 * 支付宝服务
 * 
 * @author samson
 * @version $Revision:1.0.0, $Date: 2016年11月12日 下午2:25:30 $
 */
@Service
public class AlipayService extends AbstractService {

	/**
	 * 支付宝回调地址
	 */
	@Value("${alipayNotifyUrl}")
	private String notifyUrl;

	@Autowired
	private OrderService orderService;

	/**
	 * alipay请求client
	 */
	private AlipayClient alipayClient = new DefaultAlipayClient(AlipayServiceEnvConstants.ALIPAY_GATEWAY, AlipayServiceEnvConstants.APP_ID, AlipayServiceEnvConstants.PRIVATE_KEY, "json", AlipayServiceEnvConstants.CHARSET, AlipayServiceEnvConstants.ALIPAY_PUBLIC_KEY);

	/**
	 * 根据订单生成app请求支付所需字符串
	 * 
	 * @see https
	 *      ://doc.open.alipay.com/docs/doc.htm?spm=a219a.7629140.0.0.EIcLXY
	 *      &treeId=204&articleId=105465&docType=1
	 * @author samson
	 * @createTime 2016年11月12日 下午2:26:44
	 * @param order
	 * @return
	 */
	public String getAppPayOrderString(Order order) {
		Map<String, String> param = new HashMap<String, String>();
		param.put("app_id", AlipayServiceEnvConstants.APP_ID);// 支付宝分配给开发者的应用ID
		param.put("method", "alipay.trade.app.pay");// 接口名称
		param.put("format", "JSON");// 仅支持JSON
		param.put("charset", AlipayServiceEnvConstants.CHARSET);// 请求使用的编码格式，如utf-8,gbk,gb2312等
		param.put("sign_type", AlipayServiceEnvConstants.SIGN_TYPE);// 商户生成签名字符串所使用的签名算法类型，目前支持RSA
		param.put("timestamp", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));// 发送请求的时间，格式"yyyy-MM-dd
																							// HH:mm:ss"
		param.put("version", "1.0");// 调用的接口版本，固定为：1.0
		param.put("notify_url", notifyUrl);// 支付宝服务器主动通知商户服务器里指定的页面http/https路径。建议商户使用https
		// 业务数据
		Map<String, Object> bizContent = new HashMap<String, Object>();
		bizContent.put("subject", order.getSubject());// 商品的标题/交易标题/订单标题/订单关键字等。
		bizContent.put("out_trade_no", order.getId());// 商户网站唯一订单号
		bizContent.put("timeout_express", "30m");// 该笔订单允许的最晚付款时间，逾期将关闭交易。取值范围：1m～15d。m-分钟，h-小时，d-天，1c-当天（1c-当天的情况下，无论交易何时创建，都在0点关闭）。
		bizContent.put("total_amount", String.format("%.2f", order.getOrderAmount()));// 订单总金额，单位为元，精确到小数点后两位，取值范围[0.01,100000000]
		bizContent.put("product_code", "QUICK_MSECURITY_PAY");// 销售产品码，商家和支付宝签约的产品码，为固定值QUICK_MSECURITY_PAY
		param.put("biz_content", JSON.toJSONString(bizContent));// 业务请求参数的集合，最大长度不限，除公共参数外所有请求参数都必须放在这个参数中传递，具体参照各产品快速接入文档
		// 加密字段
		String signContent = AlipaySignature.getSignContent(param);
		String sign;
		try {
			sign = AlipaySignature.rsaSign(signContent, AlipayServiceEnvConstants.PRIVATE_KEY, AlipayServiceEnvConstants.CHARSET, AlipayConstants.SIGN_TYPE_RSA);
			sign = URLEncoder.encode(sign, AlipayServiceEnvConstants.CHARSET);
		} catch (Exception e) {
			throw new SLException(ResponseCode.SERVER_ERROR, "加密失败!");
		}

		for (String key : param.keySet()) {
			try {
				param.put(key, URLEncoder.encode(param.get(key), AlipayServiceEnvConstants.CHARSET));
			} catch (UnsupportedEncodingException e) {
			}
		}
		signContent = AlipaySignature.getSignContent(param);

		return signContent + "&sign=" + sign;
	}
}