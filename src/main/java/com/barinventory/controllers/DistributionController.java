package com.barinventory.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.barinventory.config.SecurityUtils;
import com.barinventory.dtos.DistributionRequest;
import com.barinventory.dtos.DistributionRequestWrapper;
import com.barinventory.entities.Brand;
import com.barinventory.entities.Distribution;
import com.barinventory.entities.StockroomInventory;
import com.barinventory.entities.Well;
import com.barinventory.services.BrandService;
import com.barinventory.services.DistributionService;
import com.barinventory.services.StockroomInventoryService;
import com.barinventory.services.WellService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/distribution")
public class DistributionController {

	private final DistributionService distributionService;
	private final BrandService brandService;
	private final WellService wellService;
	private final StockroomInventoryService stockroomService;

	// GET /distribution/create-page/{sessionId}
	@GetMapping("/create-page/{sessionId}")
	public String createDistributionPage(@PathVariable Long sessionId, Model model) {
		model.addAttribute("sessionId", sessionId);
		return "distribution/distribution-create";
	}

	// POST /distribution/create/{sessionId}
	@PostMapping("/create/{sessionId}")
	public String createDistribution(@PathVariable Long sessionId) {
		Long barId = SecurityUtils.getBarId();
		Distribution distribution = distributionService.createDistribution(barId, sessionId);
		return "redirect:/distribution/allocation/" + distribution.getDistributionId();
	}

	// GET /distribution/allocation/{distributionId}
	@GetMapping("/allocation/{distributionId}")
	public String allocationPage(@PathVariable Long distributionId, Model model) {
		Long barId = SecurityUtils.getBarId();
		System.out.println("barId from security: " + barId);
		List<Brand> brands = brandService.getBrandsByBar(barId);
		List<Well> wells = wellService.getWellsByBar(barId);
		Map<Long, Integer> stockMap = stockroomService.getSaleStockMap(distributionId);		 
		 

		// Fallback if bar_id not set on brands/wells in DB
	 
		if (wells.isEmpty()) wells = wellService.getAllWells();
		
		System.out.println("Brands count: " + brands.size());
		System.out.println("StockMap: " + stockMap);
		System.out.println("Wells count: " + wells.size());
		List<DistributionRequest> requests = new ArrayList<>();
		for (Brand brand : brands) {
			for (Well well : wells) {
				DistributionRequest req = new DistributionRequest();
				req.setBrandId(brand.getBrandId());
				req.setWellId(well.getWellId());
				req.setDistributedQty(0);
				requests.add(req);
			}
		}

		DistributionRequestWrapper wrapper = new DistributionRequestWrapper();

		Long sessionId = distributionService.getSessionIdByDistribution(distributionId);
		wrapper.setRequests(requests);
		List<StockroomInventory> stocks = stockroomService.getStockroomBySession(sessionId);

		model.addAttribute("stocks", stocks);
		model.addAttribute("brands", brands);
		model.addAttribute("wells", wells);
		model.addAttribute("stockMap", stockMap);
		model.addAttribute("distributionId", distributionId);
		model.addAttribute("wrapper", wrapper);

		return "distribution/distribution-allocation";
	}

	// POST /distribution/allocate/{distributionId}
	@PostMapping("/allocate/{distributionId}")
	public String distribute(@PathVariable Long distributionId, @ModelAttribute DistributionRequestWrapper wrapper,
			Model model) {
		Long barId = SecurityUtils.getBarId();

		try {
			distributionService.distributeStock(distributionId, wrapper.getRequests());

			Long sessionId = distributionService.getSessionIdByDistribution(distributionId);

			return "redirect:/well/select/" + sessionId;

		} catch (RuntimeException ex) {
			ex.printStackTrace();

			List<Brand> brands = brandService.getBrandsByBar(barId);
			List<Well> wells = wellService.getWellsByBar(barId);
			Map<Long, Integer> stockMap = stockroomService.getSaleStockMap(distributionId);
			System.out.println("Brands count: " + brands.size());
			System.out.println("StockMap: " + stockMap);
			System.out.println("Wells count: " + wells.size());
			model.addAttribute("brands", brands);
			model.addAttribute("wells", wells);
			model.addAttribute("stockMap", stockMap);
			model.addAttribute("distributionId", distributionId);
			model.addAttribute("error", ex.getMessage());
			model.addAttribute("wrapper", wrapper);

			return "distribution/distribution-allocation";
		}
	}
}