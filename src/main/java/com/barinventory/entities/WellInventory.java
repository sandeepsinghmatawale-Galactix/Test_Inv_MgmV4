package com.barinventory.entities;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="well_inventory")
@Getter
@Setter
public class WellInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wellInventoryId;

    @ManyToOne
    @JoinColumn(name="session_id")
    private InventorySession session;

    @ManyToOne
    @JoinColumn(name="well_id")
    private Well well;

    @ManyToOne
    @JoinColumn(name="brand_id")
    private Brand brand;

    private Integer openingStock;
    private Integer receivedStock;
    private Integer closingStock;
    private Integer saleStock;

    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private InventoryStatus status;
    
    
}