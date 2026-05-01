package com.barinventory.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.barinventory.entities.Brand;
import com.barinventory.entities.InventorySession;
import com.barinventory.entities.SessionStatus;
import com.barinventory.entities.StockroomInventory;
import com.barinventory.repos.BrandRepository;
import com.barinventory.repos.InventorySessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InventorySessionService {

    private final InventorySessionRepository sessionRepo;
    private final StockroomInventoryService stockroomService;
    private final BrandRepository brandRepo;

    public InventorySession createSession(String sessionName) {

        InventorySession current = new InventorySession();
        current.setSessionName(sessionName);
        current.setSessionDate(LocalDate.now());
        current.setStatus(SessionStatus.OPEN);

        current = sessionRepo.save(current);


        
        Optional<InventorySession> previousSessionOpt =
                sessionRepo.findTopByStatusOrderBySessionIdDesc(SessionStatus.CLOSED);

        /*
         FIRST SESSION CASE
        */
        if (previousSessionOpt.isEmpty()) {

            List<Brand> brands = brandRepo.findAll();

            for (Brand brand : brands) {
                StockroomInventory stock = new StockroomInventory();

                stock.setSession(current);
                stock.setBrand(brand);
                stock.setOpeningStock(0);
                stock.setReceivedStock(0);
                stock.setClosingStock(0);
                stock.setSaleStock(0);

                stockroomService.save(stock);
            }

            return current;
        }

        /*
         NORMAL FLOW
        */
        InventorySession previousSession = previousSessionOpt.get();

        stockroomService.initializeStockroom(
                current.getSessionId(),
                previousSession.getSessionId()
        );

        return current;
    }

    public void closeSession(Long sessionId) {
        InventorySession session =
                sessionRepo.findById(sessionId).orElseThrow();

        session.setStatus(SessionStatus.CLOSED);

        sessionRepo.save(session);
    }
}