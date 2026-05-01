package com.barinventory.dtos;

import lombok.Data;

@Data
public class StockroomClosingRequest {

    private Long brandId;
    private Integer closingStock;
}