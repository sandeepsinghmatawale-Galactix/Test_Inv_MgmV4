package com.barinventory.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.dtos.WellClosingRequest;
import com.barinventory.entities.InventoryStatus;
import com.barinventory.entities.Well;
import com.barinventory.entities.WellInventory;
import com.barinventory.repos.WellInventoryRepository;
import com.barinventory.repos.WellRepository;
import com.barinventory.services.WellInventoryService;
import com.barinventory.services.WellService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/well")
public class WellInventoryController {

	private final WellInventoryService wellInventoryService;
	private final WellService wellService;
	private final WellInventoryRepository wellInventoryRepo;
	private final WellRepository wellRepo;

	/*
	 * SELECT WELL PAGE
	 */

	@GetMapping("/select/{sessionId}")
	public String selectWellPage(@PathVariable Long sessionId, Model model) {

		List<Well> wells = wellService.getAllWells();

		model.addAttribute("wells", wells);
		model.addAttribute("sessionId", sessionId);

		// ✅ ADD HERE (this is the correct place)
		Map<Long, InventoryStatus> wellStatusMap = wellInventoryService.getWellStatuses(sessionId);

		model.addAttribute("wellStatusMap", wellStatusMap);

		// existing logic
		boolean completed = wellInventoryService.isSessionCompleted(sessionId);
		model.addAttribute("sessionCompleted", completed);
		
 
	    
		int progress = wellInventoryService.getSessionProgress(sessionId);
		model.addAttribute("progress", progress);

		return "well/well-selection";
	}

	/*
	 * INITIALIZE WELL INVENTORY
	 */
	@PostMapping("/initialize/{sessionId}/{wellId}")
	public String initializeWell(@PathVariable Long sessionId, @PathVariable Long wellId) {

		// 🔒 SESSION LOCK
		if (wellInventoryService.isSessionCompleted(sessionId)) {
			return "redirect:/well/select/" + sessionId;
		}

		// 🔒 WELL LOCK
		if (wellInventoryService.isWellCompleted(sessionId, wellId)) {
			return "redirect:/well/select/" + sessionId;
		}

		wellInventoryService.initializeWellInventory(sessionId, wellId);

		return "redirect:/well/" + sessionId + "/" + wellId;
	}

	/*
	 * OPEN WELL INVENTORY PAGE
	 */

	@GetMapping("/{sessionId}/{wellId}")
	public String wellInventoryPage(@PathVariable Long sessionId, @PathVariable Long wellId, Model model) {

		// 🔒 SESSION LOCK
		if (wellInventoryService.isSessionCompleted(sessionId)) {
			return "redirect:/well/select/" + sessionId;
		}

		// 🔒 WELL LOCK (single source of truth)
		if (wellInventoryService.isWellCompleted(sessionId, wellId)) {
			return "redirect:/well/select/" + sessionId;
		}

		// ✅ Fetch inventory
		List<WellInventory> inventory = wellInventoryService.getWellInventory(sessionId, wellId);

		model.addAttribute("wellInventory", inventory);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("wellId", wellId);

		return "well/well-inventory";
	}
	/*
	 * SAVE WELL CLOSING
	 */

	@PostMapping("/closing/{sessionId}/{wellId}")
	public String updateClosing(@PathVariable Long sessionId, @PathVariable Long wellId,
			@RequestParam List<Long> brandId, @RequestParam List<Integer> closingStock) {

		List<WellClosingRequest> requests = new ArrayList<>();

		for (int i = 0; i < brandId.size(); i++) {
			WellClosingRequest req = new WellClosingRequest();
			req.setBrandId(brandId.get(i));
			req.setClosingStock(closingStock.get(i));
			requests.add(req);
		}

		// ✅ SAVE CURRENT WELL
		wellInventoryService.updateWellClosing(sessionId, wellId, requests);

		// ✅ GET NEXT WELL
		Long nextWellId = wellInventoryService.getNextPendingWell(sessionId);

		if (nextWellId != null) {

			// ⭐⭐⭐ THIS IS THE MISSING PIECE ⭐⭐⭐
			wellInventoryService.initializeWellInventory(sessionId, nextWellId);

			return "redirect:/well/" + sessionId + "/" + nextWellId;
		}

		// ✅ ALL DONE → BACK TO SELECT PAGE
		return "redirect:/well/select/" + sessionId;
	}
	
	

}