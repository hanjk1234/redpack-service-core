/** 
 * @(#)RedpackService.java 1.0.0 2016年7月26日 上午10:01:03  
 *  
 * Copyright © 2016 善林金融.  All rights reserved.  
 */

package com.slfinance.redpack.core.services;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.slfinance.redpack.common.utils.DateUtil;
import com.slfinance.redpack.common.utils.MapUtil;
import com.slfinance.redpack.core.constants.CommonConstant;
import com.slfinance.redpack.core.constants.RedPackConstant;
import com.slfinance.redpack.core.constants.RedpackRecordStatus;
import com.slfinance.redpack.core.constants.TableConstant;
import com.slfinance.redpack.core.constants.enums.AccountFlowTradeDirection;
import com.slfinance.redpack.core.constants.enums.AccountFlowflowType;
import com.slfinance.redpack.core.constants.enums.AmountOrder;
import com.slfinance.redpack.core.constants.enums.CustomerRelationType;
import com.slfinance.redpack.core.constants.enums.CustomerStatus;
import com.slfinance.redpack.core.constants.enums.LogType;
import com.slfinance.redpack.core.constants.enums.MessageTemplateMessageType;
import com.slfinance.redpack.core.constants.enums.OrderDetailCategory;
import com.slfinance.redpack.core.constants.enums.OrderStatus;
import com.slfinance.redpack.core.constants.enums.OrderType;
import com.slfinance.redpack.core.constants.enums.RedpackStatus;
import com.slfinance.redpack.core.constants.enums.RedpackType;
import com.slfinance.redpack.core.constants.enums.UserType;
import com.slfinance.redpack.core.entities.Account;
import com.slfinance.redpack.core.entities.AccountFlow;
import com.slfinance.redpack.core.entities.Advertisement;
import com.slfinance.redpack.core.entities.AdvertisementAnswer;
import com.slfinance.redpack.core.entities.Customer;
import com.slfinance.redpack.core.entities.Distribute;
import com.slfinance.redpack.core.entities.FileRelation;
import com.slfinance.redpack.core.entities.OperateLog;
import com.slfinance.redpack.core.entities.Order;
import com.slfinance.redpack.core.entities.OrderDetail;
import com.slfinance.redpack.core.entities.RedPack;
import com.slfinance.redpack.core.exception.SLException;
import com.slfinance.redpack.core.extend.jpa.page.PageRequestVo;
import com.slfinance.redpack.core.extend.jpa.page.PageResponse;
import com.slfinance.redpack.core.repositories.RedPackRepository;
import com.slfinance.redpack.core.services.base.BaseService;
import com.slfinance.redpack.core.utils.RedPackUtils;
import com.slfinance.redpack.core.vo.RedPackExportExcelVo;


/**
 * 红包
 * 
 * @author SangLy
 * @version $Revision:1.0.0, $Date: 2016年7月26日 上午10:01:03 $
 */
@Service
public class RedPackService extends BaseService<RedPack, RedPackRepository> {

	@Autowired
	private CodeGeneratorService codeGeneratorService;
	
	@Autowired
	private AdvertisementService advertisementService;

	@Autowired
	private CustomerRelationService customerRelationService;
	
	@Autowired
	private AdvertisementAnswerService advertisementAnswerService;

	@Autowired
	private FileService fileService;
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private AccountFlowService accountFlowService;
	
	@Autowired
	private DistributeService distributeService;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private OrderDetailService orderDetailService;
	
	@Autowired
	private FileRelationService fileRelationService;
	
	@Autowired
	private OperateLogService operateLogService;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private MessageService messageService;
	
	/**
	 * 设置红包状态
	 * 
	 * @author taoxm
	 * @createTime 2016年8月17日 下午1:50:12
	 * @param id
	 * @param status
	 * @return
	 */
	@Transactional
	public RedPack updateStatus(String id, String status) {
		RedPack redpack = findOne(id);
		if (null == redpack) {
			throw new SLException("600003");
		}
		//0、只能修改后台用户创建的红包
		if(redpack.getUserType() != UserType.员工){
			throw new SLException("600049");
		}
		// 1、只有当红包状态为未开启的时候，才能修改状态
		if (redpack.getStatus() != RedpackStatus.审核通过) {
			throw new SLException("600013");
		}
		// 2、只能将红包的状态修改为"已下架"
		if (RedpackStatus.valueOf(status) != RedpackStatus.已失效) {
			throw new SLException("600014");
		}
		// 3、当天的红包不能下架
		if (DateUtil.getToday().compareTo(DateUtil.parseToDate(new SimpleDateFormat("yyyy-MM-dd").format(redpack.getTimePoint()), "yyyy-MM-dd")) == 0) {
			throw new SLException("600019");
		}
		redpack.setStatus(RedpackStatus.valueOf(status));
		return save(redpack);
	}
	
