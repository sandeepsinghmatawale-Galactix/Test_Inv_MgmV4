package com.barinventory.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barinventory.entities.Well;

@Repository
public interface WellRepository extends JpaRepository<Well, Long> {
	
	List<Well> findAll();
	
	Optional<Well> findByWellName(String wellName);

}
