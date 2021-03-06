package com.slfinance.redpack.core.services;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slfinance.redpack.core.constants.enums.AccountFlowTradeDirection;
import com.slfinance.redpack.core.constants.enums.AccountFlowflowType;
import com.slfinance.redpack.core.entities.Account;
import com.slfinance.redpack.core.entities.AccountFlow;
import com.slfinance.redpack.core.extend.jpa.page.PageRequestVo;
import com.slfinance.redpack.core.extend.jpa.page.PageResponse;
import com.slfinance.redpack.core.repositories.AccountFlowRepository;
import com.slfinance.redpack.core.services.base.BaseService;

@Service
public class AccountFlowService extends BaseService<AccountFlow, AccountFlowRepository> {

	public AccountFlow addFlow(Account account, AccountFlowflowType flowType, AccountFlowTradeDirection tradeDirection, String targetCustomerId, String oldFlowCode) {
		return null;
	}

	/**
	 * app-账单列表
	 * 
	 * @author SangLy
	 * @createTime 2016年11月7日 下午5:18:10
	 * @param customerId
	 * @return
	 */
	public PageResponse<Map<String, Object>> flowDetail(PageRequestVo pageRequest) {

		return repository.flowDetail(pageRequest);
	}

	@Transactional
	public List<AccountFlow> save(Iterable<AccountFlow> accountFlows) {
		return repository.save(accountFlows);
	}
	
	/**
	 * 根据关联主键查询流水
	 * @param relatePrimary
	 * @return
	 */
	public AccountFlow findByRelatePrimary(String relatePrimary){
		return repository.findByRelatePrimary(relatePrimary);
	}
	
	public AccountFlow findTopByRelatePrimaryAndTradeDirectionAndFlowType(String relatePrimary, AccountFlowTradeDirection tradeDirection,AccountFlowflowType flowType){
		return repository.findTopByRelatePrimaryAndTradeDirectionAndFlowTypeOrderByCreatedDateDesc(relatePrimary,tradeDirection,flowType);
	}

}
