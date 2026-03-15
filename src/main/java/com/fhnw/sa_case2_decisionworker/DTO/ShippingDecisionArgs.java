package com.fhnw.sa_case2_decisionworker.DTO;


// Geht an die Spedition via REST-SERVICE
public class ShippingDecisionArgs {

	private DecisionMade.DestinationCountry destinationCountry;
	private Integer weight;

	public DecisionMade.DestinationCountry getDestinationCountry() {
		return destinationCountry;
	}
	public void setDestinationCountry(DecisionMade.DestinationCountry destinationCountry) {
		this.destinationCountry = destinationCountry;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

}

