package com.barinventory.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stockroom_inventory")
@Getter
@Setter
public class StockroomInventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long stockroomId;

	@ManyToOne
	@JoinColumn(name = "session_id")
	private InventorySession session;

	@ManyToOne
	@JoinColumn(name = "brand_id")
	private Brand brand;

	private Integer openingStock;
	private Integer receivedStock;
	private Integer closingStock;
	private Integer saleStock;
}