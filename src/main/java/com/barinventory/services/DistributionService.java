package com.barinventory.services;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;

import com.barinventory.dtos.DistributionRequest;
import com.barinventory.entities.Distribution;
import com.barinventory.entities.InventorySession;
import com.barinventory.entities.StockroomInventory;
import com.barinventory.entities.WellDistribution;
import com.barinventory.repos.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DistributionService {

	private final DistributionRepository distributionRepo;
	private final WellDistributionRepository wellDistributionRepo;
	private final StockroomInventoryRepository stockroomRepo;
	private final WellRepository wellRepo;
	private final BrandRepository brandRepo;
	private final InventorySessionRepository sessionRepo;

	/*
	 * CREATE DISTRIBUTION
	 */
	public Distribution createDistribution(Long sessionId) {
		InventorySession session = sessionRepo.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

		Distribution distribution = new Distribution();
		distribution.setSession(session);
		distribution.setDistributedAt(LocalDateTime.now());

		return distributionRepo.save(distribution);
	}

	/*
	 * MAIN METHOD (SCALABLE + SAFE)
	 */
	public void distributeStock(Long distributionId, List<DistributionRequest> requests) {

		// ✅ STEP 1: INPUT VALIDATION (NO DB)
		validateInput(requests);

		// ✅ STEP 2: LOAD REQUIRED DATA (MIN DB CALLS)
		Distribution distribution = distributionRepo.findById(distributionId)
				.orElseThrow(() -> new RuntimeException("Distribution not found"));

		Long sessionId = distribution.getSession().getSessionId();

		List<StockroomInventory> stocks = stockroomRepo.findDistributableStocks(sessionId);

		// ✅ STEP 3: PRE-VALIDATION (IN MEMORY)
		validateAgainstStock(requests, stocks);

		// ✅ STEP 4: PREPARE BATCH (NO DB HIT)
		List<WellDistribution> batchList = prepareBatch(requests, distribution);

		// ✅ STEP 5: WRITE TO DB (ATOMIC)
		wellDistributionRepo.deleteByDistributionId(distributionId);
		wellDistributionRepo.saveAll(batchList);

		// After saveAll, force flush before validation

		wellDistributionRepo.flush(); // ADD THIS
  System.out.println("wellDistributionRepo.flush is done");
		// ✅ STEP 6: FINAL DB VALIDATION (KEEP YOUR ORIGINAL LOGIC)
		validateDistribution(sessionId, distributionId);
System.out.println("validateDistribtuion done");
	}

	/*
	 * 🚫 BASIC INPUT VALIDATION
	 */
	private void validateInput(List<DistributionRequest> requests) {

		if (requests == null || requests.isEmpty()) {
			throw new RuntimeException("No distribution data submitted");
		}

		for (DistributionRequest r : requests) {

			if (r.getDistributedQty() == null || r.getDistributedQty() <= 0)
				continue;

			if (r.getBrandId() == null) {
				throw new RuntimeException("Brand ID missing");
			}

			if (r.getWellId() == null) {
				throw new RuntimeException("Well ID missing");
			}

			if (r.getDistributedQty() < 0) {
				throw new RuntimeException("Negative quantity not allowed");
			}
		}
	}

	/*
	 * 🧠 PRE-VALIDATION (NO DB WRITE)
	 */
	private void validateAgainstStock(List<DistributionRequest> requests, List<StockroomInventory> stocks) {

		Map<Long, Integer> totalMap = new HashMap<>();

		for (DistributionRequest r : requests) {

			if (r.getDistributedQty() == null || r.getDistributedQty() <= 0)
				continue;

			totalMap.merge(r.getBrandId(), r.getDistributedQty(), Integer::sum);
		}

		for (StockroomInventory stock : stocks) {

			Integer saleStock = stock.getSaleStock();

			if (saleStock == null || saleStock == 0)
				continue;

			Long brandId = stock.getBrand().getBrandId();

			int actual = totalMap.getOrDefault(brandId, 0);

			if (actual != saleStock) {
				throw new RuntimeException("❌ Distribution mismatch for brand: " + stock.getBrand().getBrandName()
						+ " | Expected: " + saleStock + " | Got: " + actual);
			}
		}
	}

	/*
	 * ⚡ PREPARE BATCH (SCALABLE)
	 */
	private List<WellDistribution> prepareBatch(List<DistributionRequest> requests, Distribution distribution) {

		List<WellDistribution> list = new ArrayList<>();

		for (DistributionRequest r : requests) {

			if (r.getDistributedQty() == null || r.getDistributedQty() <= 0)
				continue;
			if (r.getBrandId() == null || r.getWellId() == null)
				continue;

			WellDistribution wd = new WellDistribution();

			wd.setDistribution(distribution);

			// 🚀 No DB hit (lazy reference)
			wd.setBrand(brandRepo.getReferenceById(r.getBrandId()));
			wd.setWell(wellRepo.getReferenceById(r.getWellId()));

			wd.setDistributedQty(r.getDistributedQty());
			wd.setDistributedAt(LocalDateTime.now());

			list.add(wd);
		}

		return list;
	}

	/*
	 * 🔥 FINAL VALIDATION (DB CHECK - YOUR ORIGINAL LOGIC KEPT)
	 */
	private void validateDistribution(Long sessionId, Long distributionId) {

		List<StockroomInventory> stocks = stockroomRepo.findDistributableStocks(sessionId);

		for (StockroomInventory stock : stocks) {

			if (stock.getSaleStock() == null || stock.getSaleStock() == 0)
				continue;

			Integer distributedQty = wellDistributionRepo.getTotalDistributedQty(distributionId,
					stock.getBrand().getBrandId());

			if (distributedQty == null)
				distributedQty = 0;

			if (!distributedQty.equals(stock.getSaleStock())) {
				throw new RuntimeException("❌ Final validation failed for brand: " + stock.getBrand().getBrandName());
			}
		}
	}

	/*
	 * GET SESSION ID
	 */
	public Long getSessionIdByDistribution(Long distributionId) {
		return distributionRepo.findById(distributionId).map(d -> d.getSession().getSessionId())
				.orElseThrow(() -> new RuntimeException("Distribution not found"));
	}
}