	/**
	 * 新建和修改红包
	 * 
	 * @author taoxm
	 * @createTime 2016年8月17日 下午1:50:27
	 * @param redpack
	 *            红包对象实体
	 * @param advertisementCode
	 *            广告编码
	 * @param isSave
	 *            true ? 新建红包:修改红包
	 */
	@Transactional
	public RedPack saveRedPacket(RedPack redpack, String advertisementId, boolean isSave,String customerId) {
		//1、查询广告信息是否存在
		Advertisement advertisement = advertisementService.findById(advertisementId);
		if (advertisement == null) {
			throw new SLException("500009");
		}
		Integer smallCount = redpack.getSmallCount();
		Double smallAmount = redpack.getSmallAmount();
		Integer bigCount = redpack.getBigCount();
		Double bigAmount = redpack.getBigAmount();
		Double tradeAmount = smallAmount*smallCount + bigAmount*bigCount;//红包总额
		//2、创建红包
		if (DateUtil.addHours(redpack.getTimePoint(), -RedPackConstant.REDPACK_SAVE_BEFORE_HOURS).compareTo(new Date()) < 0) {
			throw new SLException("600054");
		}
		
		//2.0一天只能创建一个邀请红包，一个分享红包
		if(RedpackType.分享红包 == redpack.getRedpackType() || RedpackType.邀请红包 == redpack.getRedpackType()){
			int shareOrInviteRedPackCounts = repository.findByRedpackTypeWithCreateDateIsToday(redpack.getRedpackType().name());
			if(shareOrInviteRedPackCounts > 0){
				throw new SLException("500017");
			}
		}
		//2.1、对红包类型做相应的业务处理
		if(RedpackType.土豪红包 == redpack.getRedpackType()){
			//人数不能小于10人
			if((bigCount+smallCount)<10){
				throw new SLException("600056");
			}
			//土豪红包大奖人数10起
			if(bigCount.compareTo(new Integer(10))<0){
				throw new SLException("600041");
			}
			//金额必须大于200的正整数
			if(tradeAmount.compareTo(new Double(200))<0){
				throw new SLException("600042");
			}
			//大奖不小于5元，小奖不小于0.5元
			if(bigAmount.compareTo(new Double(5))<0 || smallAmount.compareTo(new Double(0.5))<0){
				throw new SLException("600055");
			}
		}else if(RedpackType.经济红包 == redpack.getRedpackType()){
			//人数不能小于10人
			if((bigCount+smallCount)<10){
				throw new SLException("600056");
			}
			//经济红包大奖人数1起
			if(bigCount.compareTo(new Integer(1))<0){
				throw new SLException("600043");
			}
			//小奖人数最多大奖人数的20倍
			if(smallCount.compareTo(bigCount*20)>0){
				throw new SLException("600044");
			}
			//大奖不小于5元，小奖不小于0.5元
			if(bigAmount.compareTo(new Double(5))<0 || smallAmount.compareTo(new Double(0.5))<0){
				throw new SLException("600055");
			}
		}
		
		redpack.setAmount(tradeAmount);
		redpack.setRedpackCode(codeGeneratorService.getRedPackCode());
		if(redpack.getUserType() == UserType.客户){
			redpack.setStatus(RedpackStatus.待付款);//前台手机用户创建红包默认状态为待付款
		}else{
			redpack.setStatus(RedpackStatus.审核通过);//默认审核通过
		}
		
		//2.2如果为创建红包则赋值广告副本，防止修改时影响之前的红包
		Advertisement newAdvertisement = createCopyAdvertisement(advertisement);
		advertisementId = newAdvertisement.getId();
		redpack.setAdvertisementId(advertisementId);//关联广告副本id
		//2.3、扣款（后台用户直接扣款，前台用户冻结），并记录流水
		//2.3.1、判断账户信息
		Account account = accountService.findOne(CommonConstant.SYSTEM_ACCOUNT_ID);
		if(null == account){
			throw new SLException("110003");
		}
		
		//记录流水
		AccountFlow accountFlow = new AccountFlow();
		//2.3.2、判断账户余额是否足够
		if(account.getTotalAmount().compareTo(tradeAmount)<0){
			throw new SLException("110011");
		}
		//扣款
		account.setTotalAmount(account.getTotalAmount()-tradeAmount);
		account.setAvailableAmount(account.getAvailableAmount()-tradeAmount);
		
		accountFlow.setTotalAmount(account.getTotalAmount()-tradeAmount);
		accountFlow.setFlowType(AccountFlowflowType.发红包);
		accountFlow.setOldFlowCode(null);
		accountFlow.setRelatePrimary(null);
		accountFlow.setMemo("后台用户创建红包");
		accountFlow.setTradeAmount(tradeAmount);
		accountFlow.setAvailableAmount(account.getAvailableAmount()-tradeAmount);
		accountFlow.setFreezeAmount(account.getFreezeAmount());
		accountFlow.setTradeDirection(AccountFlowTradeDirection.支出);
		accountFlow.setAccountId(account.getId());
		accountFlow.setFlowCode(codeGeneratorService.getFlowCode());
		//保存账户和流水信息
		accountService.save(account);
		accountFlowService.save(accountFlow);
		return save(redpack);
	}
	
	
	private Advertisement createCopyAdvertisement(Advertisement oldAdvertisement){
		Advertisement newAdvertisement = advertisementService.copyAdvertisement(oldAdvertisement);
		List<AdvertisementAnswer> advertisementAnswerList = advertisementAnswerService.findAnswerListByAdvertisementId(oldAdvertisement.getId());
		for(AdvertisementAnswer advertisementAnswer: advertisementAnswerList){
			//复制广告答案副本,并且把广告副本的答案指向答案副本
			AdvertisementAnswer newAdvertisementAnswer = advertisementAnswerService.copyAdvertisementAnswerByAdvertisement(advertisementAnswer,newAdvertisement.getId());
			if(newAdvertisementAnswer.getAnswerContent().equals(advertisementAnswerService.findOne(newAdvertisement.getCorrectAnswer()).getAnswerContent())){
				newAdvertisement.setCorrectAnswer(newAdvertisementAnswer.getId());
				advertisementService.save(newAdvertisement);
			}
		}
		// (广告副本和新文件记录做关联)
		List<FileRelation> fileRelationList = fileRelationService.findByAdvertisementAndRelatePrimary(oldAdvertisement.getId());
		for (FileRelation fileRelation : fileRelationList) {
			FileRelation newFileRelation = new FileRelation();
			newFileRelation.setRelateTable(TableConstant.T_ADVERTISEMENT);
			newFileRelation.setRelatePrimary(newAdvertisement.getId());
			newFileRelation.setFileId(fileRelation.getFileId());
			fileRelationService.save(newFileRelation);
		}
		return newAdvertisement;
	}
	
