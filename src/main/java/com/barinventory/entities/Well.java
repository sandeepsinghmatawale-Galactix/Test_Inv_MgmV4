package com.barinventory.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="wells")
@Getter
@Setter
public class Well {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wellId;

    private String wellName;
}