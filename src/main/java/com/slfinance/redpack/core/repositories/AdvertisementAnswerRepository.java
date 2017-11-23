package com.slfinance.redpack.core.repositories;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.slfinance.redpack.core.entities.AdvertisementAnswer;
import com.slfinance.redpack.core.extend.jpa.page.QueryExtend;
import com.slfinance.redpack.core.repositories.base.BaseRepository;

public interface AdvertisementAnswerRepository extends BaseRepository<AdvertisementAnswer> {

	@QueryExtend()
	@Query(nativeQuery=true,value="select t.id,t.answer_content from rp_t_advertisement_answer t where t.advertisement_id = :advertisementId")
	public List<Map<String, Object>> findByAdvertisementIdForMap(@Param("advertisementId")String advertisementId);

	
	List<AdvertisementAnswer> findByAdvertisementId(String advertisementId);
}
