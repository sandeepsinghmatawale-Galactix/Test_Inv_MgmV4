package com.barinventory.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.barinventory.dtos.DistributionRequest;
import com.barinventory.dtos.DistributionRequestWrapper;
import com.barinventory.entities.Brand;
import com.barinventory.entities.Distribution;
import com.barinventory.entities.Well;
import com.barinventory.services.*;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/distribution")
public class DistributionController {

    private final DistributionService distributionService;
    private final BrandService brandService;
    private final WellService wellService;
    private final StockroomInventoryService stockroomService;

    /*
     * Create Page
     */
    @GetMapping("/create-page/{sessionId}")
    public String createDistributionPage(@PathVariable Long sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        return "distribution/distribution-create";
    }

    /*
     * Create Distribution
     */
    @PostMapping("/create/{sessionId}")
    public String createDistribution(@PathVariable Long sessionId) {
        Distribution distribution = distributionService.createDistribution(sessionId);
        return "redirect:/distribution/allocation/" + distribution.getDistributionId();
    }

    /*
     * Allocation Page (PRE-FILLED CORRECTLY ✅)
     */
    @GetMapping("/allocation/{distributionId}")
    public String allocationPage(@PathVariable Long distributionId, Model model) {

        List<Brand> brands = brandService.getAllBrands();
        List<Well> wells = wellService.getAllWells();
        Map<Long, Integer> stockMap = stockroomService.getSaleStockMap(distributionId);

        List<DistributionRequest> requests = new ArrayList<>();

        for (Brand brand : brands) {
            for (Well well : wells) {

                DistributionRequest req = new DistributionRequest();
                req.setBrandId(brand.getBrandId());   // ✅ CRITICAL
                req.setWellId(well.getWellId());      // ✅ CRITICAL
                req.setDistributedQty(0);

                requests.add(req);
            }
        }

        DistributionRequestWrapper wrapper = new DistributionRequestWrapper();
        wrapper.setRequests(requests);

        model.addAttribute("brands", brands);
        model.addAttribute("wells", wells);
        model.addAttribute("stockMap", stockMap);
        model.addAttribute("distributionId", distributionId);
        model.addAttribute("wrapper", wrapper);

        return "distribution/distribution-allocation";
    }

    /*
     * SUBMIT DISTRIBUTION (🔥 DEBUG ENABLED)
     */
    @PostMapping("/allocate/{distributionId}")
    public String distribute(
            @PathVariable Long distributionId,
            @ModelAttribute DistributionRequestWrapper wrapper,
            Model model
    ) {

        // 🔍 DEBUG: Print incoming data
        System.out.println("======== FORM DATA ========");
        if (wrapper.getRequests() != null) {
            wrapper.getRequests().forEach(r -> {
                System.out.println(
                        "Brand=" + r.getBrandId() +
                        ", Well=" + r.getWellId() +
                        ", Qty=" + r.getDistributedQty()
                );
            });
        } else {
            System.out.println("❌ Wrapper is NULL or EMPTY");
        }

        try {
            System.out.println("➡️ Calling distributeStock...");

            distributionService.distributeStock(distributionId, wrapper.getRequests());

            System.out.println("✅ Distribution SUCCESS");

            Long sessionId =
                    distributionService.getSessionIdByDistribution(distributionId);

            System.out.println("➡️ Redirecting to /well/select/" + sessionId);

            return "redirect:/well/select/" + sessionId;

        } catch (RuntimeException ex) {

            // 🔥 CRITICAL DEBUG LINE (YOU WERE MISSING THIS)
            System.out.println("❌ ERROR: " + ex.getMessage());
            ex.printStackTrace();

            // Reload page data
            List<Brand> brands = brandService.getAllBrands();
            List<Well> wells = wellService.getAllWells();
            Map<Long, Integer> stockMap =
                    stockroomService.getSaleStockMap(distributionId);

            model.addAttribute("brands", brands);
            model.addAttribute("wells", wells);
            model.addAttribute("stockMap", stockMap);
            model.addAttribute("distributionId", distributionId);

            // 🔴 SHOW ERROR IN UI
            model.addAttribute("error", ex.getMessage());

            // 🔴 KEEP USER INPUT (VERY IMPORTANT)
            model.addAttribute("wrapper", wrapper);

            return "distribution/distribution-allocation";
        }
    }
}