package com.fhnw.sa_case2_decisionworker.DTO;

public class ShippingResult {

    private DecisionMade.DecisionType decisionType; // Flag
    private DecisionMade.ShippingMethod shippingMethod; // Action
    private String carrier;
    private Long ruleId;

    public DecisionMade.DecisionType getDecisionType() {
        return decisionType;
    }
    public void setDecisionType(DecisionMade.DecisionType decisionType) {
        this.decisionType = decisionType;
    }

    public DecisionMade.ShippingMethod getShippingMethod() {
        return shippingMethod;
    }
    public void setShippingMethod(DecisionMade.ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
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