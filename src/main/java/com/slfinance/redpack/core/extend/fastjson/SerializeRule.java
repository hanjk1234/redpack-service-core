/**
 * @(#)SerializeRule.java 1.0.0 2015年12月14日 下午6:13:06
 * <p/>
 * Copyright © 2015 善林金融.  All rights reserved.
 */

package com.slfinance.redpack.core.extend.fastjson;

import java.lang.annotation.*;

/**
 * 序列化规则
 *
 * @author samson
 * @version $Revision:1.0.0, $Date: 2015年12月14日 下午6:13:06 $
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SerializeRule {

	/**
	 * 指定类的规则 如果为默认值则默认为所有对象的指定属性
	 *
	 * @return
	 */
	Class<?> clazz() default Object.class;

	/**
	 * 序列化时包括的key集合
	 *
	 * @return
	 */
	String[] include() default {};

	/**
	 * 序列化时排除的key集合
	 *
	 * @return
	 */
	String[] exclude() default {};

}
