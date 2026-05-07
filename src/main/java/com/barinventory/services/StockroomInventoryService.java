package com.barinventory.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.barinventory.dtos.StockroomClosingRequest;
import com.barinventory.entities.InventorySession;
import com.barinventory.entities.StockroomInventory;
import com.barinventory.repos.DistributionRepository;
import com.barinventory.repos.InventorySessionRepository;
import com.barinventory.repos.StockroomInventoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StockroomInventoryService {

	private final StockroomInventoryRepository stockroomRepo;
	private final InventorySessionRepository sessionRepo;
	private final DistributionRepository distributionRepo;

	public void initializeStockroom(Long currentSessionId, Long previousSessionId) {

		List<StockroomInventory> previousStocks = stockroomRepo.findBySessionSessionId(previousSessionId);

		InventorySession currentSession = sessionRepo.findById(currentSessionId).orElseThrow();

		for (StockroomInventory previous : previousStocks) {

		    boolean exists = stockroomRepo
		        .existsByBarBarIdAndSessionSessionIdAndBrandBrandId(
		            currentSession.getBar().getBarId(),
		            currentSessionId,
		            previous.getBrand().getBrandId()
		        );

		    if (exists) {
		        continue;
		    }

		    StockroomInventory current = new StockroomInventory();

		    current.setSession(currentSession);
		    current.setBrand(previous.getBrand());
		    current.setBar(currentSession.getBar());
		    current.setOpeningStock(previous.getClosingStock());
		    current.setReceivedStock(0);
		    current.setClosingStock(0);
		    current.setSaleStock(0);

		    stockroomRepo.save(current);
		}
	}

	public void save(StockroomInventory stock){
	    stockroomRepo.save(stock);
	}
	
	public List<StockroomInventory> getStockroomByBarAndSession(Long barId, Long sessionId) {
	    return stockroomRepo.findByBarBarIdAndSessionSessionId(barId, sessionId);
	}
	
	public List<StockroomInventory> getStockroomBySession(Long sessionId) {
		return stockroomRepo.findBySessionSessionId(sessionId);
	}

	 
	// rename from updateClosingStock → updateStockroomClosing, add barId
	public void updateStockroomClosing(Long barId, Long sessionId,
	                                    List<StockroomClosingRequest> requests) {

	    // ✅ validate session belongs to bar
	    InventorySession session = sessionRepo.findById(sessionId)
	            .orElseThrow(() -> new RuntimeException("Session not found"));

	    if (!session.getBar().getBarId().equals(barId)) {
	        throw new RuntimeException("Session does not belong to this bar");
	    }

	    for (StockroomClosingRequest request : requests) {
	        StockroomInventory stock = stockroomRepo
	                .findBySessionSessionIdAndBrandBrandId(sessionId, request.getBrandId())
	                .orElseThrow(() -> new RuntimeException("Stock not found"));

	        int totalAvailable = stock.getOpeningStock() + stock.getReceivedStock();

	        if (request.getClosingStock() > totalAvailable) {
	            throw new RuntimeException("Invalid closing stock for brand: "
	                    + stock.getBrand().getBrandName());
	        }

	        stock.setClosingStock(request.getClosingStock());
	        stock.setSaleStock(totalAvailable - request.getClosingStock());
	        // ✅ no explicit save() needed — JPA dirty tracking handles it
	    }
	}
	
	public Map<Long, Integer> getSaleStockMap(Long distributionId) {

	    // 1. Get sessionId from distribution
	    Long sessionId = distributionRepo
	            .findById(distributionId)
	            .orElseThrow()
	            .getSession()
	            .getSessionId();

	    // 2. Get stockroom data
	    List<StockroomInventory> stocks =
	            stockroomRepo.findBySessionSessionId(sessionId);

	    // 3. Convert to Map<brandId, saleStock>
	    Map<Long, Integer> stockMap = new HashMap<>();

	    for (StockroomInventory stock : stocks) {
	        stockMap.put(
	                stock.getBrand().getBrandId(),
	                stock.getSaleStock()
	        );
	    }

	    return stockMap;
	}
	
	
}