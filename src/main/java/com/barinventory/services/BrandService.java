package com.barinventory.services;
 

import java.util.List;

import org.springframework.stereotype.Service;

import com.barinventory.entities.Brand;
import com.barinventory.repos.BrandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    public Brand getBrandById(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
    }
}