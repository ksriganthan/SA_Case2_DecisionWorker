package com.fhnw.sa_case2_decisionworker.DTO;


// Geht an die Spedition via REST-SERVICE
public class ShippingDecisionArgs {

	private DecisionMade.DestinationCountry destinationCountry;
	private Long weight;

	public DecisionMade.DestinationCountry getDestinationCountry() {
		return destinationCountry;
	}
	public void setDestinationCountry(DecisionMade.DestinationCountry destinationCountry) {
		this.destinationCountry = destinationCountry;
	}

	public Long getWeight() {
		return weight;
	}

	public void setWeight(Long weight) {
		this.weight = weight;
	}

}

