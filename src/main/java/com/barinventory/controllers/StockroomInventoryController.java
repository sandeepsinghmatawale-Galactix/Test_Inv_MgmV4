package com.barinventory.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.dtos.StockroomClosingRequest;
import com.barinventory.entities.StockroomInventory;
import com.barinventory.services.StockroomInventoryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/stockroom")
public class StockroomInventoryController {

    private final StockroomInventoryService stockroomService;

    @GetMapping("/{sessionId}")
    public String stockroomPage(
            @PathVariable Long sessionId,
            Model model
    ) {

        List<StockroomInventory> stocks =
                stockroomService.getStockroomBySession(sessionId);

        model.addAttribute("stocks", stocks);
        model.addAttribute("sessionId", sessionId);

        return "stockroom/stockroom-inventory";
    }

    @PostMapping("/closing/{sessionId}")
    public String updateClosing(
            @PathVariable Long sessionId,
            @RequestParam List<Long> brandId,
            @RequestParam List<Integer> closingStock
    ) {

        List<StockroomClosingRequest> requests = new ArrayList<>();

        for(int i=0; i<brandId.size(); i++){

            StockroomClosingRequest req =
                    new StockroomClosingRequest();

            req.setBrandId(brandId.get(i));
            req.setClosingStock(closingStock.get(i));

            requests.add(req);
        }

        stockroomService.updateClosingStock(
                sessionId,
                requests
        );

        return "redirect:/distribution/create-page/" + sessionId;
    }
}