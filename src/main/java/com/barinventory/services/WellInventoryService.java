package com.barinventory.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.barinventory.dtos.WellClosingRequest;
import com.barinventory.entities.InventorySession;
import com.barinventory.entities.InventoryStatus;
import com.barinventory.entities.Well;
import com.barinventory.entities.WellDistribution;
import com.barinventory.entities.WellInventory;
import com.barinventory.repos.InventorySessionRepository;
import com.barinventory.repos.WellDistributionRepository;
import com.barinventory.repos.WellInventoryRepository;
import com.barinventory.repos.WellRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WellInventoryService {

	private final WellRepository wellRepo;
	private final WellInventoryRepository wellInventoryRepo;
	private final WellDistributionRepository wellDistributionRepo;
	private final InventorySessionRepository sessionRepo;

	/*
	 * Step 1: Initialize well inventory
	 */
	public void initializeWellInventory(Long sessionId, Long wellId) {
		
		System.out.println("Initializing well: " + wellId);

		InventorySession session = sessionRepo.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Session not found"));

		Well well = wellRepo.findById(wellId).orElseThrow(() -> new RuntimeException("Well not found"));

		// ✅ 🔒 GUARD FIRST (before any insert)
		List<WellInventory> existing = wellInventoryRepo.findBySessionSessionIdAndWellWellId(sessionId, wellId);

		boolean alreadyCompleted = !existing.isEmpty()
				&& existing.stream().allMatch(inv -> inv.getStatus() == InventoryStatus.COMPLETED);

		if (alreadyCompleted) {
			throw new RuntimeException("Well already completed. Cannot reopen.");
		}

		// ✅ If already initialized but not completed → SKIP re-initialization
		if (!existing.isEmpty()) {
			return;
		}

		// -----------------------------------

		List<WellInventory> previousInventory = wellInventoryRepo.getPreviousWellInventory(wellId);

		List<WellDistribution> distributedBrands = wellDistributionRepo.findByWellWellId(wellId);

		Set<Long> existingBrandIds = previousInventory.stream().map(i -> i.getBrand().getBrandId())
				.collect(Collectors.toSet());

		/*
		 * STEP 1: EXISTING BRANDS (UPSERT)
		 */
		for (WellInventory previous : previousInventory) {

			Integer receivedQty = distributedBrands.stream()
					.filter(d -> d.getBrand().getBrandId().equals(previous.getBrand().getBrandId()))
					.mapToInt(WellDistribution::getDistributedQty).sum();

			WellInventory inventory = new WellInventory(); // ✅ no need to search again

			inventory.setSession(session);
			inventory.setWell(well);
			inventory.setBrand(previous.getBrand());
			inventory.setOpeningStock(previous.getClosingStock());
			inventory.setReceivedStock(receivedQty);
			inventory.setClosingStock(0);
			inventory.setSaleStock(0);
			inventory.setStatus(InventoryStatus.IN_PROGRESS);

			wellInventoryRepo.save(inventory);
		}

		/*
		 * STEP 2: NEW BRANDS
		 */
		for (WellDistribution distribution : distributedBrands) {

			Long brandId = distribution.getBrand().getBrandId();

			if (!existingBrandIds.contains(brandId)) {

				WellInventory inventory = new WellInventory();

				inventory.setSession(session);
				inventory.setWell(well);
				inventory.setBrand(distribution.getBrand());
				inventory.setOpeningStock(0);
				inventory.setReceivedStock(distribution.getDistributedQty());
				inventory.setClosingStock(0);
				inventory.setSaleStock(0);
				inventory.setStatus(InventoryStatus.IN_PROGRESS); // ✅ IMPORTANT

				wellInventoryRepo.save(inventory);
			}
		}
	}

	/*
	 * Step 2: Get well inventory page
	 */
	public List<WellInventory> getWellInventory(Long sessionId, Long wellId) {
		return wellInventoryRepo.findBySessionSessionIdAndWellWellId(sessionId, wellId);
	}

	/*
	 * Step 3: Update closing stock
	 */
	@Transactional
	public void updateWellClosing(Long sessionId, Long wellId, List<WellClosingRequest> requests) {

		// Validate session & well
		sessionRepo.findById(sessionId).orElseThrow(() -> new RuntimeException("Session not found"));

		wellRepo.findById(wellId).orElseThrow(() -> new RuntimeException("Well not found"));

		// 🔹 STEP 1: Update each brand row
		for (WellClosingRequest request : requests) {

			WellInventory inventory = getInventory(sessionId, wellId, request.getBrandId());

			int totalAvailable = inventory.getOpeningStock() + inventory.getReceivedStock();

			if (request.getClosingStock() > totalAvailable) {
				throw new RuntimeException(
						"Closing stock cannot exceed available stock for brandId: " + request.getBrandId());
			}

			inventory.setClosingStock(request.getClosingStock());
			inventory.setSaleStock(totalAvailable - request.getClosingStock());

			// optional: mark IN_PROGRESS while editing
			inventory.setStatus(InventoryStatus.IN_PROGRESS);

			wellInventoryRepo.save(inventory);
		}

		// 🔹 STEP 2: Mark FULL WELL as COMPLETED (after all brands updated)
		List<WellInventory> inventories = wellInventoryRepo.findBySessionSessionIdAndWellWellId(sessionId, wellId);

		inventories.forEach(inv -> inv.setStatus(InventoryStatus.COMPLETED));

		// No need to call save again if within transactional context (JPA dirty
		// checking)
	}

	private WellInventory getInventory(Long sessionId, Long wellId, Long brandId) {
		return wellInventoryRepo.findBySessionSessionIdAndWellWellIdAndBrandBrandId(sessionId, wellId, brandId)
				.orElseThrow(() -> new RuntimeException(
						"Inventory not found for session=" + sessionId + ", well=" + wellId + ", brand=" + brandId));
	}

	public boolean isSessionCompleted(Long sessionId) {

		List<WellInventory> all = wellInventoryRepo.findBySessionSessionId(sessionId);

		if (all.isEmpty()) {
			return false; // ✅ FIX
		}

		return all.stream().allMatch(inv -> inv.getStatus() == InventoryStatus.COMPLETED);
	}

	 
	public Map<Long, InventoryStatus> getWellStatuses(Long sessionId) {

	    List<Well> allWells = wellRepo.findAll(); // 👈 IMPORTANT
	    List<WellInventory> inventories =
	            wellInventoryRepo.findBySessionSessionId(sessionId);

	    Map<Long, List<WellInventory>> grouped =
	            inventories.stream()
	                    .collect(Collectors.groupingBy(i -> i.getWell().getWellId()));

	    Map<Long, InventoryStatus> result = new HashMap<>();

	    for (Well well : allWells) {

	        List<WellInventory> wellInv = grouped.get(well.getWellId());

	        if (wellInv == null || wellInv.isEmpty()) {
	            result.put(well.getWellId(), InventoryStatus.IN_PROGRESS); // ✅ FIX
	        } else {
	            boolean completed = wellInv.stream()
	                    .allMatch(inv -> inv.getStatus() == InventoryStatus.COMPLETED);

	            result.put(well.getWellId(),
	                    completed ? InventoryStatus.COMPLETED : InventoryStatus.IN_PROGRESS);
	        }
	    }

	    return result;
	}

	public Long getNextPendingWell(Long sessionId) {

	    List<Well> allWells = wellRepo.findAll(); // ✅ include ALL wells

	    for (Well well : allWells) {

	        List<WellInventory> inventories =
	                wellInventoryRepo.findBySessionSessionIdAndWellWellId(sessionId, well.getWellId());

	        // ✅ NOT STARTED
	        if (inventories.isEmpty()) {
	            return well.getWellId();
	        }

	        // ✅ IN PROGRESS
	        boolean completed = inventories.stream()
	                .allMatch(inv -> inv.getStatus() == InventoryStatus.COMPLETED);

	        if (!completed) {
	            return well.getWellId();
	        }
	    }

	    return null; // ✅ all done
	}

	
	public boolean isWellCompleted(Long sessionId, Long wellId) {

	    List<WellInventory> inventories =
	            wellInventoryRepo.findBySessionSessionIdAndWellWellId(sessionId, wellId);

	    return !inventories.isEmpty() &&
	            inventories.stream()
	                    .allMatch(inv -> inv.getStatus() == InventoryStatus.COMPLETED);
	}
	
	public int getSessionProgress(Long sessionId) {

	    List<Well> allWells = wellRepo.findAll();

	    if (allWells.isEmpty()) {
	        return 0;
	    }

	    List<WellInventory> allInventories =
	            wellInventoryRepo.findBySessionSessionId(sessionId);

	    Map<Long, List<WellInventory>> grouped =
	            allInventories.stream()
	                    .collect(Collectors.groupingBy(i -> i.getWell().getWellId()));

	    int completedCount = 0;

	    for (Well well : allWells) {

	        List<WellInventory> inventories = grouped.get(well.getWellId());

	        boolean completed = inventories != null &&
	                inventories.stream()
	                        .allMatch(inv -> inv.getStatus() == InventoryStatus.COMPLETED);

	        if (completed) {
	            completedCount++;
	        }
	    }

	    return (completedCount * 100) / allWells.size();
	}
	
	
}