package com.fhnw.sa_case2_decisionworker.DTO;

public class ShippingResult {

    private DecisionMade.DecisionType decisionType; // Flag
    private DecisionMade.ShippingType shippingType; // Action
    private String carrier;
    private Long ruleId;

    public DecisionMade.DecisionType getDecisionType() {
        return decisionType;
    }
    public void setDecisionType(DecisionMade.DecisionType decisionType) {
        this.decisionType = decisionType;
    }

    public DecisionMade.ShippingType getShippingType() {
        return shippingType;
    }
    public void setShippingMethod(DecisionMade.ShippingType shippingType) {
        this.shippingType = shippingType;
    }

    public String getCarrier() {
        return carrier;
    }
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
    public Long getRuleId() {
        return ruleId;
    }
    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }
}