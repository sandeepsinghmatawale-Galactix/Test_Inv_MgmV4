package com.barinventory.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.barinventory.entities.Distribution;

@Repository
public interface DistributionRepository 
        extends JpaRepository<Distribution, Long> {
	
	
	Optional<Distribution> findBySessionSessionId(Long sessionId);
	
	Optional<Distribution> findTopByOrderByDistributionIdDesc();
	
	 Optional<Distribution> findByDistributionId(
	            Long distributionId
	    );
	 
	 
	 
	 @Query("""
			 SELECT COALESCE(SUM(wd.distributedQty), 0)
			 FROM WellDistribution wd
			 WHERE wd.distribution.distributionId = :distributionId
			 AND wd.brand.brandId = :brandId
			 """)
			 Integer getTotalDistributedQty(Long distributionId, Long brandId);
}