	/**
	 * 新建和修改红包
	 * 
	 * @author SangLy
	 * @createTime 2016年8月17日 下午1:50:27
	 * @param redpack
	 *            红包对象实体
	 * @param advertisementId
	 *            id
	 */
	@Transactional
	public RedPack appSaveRedPacket(Map<String, Object> params, String customerId) throws SLException{
		// 查询广告信息是否存在
		Advertisement advertisement = advertisementService.findById(MapUtil.getStringTrim(params, "id"));
		if (advertisement == null) {
			throw new SLException("500009");
		}
		RedPack redpack = new RedPack();
		redpack.setAdvertisementId(MapUtils.getString(params, "id"));
		redpack.setTimePoint(DateUtil.parseToDate(MapUtil.getStringTrim(params, "timePoint"), "yyyy-MM-dd"));
		redpack.setRedpackType(RedpackType.valueOf(MapUtil.getString(params, "redpackType")));
		redpack.setBigAmount(MapUtil.getDouble(params, "bigAmount"));
		redpack.setBigCount(MapUtil.getInteger(params, "bigCount"));
		redpack.setSmallAmount(MapUtil.getDouble(params, "smallAmount"));
		redpack.setSmallCount(MapUtil.getInteger(params, "smallCount"));
		redpack.setStatus(RedpackStatus.待付款);
		redpack.setUserType(UserType.客户);
		redpack.setCreatedUser(customerId);
		redpack.setRecordStatus(RedpackRecordStatus.正常);

		Integer smallCount = redpack.getSmallCount();
		Double smallAmount = redpack.getSmallAmount();
		Integer bigCount = redpack.getBigCount();
		Double bigAmount = redpack.getBigAmount();
		Double tradeAmount = smallAmount * smallCount + bigAmount * bigCount;// 红包总额
		Integer totalCount = bigCount+smallCount; //红包大奖和小奖总人数
		
		/**
		 * 土豪红包中，大奖人数不少于10人，小奖无限制；经济红包中，大奖人数不少于1人，小奖与大奖人数比例不大于20:1。
		 * 土豪红包与经济红包中的大奖金额不小于5元，小奖金额不小于0.5元。土豪红包总金额大于200正整数，经济红包金额不限制。
		 */
		if (bigAmount.compareTo(new Double(5)) < 0) {
			throw new SLException("600058", "大奖金额不小于5元");
		}
		if (smallAmount.compareTo(new Double(0.5)) < 0) {
			throw new SLException("600059", "小奖金额不小于0.5元");
		}
		if (RedpackType.土豪红包 == redpack.getRedpackType()) {
			// 土豪红包大奖人数10起
			if (bigCount.compareTo(new Integer(10)) < 0) {
				throw new SLException("600041");
			}
			// 金额必须大于200的正整数
			if (tradeAmount.compareTo(new Double(200)) < 0 || tradeAmount.compareTo(new Double(String.valueOf(tradeAmount.intValue()))) != 0) {
				throw new SLException("600042");
			}
		} else if (RedpackType.经济红包 == redpack.getRedpackType()) {
			// 经济红包大奖人数1起
			if (bigCount.compareTo(new Integer(1)) < 0) {
				throw new SLException("600043");
			}
			// 小奖人数最多大奖人数的20倍
			if (smallCount.compareTo(bigCount * 20) > 0) {
				throw new SLException("600044");
			}
		}else{
			throw new SLException("600051","不支持的红包类型");
		}
		//红包总数不少于10人
		if(totalCount.compareTo(10) < 0){
			throw new SLException("600065","红包大奖和小奖总人数不能少于10人");
		}
		
		redpack.setAmount(tradeAmount);
		redpack.setRedpackCode(codeGeneratorService.getRedPackCode());
		
		if (DateUtil.addHours(redpack.getTimePoint(), -RedPackConstant.REDPACK_SAVE_BEFORE_HOURS).compareTo(new Date()) < 0) {
			throw new SLException("600054","红包的创建在前一天15点之前完成 ,红包发送时间必须在第二天凌晨前9个小时完成");
		}
		
		// 添加广告副本，用于新建创建红包
		Advertisement newAdvertisement = advertisementService.copyAdvertisement(advertisement);
		List<AdvertisementAnswer> advertisementAnswerList = advertisementAnswerService.findAnswerListByAdvertisementId(advertisement.getId());
		for(AdvertisementAnswer advertisementAnswer: advertisementAnswerList){
			//复制广告答案副本,并且把广告副本的答案指向答案副本
			AdvertisementAnswer newAdvertisementAnswer = advertisementAnswerService.copyAdvertisementAnswerByAdvertisement(advertisementAnswer,newAdvertisement.getId());
			if(newAdvertisementAnswer.getAnswerContent().equals(advertisementAnswerService.findOne(newAdvertisement.getCorrectAnswer()).getAnswerContent())){
				newAdvertisement.setCorrectAnswer(newAdvertisementAnswer.getId());
				advertisementService.save(newAdvertisement);
			}
		}
		// (广告副本和新文件记录做关联)
		List<FileRelation> fileRelationList = fileRelationService.findByAdvertisementAndRelatePrimary(advertisement.getId());
		for (FileRelation fileRelation : fileRelationList) {
			FileRelation newFileRelation = new FileRelation();
			newFileRelation.setRelateTable(TableConstant.T_ADVERTISEMENT);
			newFileRelation.setRelatePrimary(newAdvertisement.getId());
			newFileRelation.setFileId(fileRelation.getFileId());
			fileRelationService.save(newFileRelation);
		}
		// 红包表添加记录
		redpack.setAdvertisementId(newAdvertisement.getId());
		RedPack newRedpack = repository.save(redpack);

		// 查询账户余额是否够支付（红包金额+手续费）
		Account account = accountService.findBycustomerId(customerId);
		if (null == account) {
			throw new SLException("110003");
		}
		if (account.getAvailableAmount().compareTo(tradeAmount*(1+RedPackConstant.REDPACK_PAY_RATE)) < 0) {
			throw new SLException("110011");
		}

		// 创建订单(订单表添加记录)
		Order order = new Order();
		order.setCustomerId(customerId);
		order.setRelatePrimary(newRedpack.getId());
		order.setOrderAmount(new BigDecimal(newRedpack.getAmount() * (1 + RedPackConstant.REDPACK_PAY_RATE)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 20%的手续费
		order.setSubject("发红包");
		order.setOrderType(OrderType.发红包);
		order.setOrderStatus(OrderStatus.正常);
		Order newOrder = orderService.save(order);

		// 红包详情添加2条记录,一条是红包金额。一条是手续费
		OrderDetail orderDetail = new OrderDetail();
		orderDetail.setOrderId(newOrder.getId());
		orderDetail.setCategory(OrderDetailCategory.发红包);
		orderDetail.setAmount(newRedpack.getAmount());
		orderDetailService.save(orderDetail);

		OrderDetail orderDetail2 = new OrderDetail();
		orderDetail2.setOrderId(newOrder.getId());
		orderDetail2.setCategory(OrderDetailCategory.发红包手续费);
		orderDetail2.setAmount(newOrder.getOrderAmount() - newRedpack.getAmount());
		orderDetailService.save(orderDetail2);
		return newRedpack;
	}

	/**
	 * 查询红包列表
	 * 
	 * @author taoxm
	 * @createTime 2016年8月17日 下午1:50:49
	 * @param pageRequest
	 * @return
	 */
	public PageResponse<Map<String, Object>> findAllPage(PageRequestVo pageRequest) {
		PageResponse<Map<String, Object>> result = repository.findAllPage(pageRequest);
		return result;
	}

	/**
	 * 查询单个红包详情
	 * 
	 * @author taoxm
	 * @createTime 2016年8月17日 下午1:51:37
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> findRedpackAndAdvertisementInfo(String id) throws Exception {
		Map<String, Object> result = repository.findRedpackAndAdvertisementInfo(id);
		if (null == result || result.isEmpty()) {
			throw new SLException("600003");
		}
		String advertisementId = MapUtil.getStringTrim(result, "advertisementId");
		List<Map<String, Object>> answers = advertisementAnswerService.findByAdvertisementId(advertisementId);
		result.put("answers", answers);
		List<Map<String, Object>> files = fileService.findByRelateTableAndRelatePrimary(TableConstant.T_ADVERTISEMENT, advertisementId);
		result.put("files", files);
		return result;
	}

	/**
	 * 根据传入的日期计算红包总发送次数 其中不包括"已下架"的红包
	 * 
	 * @author SangLy
	 * @createTime 2016年8月16日 下午2:53:41
	 * @param day
	 *            日期
	 * @return
	 */
	public Long todayTotalCount(Date day) {
		Date date = DateUtil.weedDayBelow(day);
		List<String> status = new ArrayList<String>();
		status.add(RedpackStatus.未开启.toString());
		status.add(RedpackStatus.正开启.toString());
		status.add(RedpackStatus.已开启.toString());
		return repository.todayRedpackCount(date, status,RedpackRecordStatus.正常.toString()); 
	}

	/**
	 * 根据传入的日期计算红包已经发送的次数
	 * 
	 * @author SangLy
	 * @createTime 2016年8月16日 下午2:53:41
	 * @param day
	 *            日期
	 * @return
	 */
	public Long todaySentCount(Date day) {
		Date date = DateUtil.weedDayBelow(day);
		List<String> status = new ArrayList<String>();
		status.add(RedpackStatus.正开启.toString());
		status.add(RedpackStatus.已开启.toString());
		return repository.todayRedpackCount(date, status,RedpackRecordStatus.正常.toString()); 
	}

	/**
	 * 距离系统时间最近的当天还没有开启的红包
	 * 
	 * @author SangLy
	 * @createTime 2016年8月16日 下午5:01:52
	 * @param systemTime
	 *            系统时间 格式：2016-8-16 17:28:17
	 * @return
	 */
	public RedPack findCloselyHaveNotOpen(Date systemTime) {
		Date date = DateUtil.weedDayBelow(systemTime);
		return repository.findCloselyHaveNotOpen(date, systemTime, RedpackStatus.未开启.toString());
	}

	/**
	 * 距离当天时间最近的还没有开启的红包，例如系统时间11点。9点场次的红包由于某种原因是“未开启”状态，则查询出的是9点场次的红包
	 * 
	 * @author SangLy
	 * @createTime 2016年8月16日 下午5:01:52
	 * @param systemTime
	 *            系统时间 格式：2016-8-16 17:28:17
	 * @return
	 */
	public RedPack findCloselyDayHaveNotOpen(Date systemTime) {
		Date date = DateUtil.weedDayBelow(systemTime);
		return repository.findCloselyDayHaveNotOpen(date, RedpackStatus.未开启.toString(),RedpackRecordStatus.正常.toString());
	}

	/**
	 * 距离系统时间最近的正在开启的红包
	 * 
	 * @author SangLy
	 * @createTime 2016年8月16日 下午5:01:52
	 * @param systemTime
	 *            系统时间 格式：2016-8-16 17:28:17
	 * @return
	 */
	public RedPack findCloselyHaveOpening(Date systemTime) {
		Date date = DateUtil.weedDayBelow(systemTime);
		return repository.findCloselyHaveOpening(date, systemTime, RedpackStatus.正开启.toString());
	}

	/**
	 * 距离当天时间最近“正开启”的红包，例如系统时间11点。9点场次的红包由于某种原因是“正开启”状态没有变成“已开启”， 则查询出的是9点场次的红包
	 * 
	 * @author SangLy
	 * @createTime 2016年8月16日 下午5:01:52
	 * @param systemTime
	 *            系统时间 格式：2016-8-16 17:28:17
	 * @return
	 */
	public RedPack findCloselyDayHaveOpening(Date systemTime) {
		Date date = DateUtil.weedDayBelow(systemTime);
		return repository.findCloselyDayHaveOpening(date, RedpackStatus.正开启.toString(),RedpackRecordStatus.正常.toString());
	}

	/**
	 * 导出红包
	 * 
	 * @author taoxm
	 * @createTime 2016年8月17日 下午1:52:00
	 * @param redPackExportExcelVo
	 * @param monthsBetween
	 * @return
	 */
	public List<Map<String, Object>> exportExcel(RedPackExportExcelVo redPackExportExcelVo, int monthsBetween) {
		String endTimePoint = redPackExportExcelVo.getEndTimePoint();
		String startTimePoint = redPackExportExcelVo.getStartTimePoint();
		Date endTimePointD = null;
		Date startTimePointD = null;
		// 1、判断并设置开始和结束时间
		if (StringUtils.isBlank(endTimePoint) && StringUtils.isBlank(startTimePoint)) {
			endTimePointD = DateUtil.getToday();
			startTimePointD = DateUtil.addMonths(endTimePointD, -monthsBetween);
		} else if ((!StringUtils.isBlank(endTimePoint)) && StringUtils.isBlank(startTimePoint)) {
			endTimePointD = DateUtil.parseToDate(endTimePoint, "yyyy-MM-dd");
			startTimePointD = DateUtil.addMonths(endTimePointD, -monthsBetween);
		} else if ((!StringUtils.isBlank(startTimePoint)) && StringUtils.isBlank(endTimePoint)) {
			startTimePointD = DateUtil.parseToDate(startTimePoint, "yyyy-MM-dd");
			endTimePointD = DateUtil.addMonths(startTimePointD, monthsBetween);
		} else {
			endTimePointD = DateUtil.parseToDate(endTimePoint, "yyyy-MM-dd");
			startTimePointD = DateUtil.parseToDate(startTimePoint, "yyyy-MM-dd");
		}
		// 2、判断时间跨度是否大于3个月
		if (DateUtil.addMonths(startTimePointD, monthsBetween).compareTo(endTimePointD) < 0) {
			throw new SLException("600008");
		}
		redPackExportExcelVo.setEndTimePoint(new SimpleDateFormat("yyyy-MM-dd").format(endTimePointD));
		redPackExportExcelVo.setStartTimePoint(new SimpleDateFormat("yyyy-MM-dd").format(startTimePointD));
		return repository.exportExcel(redPackExportExcelVo);
	}

	/**
	 * 根据id查询红包
	 *
	 * @author SangLy
	 * @createTime 2016年4月26日 下午2:42:56
	 * @param id
	 *            红包id
	 * @return
	 */
	public RedPack findById(String id) {
		return findOne(id);
	}

	/**
	 * app-红包里列表
	 * 
	 * @author SangLy
	 * @createTime 2016年8月19日 上午9:41:14
	 * @param pageRequest
	 * @return
	 */
	public PageResponse<Map<String, Object>> appRedpackListSort(PageRequestVo pageRequest) throws SLException {
		// 排序条件
		String type = (String) pageRequest.getParam("type"); // 值为：初始化、全部、淘金、捡漏
		String amountOrder = (String) pageRequest.getParam("amountOrder"); // 对应页面金额的升降（值为，ASC,DESC）

		// 校验参数并设定参数值
		validationAndsetParamOfRedpacklist(pageRequest);

		// 一、全部
		/**
		 * 1.如果所有的参数为空则，按照全部，排序规则为： a.正在开启，未开启时间正排序，b.未开启，时间正序，c.已开启，时间倒序
		 */
		if (StringUtils.isBlank(type) && StringUtils.isBlank(amountOrder)) {
			return repository.appRedpackAllDefaultSort(pageRequest);
		}
		/**
		 * 2.全部，并且金额排序不为空，按照金额排序，如果金额相同，按照开启时间正序
		 */
		if (StringUtils.isBlank(type) && StringUtils.isNotBlank(amountOrder)) {
			if (AmountOrder.ASC.toString().equals(amountOrder)) {
				return repository.appRedpackAllByAmountOrderAsc(pageRequest);
			} else if (AmountOrder.DESC.toString().equals(amountOrder)) {
				return repository.appRedpackAllByAmountOrderDsc(pageRequest);
			}
		}
		// 二、任务
		/**
		 * 默认，正在开启放前，未开启放后，按照开启时间正序
		 */
		if ("任务".equals(type) && StringUtils.isBlank(amountOrder)) {
			return repository.appRedpackRenWuByDefaultSort(pageRequest);
		}
		if ("任务".equals(type) && StringUtils.isNotBlank(amountOrder)) {
			if (AmountOrder.ASC.toString().equals(amountOrder)) {
				return repository.appRedpackRenWuByAmountOrderAsc(pageRequest);
			} else if (AmountOrder.DESC.toString().equals(amountOrder)) {
				return repository.appRedpackRenWuByAmountOrderDsc(pageRequest);
			}
		}
		// 三、土豪
		/**
		 * 默认：开启时间倒序
		 */
		if ("土豪".equals(type) && StringUtils.isBlank(amountOrder)) {
			return repository.appRedpackRedPackTypeByDefaultSort(pageRequest);
		}
		if ("土豪".equals(type) && StringUtils.isNotBlank(amountOrder)) {
			if (AmountOrder.ASC.toString().equals(amountOrder)) {
				return repository.appRedpackRedPackTypeByAmountOrderAsc(pageRequest);
			} else if (AmountOrder.DESC.toString().equals(amountOrder)) {
				return repository.appRedpackRedPackTypeByAmountOrderDsc(pageRequest);
			}
		}
		// 三、经济
		/**
		 * 默认：开启时间倒序
		 */
		if ("经济".equals(type) && StringUtils.isBlank(amountOrder)) {
			return repository.appRedpackRedPackTypeByDefaultSort(pageRequest);
		}
		if ("经济".equals(type) && StringUtils.isNotBlank(amountOrder)) {
			if (AmountOrder.ASC.toString().equals(amountOrder)) {
				return repository.appRedpackRedPackTypeByAmountOrderAsc(pageRequest);
			} else if (AmountOrder.DESC.toString().equals(amountOrder)) {
				return repository.appRedpackRedPackTypeByAmountOrderDsc(pageRequest);
			}
		}
		return null;
	}

	// 红包列表设置时间参数
	@SuppressWarnings("deprecation")
	private void validationAndsetParamOfRedpacklist(PageRequestVo pageRequest) {

		Date systemTime = new Date();
		Date queryStartTimePoint = DateUtil.weedDayBelow(systemTime);
		Date queryEndTimePoint = DateUtil.weedDayBelow(systemTime);

		String startTimePoint = MapUtils.getString(pageRequest.getParams(), "startTimePoint");
		String endTimePoint = MapUtils.getString(pageRequest.getParams(), "endTimePoint");
		String startAmount = MapUtils.getString(pageRequest.getParams(), "startAmount");
		String endAmount = MapUtils.getString(pageRequest.getParams(), "endAmount");
		String amountOrder = MapUtils.getString(pageRequest.getParams(), "amountOrder");
		String type = MapUtils.getString(pageRequest.getParams(), "type");

		try {
			if (StringUtils.isNotBlank(startTimePoint)) {
				queryStartTimePoint.setHours(Integer.parseInt(startTimePoint));
			}
			if (StringUtils.isNotBlank(endTimePoint)) {
				queryEndTimePoint.setHours(Integer.parseInt(endTimePoint));
			} else {
				queryEndTimePoint.setHours(23);
				queryEndTimePoint.setMinutes(59);
				queryEndTimePoint.setSeconds(59);
			}
			if (StringUtils.isNotBlank(amountOrder)) {
				if (AmountOrder.valueOf(amountOrder) == null) {
					throw new Exception();
				}
			}
			//相应类型传递的数据对应如下: 全部：空串 任务红包：任务 土豪红包：土豪 经济红包: 经济 
			if (!"".equals(type) && !"任务".equals(type) && !"土豪".equals(type) && !"经济".equals(type)) {
				throw new Exception();
			}
			if("经济".equals(type)){
				pageRequest.addParam("redpackType", RedpackType.经济红包.toString());
			}else if("土豪".equals(type)){
				pageRequest.addParam("redpackType", RedpackType.土豪红包.toString());
			}

			// 设置参数
			Date day = DateUtil.weedDayBelow(systemTime);
			pageRequest.addParam("day", day);
			pageRequest.addParam("startTimePoint", queryStartTimePoint);
			pageRequest.addParam("endTimePoint", queryEndTimePoint);
			if (StringUtils.isNotBlank(startAmount)) {
				pageRequest.addParam("startAmount", Double.parseDouble(startAmount));
			}
			if (StringUtils.isNotBlank(endAmount)) {
				pageRequest.addParam("endAmount", Double.parseDouble(endAmount));
			}

		} catch (Exception e) {
			throw new SLException("100000", "param wors wrong");
		}
	}

	/**
	 * app-我的红包预约列表
	 * 
	 * @author SangLy
	 * @createTime 2016年8月19日 下午4:54:07
	 * @param pageRequest
	 * @return
	 */
	public PageResponse<Map<String, Object>> appGetSubscription(PageRequestVo pageRequest) {
		return repository.appGetSubscription(pageRequest);
	}

	/**
	 * app-获取红包口令 前端根据此值进行不能的操作 0：回答正确 1: 回答错误 2: 红包抢光 3：任务未做完
	 * 
	 * @author SangLy
	 * @createTime 2016年8月22日 上午10:25:18
	 * @param id
	 *            红包id
	 * @param userId
	 *            用户id
	 * @return
	 */
	@Transactional
	public Map<String, Object> appGetShibboleth(String redpackId, String correctAnswer, String customerId) throws SLException {
		Map<String, Object> result = new HashMap<String, Object>();
		RedPack redpack = findOne(redpackId);
		if (redpack != null) {
			if (RedpackStatus.正开启.equals(redpack.getStatus()) || RedpackStatus.已开启.equals(redpack.getStatus())) {
				Date systemTime = new Date();
				if (redpack.getRedpackType().equals(RedpackType.分享红包)) {
					// 分享任务
					Advertisement dailyQuestAdvertisement = advertisementService.dailyQuest();
					if (dailyQuestAdvertisement != null) {
						List<Map<String, Object>> hongbaofenxiangList = customerRelationService.findListCustomerIdAndRelateTableAndRelatePrimaryAndTypeAndDay(systemTime, customerId, TableConstant.T_ADVERTISEMENT, dailyQuestAdvertisement.getId(), CustomerRelationType.广告分享);
						if (hongbaofenxiangList.size() < 0) {
							result.put("condition", "3");
							return result;
						}
					}
				} else if (redpack.getRedpackType().equals(RedpackType.邀请红包)) {
					List<Map<String, Object>> hongbaoyaoqingList = customerRelationService.findListCustomerIdAndRelateTableAndTypeAndDay(systemTime, customerId, CustomerRelationType.好友邀请);
					List<Map<String, Object>> userRegisterLog = customerRelationService.findListRelatePrimaryAndRelateTableAndTypeAndDay(systemTime, customerId, CustomerRelationType.好友邀请);
					if (hongbaoyaoqingList.size() < 0 && userRegisterLog.size() < 0) {
						result.put("condition", "3");
						return result;
					}
				}
				// 判断红包是否抢光
				if (RedpackStatus.已抢完.equals(redpack.getStatus())) {
					result.put("condition", "2");
					return result;
				}
				
				//判断该用户是否抢过此红包
				List<Distribute> listDistributeById = distributeService.findByAssigneeAndRedpackId(customerId, redpackId);
				if(listDistributeById.size() > 0){
					result.put("condition", "4"); //对外4是已经抢到过
					return result;
				}
				
				// 根据红包id获取答案
				Advertisement advertisement = advertisementService.findOne(redpack.getAdvertisementId());
				if (advertisement != null) {
					AdvertisementAnswer advertisementAnswer = advertisementAnswerService.findOne(advertisement.getCorrectAnswer());
					if (advertisementAnswer != null) {
						if (correctAnswer.equals(advertisementAnswer.getId())) {
							try {
								//抢红包status 0 ：抢到1：已经抢到过不能重复抢2 红包已经被抢光
								Map<String,Object>   distributeResult= distributeService.robRedPack(redpackId, customerId);
								if ("0".equals(distributeResult.get("status").toString())) {
									result.put("condition", "0");
									Distribute newDistribute = ((Distribute) distributeResult.get("distribute"));
									result.put("amount", String.format("%.2f", newDistribute.getAmount()));
									// 把用户抢到的钱，转入该用户账可用余额中,记流水
									Account customerAccount = accountService.findBycustomerId(customerId);
									accountService.robRedPackMoneyFlow(customerAccount,redpackId, newDistribute.getAmount());
									return result;
								} else if("1".equals(distributeResult.get("status").toString())){
									result.put("condition", "4"); //对外4是已经抢到过
									return result;
								}else{
									result.put("condition", "2");
									return result;
								}
							} catch (Exception e) {
								result.put("condition", "2");
								return result;
							}
						}
					}
					result.put("condition", "1");
					return result;
				}
			} else {
				throw new SLException("600012", "no redpack find by id");
			}
		} else {
			throw new SLException("600012", "no redpack find by id");
		}
		return result;
	}
	
	/**
	 * 根据红包id获取答案列表
	 * @author SangLy
	 * @createTime 2016年11月2日 下午2:00:01
	 * @param redpackId
	 * @return
	 */
	public List<AdvertisementAnswer> findAnswerListByRedpackId(String redpackId){
		return repository.findAnswerListByRedpackId(redpackId);
	}

	/**
	 * 红包订阅
	 * 
	 * @author SangLy
	 * @createTime 2016年8月31日 上午11:20:45
	 * @param redpackId
	 * @param userId
	 */
	public void appointment(String redpackId, String userId) {
		// 先判断此红包是否已经订阅
		List<Map<String, Object>> hongbaodingyueList = customerRelationService.findListCustomerIdAndRelateTableAndRelatePrimaryAndTypeAndDay(new Date(), userId, TableConstant.T_REDPACK, redpackId, CustomerRelationType.红包订阅);
		if (hongbaodingyueList.size() > 0) {
			throw new SLException("600011", "this Redpack have subscribed");
		}
		// 判断红包是否存在
		RedPack redpack = findById(redpackId);
		if (redpack == null) {
			throw new SLException("600012", "not found redpack by id");
		}
		Date systemTime = new Date();
		if (systemTime.getTime() > redpack.getTimePoint().getTime()) {
			throw new SLException("600017", "红包已经开启");
		}
		Date dayHour20 = DateUtil.weedDayBelow(new Date());
		Date dayHour21 = DateUtil.weedDayBelow(new Date());
		dayHour20.setHours(20);
		dayHour21.setHours(21);
		// 20点钟分享任务 --关联表为广告表,关联表主键是此红包的广告
		if (redpack.getTimePoint().getTime() == dayHour20.getTime()) {
			List<Map<String, Object>> hongbaofenxiangList = customerRelationService.findListCustomerIdAndRelateTableAndRelatePrimaryAndTypeAndDay(systemTime, userId, TableConstant.T_ADVERTISEMENT, redpack.getAdvertisementId(), CustomerRelationType.广告分享);
			if (hongbaofenxiangList.size() < 0) {
				throw new SLException("600021", "20点红包需要解锁");
			}
		}
		// 21点钟邀请任务-- 关联表为前端客户表
		if (redpack.getTimePoint().getTime() == dayHour21.getTime()) {
			List<Map<String, Object>> hongbaoyaoqingList = customerRelationService.findListCustomerIdAndRelateTableAndTypeAndDay(systemTime, userId, CustomerRelationType.好友邀请);
			List<Map<String, Object>> userRegisterLog = customerRelationService.findListRelatePrimaryAndRelateTableAndTypeAndDay(systemTime, userId, CustomerRelationType.好友邀请);
			if (hongbaoyaoqingList.size() < 0 && userRegisterLog.size() < 0) {
				throw new SLException("600022", "21点红包需要解锁");
			}
		}
		if (DateUtil.addMinutes(new Date(), RedPackConstant.BEFORE_START_REDPACK_TIME_MINITS_NOT_DINGYUE.intValue()).getTime() >= redpack.getTimePoint().getTime()) {
			throw new SLException("600018", RedPackConstant.BEFORE_START_REDPACK_TIME_MINITS_NOT_DINGYUE + "分钟之内无法订阅");
		} else {
			customerRelationService.saveCustomerRelation(userId, TableConstant.T_REDPACK, redpackId, CustomerRelationType.红包订阅);
		}
	}

	/**
	 * 修改红包广告信息
	 * @param id
	 * @param advertisementId
	 * @return
	 */
	@Transactional
	public RedPack updateRedPackAdvertisementInfoById(String id,String advertisementId){
		RedPack redPack = findById(id);
		if(null == redPack){
			throw new SLException("600003");
		}
		if(redPack.getUserType() != UserType.员工){
			throw new SLException("600049");
		}
		if (DateUtil.formatyyyyMMdd(redPack.getTimePoint()).compareTo(DateUtil.formatyyyyMMdd(new Date())) < 0 ) {
			throw new SLException("600009");
		}
		Advertisement advertisement = advertisementService.findById(advertisementId);
		if(null == advertisement){
			throw new SLException("500009");
		}
		Advertisement newAdvertisement = createCopyAdvertisement(advertisement);
		redPack.setAdvertisementId(newAdvertisement.getId());
		return save(redPack);
	}
	
	/**
	 * 红包审核
	 * @param id
	 * @param auditStatus
	 * @param memo
	 * @return
	 */
	@Transactional
	public RedPack auditRedPackById(String id, String auditStatus, String memo) {
		RedPack redPack = findById(id);
		if(null == redPack){
			throw new SLException("600003");
		}
		//红包的状态必须为待审核
		if(redPack.getStatus() != RedpackStatus.待审核){
			throw new SLException("600032");
		}
		//如果是审核驳回，必须填写理由
		if(RedpackStatus.valueOf(auditStatus) != RedpackStatus.审核通过 && StringUtils.isBlank(memo)){
			throw new SLException("600033");
		}
		//记账记流水
		String customerId = redPack.getCreatedUser();
		Customer customer = customerService.findById(customerId);
		//判断用户状态
		if(null == customer){
			throw new SLException("600060");
		}
		if(customer.getStatus() != CustomerStatus.正常){
			throw new SLException("600061");
		}
		//判断账户状态
		Account account = accountService.findBycustomerId(customerId);
		if(null == account){
			throw new SLException("600062");
		}
		//查询红包冻结流水
		AccountFlow redPackFreezeFlow = accountFlowService.findTopByRelatePrimaryAndTradeDirectionAndFlowType(id, AccountFlowTradeDirection.冻结, AccountFlowflowType.红包冻结);
		if(null == redPackFreezeFlow){
			throw new SLException("600063");
		}
		//查询订单信息
		Order order = orderService.findByRelatePrimary(id);
		if(null == order){
			throw new SLException("600064");
		}
		//为了适配在红包生成之后，红包手续费比变动问题，扣除手续费的时候需安装订单来扣除
		OrderDetail redPackOrderDetai = orderDetailService.findByOrderIdAndCategory(order.getId(), OrderDetailCategory.发红包);
		if(null == redPackOrderDetai){
			throw new SLException("红包订单详情不存在，审核失败");
		}
		//手续费详情可能为空，故不做是否为空判断
		OrderDetail redPackPoundageOrderDetai = orderDetailService.findByOrderIdAndCategory(order.getId(), OrderDetailCategory.发红包手续费);
		
		redPack.setStatus(RedpackStatus.valueOf(auditStatus));
		redPack.setMemo(memo);
		
		//判断并记录审核结果，并记录流水
		//先解冻红包并记录流水
		accountService.saveAppRedPackUnFreezeFlow(account, redPack, redPackFreezeFlow);
		if(auditStatus.equals(RedpackStatus.审核通过.name())){
			//发红包、手续费流水
			accountService.saveAppSendRedPackFlow(account, redPack,redPackOrderDetai,redPackPoundageOrderDetai);
			//发送站内信
			messageService.sendMessage(MessageTemplateMessageType.红包通过审核, customerId, redPack.getId());
		}else{
			//发送站内信
			messageService.sendMessage(MessageTemplateMessageType.红包被驳回, customerId, redPack.getId());
		}
		return save(redPack);
	}
	
	/**
	 * 根据红包id和用户id判断红包是否订阅
	 * 
	 * @author SangLy
	 * @createTime 2016年11月2日 下午6:16:41
	 * @param redpackId
	 * @param userId
	 * @return
	 */
	public Boolean hasAppointment(String redpackId, String userId) {
		List<Map<String, Object>> hongbaodingyueList = customerRelationService.findListCustomerIdAndRelateTableAndRelatePrimaryAndTypeAndDay(new Date(), userId, TableConstant.T_REDPACK, redpackId, CustomerRelationType.红包订阅);
		if (hongbaodingyueList.size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * app-移除红包订单
	 * 
	 * RedpackStatus.已失效 RedpackRecordStatus.删除
	 * 
	 * @author SangLy
	 * @createTime 2016年11月3日 下午8:13:26
	 * @param redpackId
	 * @param redpackId
	 */
	@Transactional
	public void removeRedPackById(String redpackId, String customerId) {
		RedPack redpack = repository.findOne(redpackId);
		if (redpack == null) {
			throw new SLException("600003", "redpack not found");
		} else {
			if (customerId.equals(redpack.getCreatedUser())) {
				redpack.setStatus(RedpackStatus.已失效);
				redpack.setRecordStatus(RedpackRecordStatus.删除);
				repository.save(redpack);
			} else {
				throw new SLException("600003", "redpack not found");
			}
		}
	}
	
	/**
	 * app-取消红包订单
	 * 
	 * RedpackStatus.已失效
	 * 
	 * @author SangLy
	 * @createTime 2016年11月3日 下午8:13:26
	 * @param redpackId
	 * @param redpackId
	 */
	@Transactional
	public void cancelOrderRedPackById(String redpackId, String customerId) {
		RedPack redpack = repository.findOne(redpackId);
		if (redpack == null) {
			throw new SLException("600003", "redpack not found");
		} else {
			if (customerId.equals(redpack.getCreatedUser())) {
				redpack.setStatus(RedpackStatus.已失效);
				redpack.setMemo("客户主动取消红包");
				repository.save(redpack);
			} else {
				throw new SLException("600003", "redpack not found");
			}
		}
	}
	
	/**
	 * app-首页突出红包展示
	 * 
	 * @author SangLy
	 * @createTime 2016年11月4日 上午10:28:39
	 * @param redpackType
	 * @return
	 */
	public RedPack findNextRedpack(RedpackType redpackType){
		Date day = DateUtil.weedDayBelow(new Date());
		return repository.findNextRedpack(day,RedpackStatus.未开启.toString(),RedpackRecordStatus.正常.toString(),redpackType.toString());
	}
	
	
	/**
	 * app-首页突出红包展示(全部)
	 * 
	 * @author SangLy
	 * @createTime 2016年11月4日 上午10:28:39
	 * @param redpackType
	 * @return
	 */
	public RedPack findNextRedpackForAll(){
		Date day = DateUtil.weedDayBelow(new Date());
		return repository.findNextRedpackForAll(day,RedpackStatus.未开启.toString(),RedpackRecordStatus.正常.toString());
	}
	
	/**
	 * app-中奖名单
	 * @author SangLy
	 * @createTime 2016年11月4日 下午4:18:53
	 * @param pageRequest
	 * @return
	 */
	public PageResponse<Map<String, Object>> winnersRedpackList(PageRequestVo pageRequest){
		return repository.winnersRedpackList(pageRequest);
	}
	
	/**
	 * app-红包订单列表
	 *
	<pre>
	  	待付款-待付款
		已失效-已失效
		待审核-待审核
		已通过-审核通过、未开启、正开启、已开启、已抢完、已过期
		被驳回-审核驳回
	</pre>
	 * 
	 * @author SangLy
	 * @createTime 2016年11月3日 下午8:13:26
	 * @param advertisementId
	 * @param customerId
	 */
	public PageResponse<Map<String, Object>> orderListByCustomerId(PageRequestVo pageRequest,String customerId){
		String redpackOrderType = MapUtils.getString(pageRequest.getParams(), "redpackOrderType");
		if("被驳回".equals(redpackOrderType)){
			redpackOrderType = RedpackStatus.审核驳回.toString();
		}
		PageResponse<Map<String, Object>> result = null;
		pageRequest.addParam("recordStatus", RedpackRecordStatus.正常.toString());
		if(RedpackStatus.待付款.toString().equals(redpackOrderType) || RedpackStatus.已失效.toString().equals(redpackOrderType) || RedpackStatus.待审核.toString().equals(redpackOrderType) || RedpackStatus.审核驳回.toString().equals(redpackOrderType)){
			List<String> status = new ArrayList<String>();
			status.add(RedpackStatus.valueOf(redpackOrderType).toString());
			pageRequest.addParam("status", status);
			pageRequest.addParam("customerId", customerId);
			result = repository.orderListByCustomerId(pageRequest);
		}else if ("已通过".equals(redpackOrderType)){
			List<String> status = new ArrayList<String>();
			status.add(RedpackStatus.审核通过.toString());
			status.add(RedpackStatus.未开启.toString());
			status.add(RedpackStatus.正开启.toString());
			status.add(RedpackStatus.已抢完.toString());
			status.add(RedpackStatus.已过期.toString());
			pageRequest.addParam("status", status);
			pageRequest.addParam("customerId", customerId);
			result = repository.orderListByCustomerId(pageRequest);
		}else{
			//未知状态
			throw new SLException("600047","not found status");
		}
		if(result != null){
			List<Map<String,Object>> list = result.getData();
			for(Map<String,Object> map: list){
				map.put("systemTime", new Date());
				map.put("redpackOrderType",redpackOrderType);
			}
		}
		return result;
	}
	
	/**
	 * 当天红包预分配表添加记录
	 * 
	 * @author SangLy
	 * @createTime 2016年11月11日 下午5:24:03
	 */
	@Transactional
	public void redPackDistributeJob() {
		List<RedPack> list = findByTimePointAndRecordStatusAndStatusAndRedpackTypeNotInOrderByCreatedDateAsc();
		Date today = DateUtil.weedDayBelow(new Date());
		int temp = 0;
		Date targetRedpackTimePoint;
		for (int i = 0; i < list.size(); i++) {
			RedPack redPack = list.get(i);
			if (temp < RedPackConstant.REDPACK_TIME_POINT.length) {
				targetRedpackTimePoint = RedPackUtils.getTimePoint(today, RedPackConstant.REDPACK_TIME_POINT[temp]);
				saveDistributeAndupdateRedpack(redPack, targetRedpackTimePoint);
				temp++;
			} else {
				// 重新计数
				temp = 0;
				targetRedpackTimePoint = RedPackUtils.getTimePoint(today, RedPackConstant.REDPACK_TIME_POINT[temp]);
				saveDistributeAndupdateRedpack(redPack, targetRedpackTimePoint);
				temp++;
			}
		}
		// 查询分享红包，固定分配到20点
		List<RedPack> fenXingRedpackList = repository.findByTimePointAndRecordStatusAndStatusAndRedpackTypeOrderByCreatedDateAsc(today, RedpackRecordStatus.正常, RedpackStatus.审核通过, RedpackType.分享红包);
		for (RedPack fenXingRedpack : fenXingRedpackList) {
			saveDistributeAndupdateRedpack(fenXingRedpack, DateUtils.addHours(today, 20));
		}
		// 查询分享红包，固定分配到21点
		List<RedPack> yaoQingRedpackList = repository.findByTimePointAndRecordStatusAndStatusAndRedpackTypeOrderByCreatedDateAsc(today, RedpackRecordStatus.正常, RedpackStatus.审核通过, RedpackType.邀请红包);
		for (RedPack yaoQingRedpack : yaoQingRedpackList) {
			saveDistributeAndupdateRedpack(yaoQingRedpack, DateUtils.addHours(today, 21));
		}
	}
	
	/**
	 * 查询当天需要生成红分配记录表的红包
	 */
	public List<RedPack> findByTimePointAndRecordStatusAndStatusAndRedpackTypeNotInOrderByCreatedDateAsc() {
		List<RedpackType> redpackType = new ArrayList<RedpackType>();
		redpackType.add(RedpackType.邀请红包);
		redpackType.add(RedpackType.分享红包);
		Date today = DateUtil.weedDayBelow(new Date());
		return repository.findByTimePointAndRecordStatusAndStatusAndRedpackTypeNotInOrderByCreatedDateAsc(today, RedpackRecordStatus.正常, RedpackStatus.审核通过, redpackType);
	}
	
	/**
	 * 根据红包id生成预分配记录，和更新红包
	 * @author SangLy
	 * @createTime 2016年11月11日 下午5:53:02
	 * @param redPack
	 */
	@Transactional
	private void saveDistributeAndupdateRedpack(RedPack redPack,Date targetRedpackTimePoint){
		// 生成红包分配表
		Integer bigCount = redPack.getBigCount();
		Integer smallCount = redPack.getSmallCount();
		Integer totalCount = bigCount + smallCount;

		for (int j = 0; j < totalCount; j++) {
			if (j < bigCount) {
				//设置大奖
				Distribute distribute = new Distribute();
				distribute.setRedpackId(redPack.getId());
				distribute.setAmount(redPack.getBigAmount());
				distribute.setOrdered(j);
				distribute.setCreatedUser(UserType.robot.toString());
				distributeService.save(distribute);
			} else {
				//设置小奖
				Distribute distribute = new Distribute();
				distribute.setRedpackId(redPack.getId());
				distribute.setAmount(redPack.getSmallAmount());
				distribute.setOrdered(j);
				distribute.setCreatedUser(UserType.robot.toString());
				distributeService.save(distribute);
			}
		}
		redPack.setTimePoint(targetRedpackTimePoint); // 修改红包开启时间
		RedpackType redpackType = redPack.getRedpackType();
		redPack.setOrdered(RedPackUtils.setRedPackOrderedValueOfNotOpen(redpackType));
		redPack.setStatus(RedpackStatus.未开启);
		repository.save(redPack);
	}
	
	/**
	 * 
	 * @author 30分钟未支付,修改红包状态
	 * @createTime 2016年11月17日 下午5:09:28
	 * @param redpack
	 */
	@Transactional
	public void modifyCustomerRedpackOrderPayStatusThreadSevice(RedPack redpack){
		redpack.setStatus(RedpackStatus.已失效);
		redpack.setMemo("订单生成30分钟内未支付，自动失效");
		repository.save(redpack);
		Order order = orderService.findByRelatePrimary(redpack.getId());
		if (order != null) {
			order.setOrderStatus(OrderStatus.失效);
			orderService.save(order);
		}
		operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "订单大于30分钟，红包状态和订单状态设置成已失效"));
	}
	
	/**
	 * 查询最早一条某种状态的订单，例如最早一条没有支付的订单（例如现在时间是11点9点钟和10点钟都有一条"待付款"订单则查询出9点的记录）
	 * 
	 * @author SangLy
	 * @createTime 2016年11月18日 上午10:18:58
	 * @param redpackStatus
	 * @return
	 */
	public RedPack findCloselyHaveNotPayOfCustomerOrder(RedpackStatus redpackStatus,RedpackRecordStatus recordStatus){
		return repository.findCloselyHaveNotPayOfCustomerOrder(redpackStatus.toString(),recordStatus.toString());
	}
	
	/**
	 * 查询最近一条没有发送支付提醒的订单
	 * 
	 * @author SangLy
	 * @createTime 2016年11月18日 上午10:18:58
	 * @param redpackStatus
	 * @return
	 */
	public RedPack findCloselyHaveNotRemindMessageOfCustomerOrderOutOfTime(RedpackStatus redpackStatus,RedpackRecordStatus recordStatus){
		return repository.findCloselyHaveNotRemindMessageOfCustomerOrderOutOfTime(redpackStatus.toString(),recordStatus.toString());
	}
	
	/**
	 * 红包设置过期
	 * 
	 * @author samson
	 * @createTime 2016年11月21日 下午2:41:32
	 * @param date
	 */
	public void expire(Date now) {
		Date startDate = DateUtil.weedDayBelow(DateUtil.addDays(now, -1));
		Date endDate = DateUtil.addSeconds(DateUtil.addDays(startDate, 1), -1);
		List<RedPack> redPacks = repository.findAllByTimePointBetweenAndStatusIn(startDate, endDate, Lists.newArrayList(RedpackStatus.未开启, RedpackStatus.正开启, RedpackStatus.已开启));
		for (RedPack redPack : redPacks) {
			redPack.setStatus(RedpackStatus.已过期);

			// 单个保存单开事务
			save(redPack);
			operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.红包设置过期, redPack.getId(), UserType.robot.name(), new Date(), null));
		}
	}
	
