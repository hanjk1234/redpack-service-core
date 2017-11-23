/** 
 * @(#)Distribute.java 1.0.0 2016年11月1日 上午11:09:02  
 *  
 * Copyright © 2016 善林金融.  All rights reserved.  
 */

package com.slfinance.redpack.core.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.slfinance.redpack.core.entities.base.BaseEntity;

/**
 * 
 * 
 * @author SangLy
 * @version $Revision:1.0.0, $Date: 2016年11月1日 上午11:09:02 $
 */
@Entity
@Table(name = "RP_T_DISTRIBUTE")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Distribute extends BaseEntity {

	private static final long serialVersionUID = -5000000000000000008L;

	/**
	 * 红包ID
	 */
	private String redpackId;

	/**
	 * 分配到的用户ID
	 */
	private String assignee;

	/**
	 * 金额
	 */
	private Double amount;

	/**
	 * 排序
	 */
	private Integer ordered;
	/**
	 * 分配时间
	 */
	private Date distributeTime;

}