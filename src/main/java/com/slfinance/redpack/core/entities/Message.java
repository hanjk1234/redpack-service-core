/** 
 * @(#)Message.java 1.0.0 2016年11月1日 下午1:44:13  
 *  
 * Copyright © 2016 善林金融.  All rights reserved.  
 */

package com.slfinance.redpack.core.entities;

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
 * @version $Revision:1.0.0, $Date: 2016年11月1日 下午1:44:13 $
 */
@Entity
@Table(name = "RP_T_MESSAGE")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Message extends BaseEntity {

	private static final long serialVersionUID = 6915737542774982806L;

	private String customerId;
	private String content;
	private String relatePrimary;
	private String messageTemplateId;

	/**
	 * 是否已读 1:已读 0:未读
	 */
	private Integer isRead = 0;

	public Message(String customerId, String relatePrimary, String messageTemplateId, String content) {
		this.customerId = customerId;
		this.relatePrimary = relatePrimary;
		this.messageTemplateId = messageTemplateId;
		this.content = content;
	}

}
