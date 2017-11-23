/** 
 * @(#)AdvertisementVo.java 1.0.0 2016年8月16日 上午11:59:05  
 *  
 * Copyright © 2016 善林金融.  All rights reserved.  
 */ 

package com.slfinance.redpack.core.vo;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.validator.constraints.NotBlank;

/**   
 * 
 *  
 * @author  taoxm
 * @version $Revision:1.0.0, $Date: 2016年8月16日 上午11:59:05 $ 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementVo {
	private String id;
	
	@NotBlank(message="500013")
	private String advertiserName;
	
	@NotBlank(message="500014")
	private String logo;
	
	@NotBlank(message="500015")
	private String title;
	
	@NotBlank(message="500001")
	private String type;
	
	private String content;
	
	private String startDate;
	
	private String endDate;

	@NotNull(message="500003")
	private List<Map<String, Object>> files;
	
	private String hyperlink;
	
	private List<Map<String, Object>> answers;
	
	private String correctAnswer;
	
	
}
