/** 
 * @(#)UserServiceTest.java 1.0.0 2016年8月26日 上午9:01:43  
 *  
 * Copyright © 2016 善林金融.  All rights reserved.  
 */ 

package com.slfinance.redpack.core.services;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.slfinance.redpack.common.utils.DigestUtil;
import com.slfinance.redpack.core.ServiceCoreApplicationTests;
import com.slfinance.redpack.core.entities.User;
import com.slfinance.redpack.core.vo.UpdatePasswordUserVo;

/**   
 * 
 *  
 * @author  work
 * @version $Revision:1.0.0, $Date: 2016年8月26日 上午9:01:43 $ 
 */
public class UserServiceTest extends ServiceCoreApplicationTests{
	
	@Autowired
	private UserService userService;
	
	@Test
	public void testSave(){
		User user = new User();
		user.setLoginName("taoxm");
		user.setMobile("13939395959");
		user.setEmail("gugo@sina.com");
		user.setPassword(DigestUtil.encryptPassword("123456789"));
		userService.save(user);
	}
	
	@Test
	public void testUpdatePassowrd(){
		UpdatePasswordUserVo vo = new UpdatePasswordUserVo();
		vo.setId("4090829356c462f30156c463011d0000");
		vo.setOldPassword("123456789");
		vo.setPassword("987654321");
		userService.updatePassowrd(vo);
	}
	
	
}
