package com.barinventory.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "brands")
@Data
public class Brand {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long brandId;

	private String brandName;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bar_id")
    private Bar bar;

}