package com.barinventory.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barinventory.entities.InventorySession;
import com.barinventory.entities.SessionStatus;
import com.barinventory.entities.StockroomInventory;

public interface InventorySessionRepository 
extends JpaRepository<InventorySession,Long> {

	Optional<InventorySession> findTopByStatusOrderBySessionIdDesc(
            SessionStatus status
    );
}