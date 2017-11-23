/** 
 * @(#)ModifyRedPackStatusOpeningThread.java 1.0.0 2016年8月22日 下午5:16:34  
 *  
 * Copyright © 2016 善林金融.  All rights reserved.  
 */

package com.slfinance.redpack.core.thread;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.slfinance.redpack.common.utils.DateUtil;
import com.slfinance.redpack.core.constants.RedPackConstant;
import com.slfinance.redpack.core.constants.enums.LogType;
import com.slfinance.redpack.core.constants.enums.RedpackStatus;
import com.slfinance.redpack.core.constants.enums.UserType;
import com.slfinance.redpack.core.entities.OperateLog;
import com.slfinance.redpack.core.entities.RedPack;
import com.slfinance.redpack.core.services.OperateLogService;
import com.slfinance.redpack.core.services.RedPackService;
import com.slfinance.redpack.core.utils.RedPackUtils;

/**
 * 定时扫描，最近一条红包"未开启"状态的红包并且修改此红包状态为"正开启"
 * 
 * @author SangLy
 * @version $Revision:1.0.0, $Date: 2016年8月22日 下午5:16:34 $
 */
public class ModifyRedPackStatusNotOpenToOpeningThread extends Thread {

	private final Log logger = LogFactory.getLog(ModifyRedPackStatusNotOpenToOpeningThread.class);

	private RedPackService redpackService;

	private OperateLogService operateLogService;

	private Date systemTime = new Date();

	private RedPack redpack = null;

	public ModifyRedPackStatusNotOpenToOpeningThread(RedPackService redpackService, OperateLogService operateLogService) {
		this.operateLogService = operateLogService;
		this.redpackService = redpackService;
		this.start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				redpack = redpackService.findCloselyDayHaveNotOpen(systemTime); // 距离当天时间最近的还没有开启的红包，例如系统时间11点。9点场次的红包由于某种原因是“未开启”状态，则查询出的是9点场次的红包
				systemTime = new Date();
				Date nextTimePointBysystemTime = RedPackUtils.getNextTimePoint(systemTime);
				Long sleepTime = 0L;
				if (redpack != null) {
					Date redpackTimePoint = redpack.getTimePoint();
					Date nextTimePointByRedpackTimePoint = RedPackUtils.getNextTimePoint(redpack.getTimePoint());
					if ((nextTimePointBysystemTime != null) && (redpackTimePoint != null) && (nextTimePointByRedpackTimePoint != null)) {
						// 还未到开启时间的红包
						if (systemTime.getTime() <= redpack.getTimePoint().getTime()) {
							sleepTime = redpackTimePoint.getTime() - systemTime.getTime();
							if (sleepTime > 0) {
								Thread.sleep(sleepTime);
							}
							// 修改红包状态，有未开启，到正开启
							redpack.setStatus(RedpackStatus.正开启);
							redpack.setOrdered(RedPackUtils.setRedPackOrderedValueOfOpening(redpack.getRedpackType()));
							redpackService.update(redpack);
							operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "ModifyRedPackStatusNotOpenToOpeningThread 由未开启状态改成正开启"));

						} else {
							// 已经过了开启时间的红包。这个时候红包由2中状态，一种。正开启。一种已经开启
							if (nextTimePointByRedpackTimePoint.getTime() < nextTimePointBysystemTime.getTime()) {
								// 由于某种原因倒点未开启的红包而没有变成正开启则直接变成已开启
								redpack.setStatus(RedpackStatus.已开启);
								redpack.setOrdered(RedPackUtils.setRedPackOrderedValueOfOpened(redpack.getRedpackType()));
								redpackService.update(redpack);
								operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "ModifyRedPackStatusNotOpenToOpeningThread 由于某种原因倒点未开启的红包而没有变成正开启则直接变成已开启"));
							} else {
								// 距离下一个红包小于10分钟，红包状态为，已开启
								if ((nextTimePointBysystemTime.getTime() - systemTime.getTime() !=0) && ((nextTimePointBysystemTime.getTime() - systemTime.getTime() <= 10 * 60000))) {
									redpack.setStatus(RedpackStatus.已开启);
									redpack.setOrdered(RedPackUtils.setRedPackOrderedValueOfOpened(redpack.getRedpackType()));
									redpackService.update(redpack);
									operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "ModifyRedPackStatusNotOpenToOpeningThread 距离下一个红包小于10分钟，红包状态为，已开启"+(nextTimePointBysystemTime.getTime()+"---"+systemTime.getTime()) ));
								} else {
									redpack.setStatus(RedpackStatus.正开启);
									redpack.setOrdered(RedPackUtils.setRedPackOrderedValueOfOpening(redpack.getRedpackType()));
									redpackService.update(redpack);
									operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "ModifyRedPackStatusNotOpenToOpeningThread 由未开启状态直接更改为正开启状态"));
								}
							}

						}

					} else {
						if ((redpackTimePoint == null) && (nextTimePointBysystemTime == null)) {
							// 当天已经没有红包了,休眠一个小时做刷新
							Thread.sleep(1000 * 60 * 60);
						} else if ((redpackTimePoint != null) && (nextTimePointByRedpackTimePoint != null)) {
							// 当前时间下已经没有下一场了。并且查询到的红包场次不 是最后一场，直接修改成，已开启
							redpack.setStatus(RedpackStatus.已开启);
							redpack.setOrdered(RedPackUtils.setRedPackOrderedValueOfOpened(redpack.getRedpackType()));
							redpackService.update(redpack);
							operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "ModifyRedPackStatusNotOpenToOpeningThread 当前时间下已经没有下一场了。并且查询到的红包场次不 是最后一场，直接修改成，已开启"));
						} else if ((redpackTimePoint == null) && (nextTimePointBysystemTime != null)) {
							// 红包已经发放完,到下一个红包发放时间点做刷新操作
							sleepTime = nextTimePointBysystemTime.getTime() - systemTime.getTime();
							if (sleepTime > 0) {
								Thread.sleep(sleepTime);
							}
						} else if ((redpackTimePoint != null) && (nextTimePointBysystemTime == null) && (nextTimePointByRedpackTimePoint == null)) {
							// 最后一场红包，修改成正开启
							redpack.setStatus(RedpackStatus.正开启);
							redpack.setOrdered(RedPackUtils.setRedPackOrderedValueOfOpening(redpack.getRedpackType()));
							redpackService.update(redpack);
							operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "ModifyRedPackStatusNotOpenToOpeningThread 最后一场红包，修改成正开启"));
						}
					}
				} else {
					// 休眠4分钟后做刷新
					sleep(RedPackConstant.REDPACK_OPENPOINT_BEFORE_NOW_FRESH * DateUtil.MINUTE_MILISECONDS);
				}

			} catch (Exception ex) {
				try {
					if (redpack != null) {
						redpackService.update(redpack);
						operateLogService.saveAsync(new OperateLog(UserType.robot, LogType.自动更改红包状态, redpack.getId(), null, new Date(), "ModifyRedPackStatusNotOpenToOpeningThread 更改红包状态"));
					}
				} catch (Exception e) {
					logger.error("redpack update error", e);
				}
			}
		}
	}
}