package com.barinventory.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DistributionRequestWrapper {

	private List<DistributionRequest> requests= new ArrayList<>();
}