	/**
	 * 找出最近一条，客户没有抢完的红包，结构为，红包id，需要返的钱是多少
	 * 
	 * @author SangLy
	 * @createTime 2016年11月21日 上午10:35:00
	 * @return
	 */
	public List<Map<String, Object>> findCloselyCustomerNotRobRedpackMoney() {
		return repository.findCloselyCustomerNotRobRedpackMoney(new Date());
	}
	
	/**
	 * 未抢完的红包返现
	 * 
	 * @author SangLy
	 * @createTime 2016年11月21日 上午10:37:05
	 */
	@Transactional
	public void returnCustomerNotRobRedpackMoney(Map<String, Object> map) {
		if (map != null) {
			String createdUserId = repository.findOne(MapUtils.getString(map, "redpackId")).getCreatedUser();
			if (StringUtils.isNotBlank(createdUserId)) {
				accountService.returnCustomerNotRobRedpackMoney(createdUserId, MapUtils.getString(map, "redpackId"), MapUtils.getDoubleValue(map, "amount"));
				// 2.红包分布表自动改成，机器人抢过
				List<Distribute> distributeList = distributeService.findByAssigneeAndRedpackId(null, MapUtils.getString(map, "redpackId"));
				for (Distribute distribute : distributeList) {
					distribute.setAssignee(UserType.robot.toString());
					distributeService.save(distribute);
				}
			}

		}
	}
	
	/**
	 * 未抢完的红包返现
	 * 
	 * @author 返现job
	 * @createTime 2016年11月21日 下午2:49:31
	 */
	public void returnCustomerNotRobRedpackMoneyJob() {
		List<Map<String, Object>> listMap = findCloselyCustomerNotRobRedpackMoney();
		for (Map<String, Object> map : listMap) {
			returnCustomerNotRobRedpackMoney(map);
		}
	}
	
}
