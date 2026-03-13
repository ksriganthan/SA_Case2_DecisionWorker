package com.fhnw.sa_case2_decisionworker.DTO;


// Geht an die Spedition via REST-SERVICE
public class ShippingDecisionArgs {

	private DecisionMade.DestinationCountry country;
	private Integer weight;

	public DecisionMade.DestinationCountry getCountry() {
		return country;
	}
	public void setCountry(DecisionMade.DestinationCountry country) {}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

}

