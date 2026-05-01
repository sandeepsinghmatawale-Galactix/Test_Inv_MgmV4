package com.barinventory.dtos;

import lombok.Data;

@Data
public class DistributionRequest {

    private Long wellId;
    private Long brandId;
    private Integer distributedQty;
}