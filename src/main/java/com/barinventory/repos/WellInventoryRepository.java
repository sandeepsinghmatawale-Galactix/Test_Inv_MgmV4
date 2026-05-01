package com.barinventory.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barinventory.entities.WellInventory;

@Repository
public interface WellInventoryRepository 
        extends JpaRepository<WellInventory, Long> {
	
	List<WellInventory> 
	findBySessionSessionIdAndWellWellId(Long sessionId, Long wellId);
	
	Optional<WellInventory> 
	findBySessionSessionIdAndWellWellIdAndBrandBrandId(
	        Long sessionId,
	        Long wellId,
	        Long brandId
	);
	
	@Query("""
			SELECT w
			FROM WellInventory w
			WHERE w.well.wellId = :wellId
			AND w.session.sessionId = (
			    SELECT MAX(w2.session.sessionId)
			    FROM WellInventory w2
			    WHERE w2.well.wellId = :wellId
			)
			""")
			List<WellInventory> getPreviousWellInventory(Long wellId);
	
	boolean existsByWellWellIdAndBrandBrandId(
	        Long wellId,
	        Long brandId
	);

	 
	    List<WellInventory> findByWellWellIdAndSessionSessionId(
	            Long wellId,
	            Long sessionId
	    );
	    
	    List<WellInventory> findBySessionSessionId(Long sessionId);

	    @Query("SELECT wi FROM WellInventory wi JOIN FETCH wi.well WHERE wi.session.sessionId = :sessionId")
	    List<WellInventory> findBySessionSessionIdWithWell(@Param("sessionId") Long sessionId);
 
}
