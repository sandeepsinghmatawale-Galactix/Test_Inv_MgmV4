package com.barinventory.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.barinventory.entities.InventorySession;
import com.barinventory.entities.StockroomInventory;

@Repository
public interface StockroomInventoryRepository 
        extends JpaRepository<StockroomInventory, Long> {
	
	List<StockroomInventory> findBySessionSessionId(Long sessionId);
	
	Optional<StockroomInventory> 
	findBySessionSessionIdAndBrandBrandId(Long sessionId, Long brandId);
	
	@Query("""
			SELECT s
			FROM StockroomInventory s
			WHERE s.session.sessionId = :sessionId
			""")
			List<StockroomInventory> getPreviousSessionStocks(Long sessionId);
	
	@Query("""
			SELECT s
			FROM StockroomInventory s
			WHERE s.session.sessionId = :sessionId
			AND s.saleStock > 0
			""")
			List<StockroomInventory> findDistributableStocks(Long sessionId);
	
	@Query("""
			SELECT s
			FROM StockroomInventory s
			JOIN FETCH s.brand
			WHERE s.session.sessionId = :sessionId
			""")
			List<StockroomInventory> findBySessionWithBrand(Long sessionId);
	
	 

}
