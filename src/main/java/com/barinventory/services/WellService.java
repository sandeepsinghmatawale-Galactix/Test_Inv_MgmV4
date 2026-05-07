package com.barinventory.services;

 

import java.util.List;

import org.springframework.stereotype.Service;

import com.barinventory.entities.Well;
import com.barinventory.repos.WellRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WellService {

    private final WellRepository wellRepository;

    public List<Well> getAllWells() {
        return wellRepository.findAll();
    }

    public Well getWellById(Long wellId) {
        return wellRepository.findById(wellId)
                .orElseThrow(() -> new RuntimeException("Well not found"));
    }
    
     
    public List<Well> getWellsByBar(Long barId) {
        List<Well> result = wellRepository.findByBar_BarId(barId);
        System.out.println("getWellsByBar(" + barId + ") = " + result.size());
        return result;
    }
     
}