package com.barinventory.dtos;

import java.util.List;

import lombok.Data;

@Data
public class StockroomClosingWrapper {

	private List<StockroomClosingRequest> requests;
 	 

    public List<StockroomClosingRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<StockroomClosingRequest> requests) {
        this.requests = requests;
    }
}