package com.barinventory.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.barinventory.entities.Bar;
import com.barinventory.entities.Brand;
import com.barinventory.entities.InventorySession;
import com.barinventory.entities.SessionStatus;
import com.barinventory.entities.StockroomInventory;
import com.barinventory.repos.BarRepository;
import com.barinventory.repos.BrandRepository;
import com.barinventory.repos.InventorySessionRepository;
import com.barinventory.repos.StockroomInventoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InventorySessionService {

	private final InventorySessionRepository sessionRepo;
	private final StockroomInventoryService stockroomService;
	private final BrandRepository brandRepo;
	private final BarRepository barRepo;
	private final StockroomInventoryRepository stockroomRepo;

	/*
	 * ----------------------------------------- CREATE SESSION (MULTI-BAR SAFE)
	 * -----------------------------------------
	 */
	public InventorySession createSession(Long barId, String sessionName) {

		// ✅ 1. Validate Bar
		Bar bar = barRepo.findById(barId).orElseThrow(() -> new RuntimeException("Bar not found"));

		// ✅ 2. Enforce ONE OPEN session per bar (optimized)
		if (sessionRepo.existsByBarBarIdAndStatus(barId, SessionStatus.OPEN)) {
			throw new RuntimeException("An OPEN session already exists for this bar");
		}

		// ✅ 3. Create new session
		InventorySession current = new InventorySession();
		current.setSessionName(sessionName);
		current.setSessionDate(LocalDateTime.now()); // ✅ correct type
		current.setStatus(SessionStatus.OPEN);
		current.setBar(bar);

		current = sessionRepo.save(current);

		// ✅ 4. Fetch previous CLOSED session (same bar)
		Optional<InventorySession> previousSessionOpt = sessionRepo
				.findTopByBarBarIdAndStatusOrderBySessionIdDesc(barId, SessionStatus.CLOSED);

		/*
		 * ----------------------------------------- FIRST SESSION CASE
		 * -----------------------------------------
		 */
		if (previousSessionOpt.isEmpty()) {

		    List<Brand> brands = brandRepo.findByBarBarId(barId);

		    InventorySession savedSession = sessionRepo.save(current);

		    List<StockroomInventory> stocks = brands.stream().map(brand -> {

		        StockroomInventory stock = new StockroomInventory();

		        stock.setSession(savedSession);
		        stock.setBrand(brand);
		        stock.setBar(bar);

		        stock.setOpeningStock(0);
		        stock.setReceivedStock(0);
		        stock.setClosingStock(0);
		        stock.setSaleStock(0);

		        return stock;

		    }).toList();

		    stockroomRepo.saveAll(stocks);

		    return current;
		}

		/*
		 * ----------------------------------------- NORMAL FLOW (Carry Forward)
		 * -----------------------------------------
		 */
		InventorySession previousSession = previousSessionOpt.get();

		stockroomService.initializeStockroom(current.getSessionId(), previousSession.getSessionId());

		return current;
	}

	/*
	 * ----------------------------------------- CLOSE SESSION
	 * -----------------------------------------
	 */
	public void closeSession(Long sessionId) {

		InventorySession session = sessionRepo.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Session not found"));

		// ✅ Prevent duplicate closing
		if (session.getStatus() == SessionStatus.CLOSED) {
			throw new RuntimeException("Session already closed");
		}

		// ✅ Optional: enforce completion before closing
		// if (!wellInventoryService.isSessionCompleted(sessionId)) {
		// throw new RuntimeException("Cannot close session. Pending wells exist.");
		// }

		session.setStatus(SessionStatus.CLOSED);
		// ✅ No need to call save() → JPA will auto flush
	}
}