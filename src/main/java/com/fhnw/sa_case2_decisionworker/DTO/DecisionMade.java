package com.fhnw.sa_case2_decisionworker.DTO;

public class DecisionMade {
    public enum DecisionType {
        AUTOMATIC,
        MANUAL
    }

    public enum ShippingMethod {
        SPECIAL, NORMAL, AIR
    }

    public enum DestinationCountry {
        ARG, JAP, DE, CH, RUS
    }

    private DecisionType decisionType; // Flag
    private ShippingMethod shippingMethod; // Action
    private String carrier;
    private Long ruleId;

    public DecisionType getDecisionType() {
        return decisionType;
    }
    public void setDecisionType(DecisionType decisionType) {
        this.decisionType = decisionType;
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }
    public void setShippingMethod(ShippingMethod shippingMethod) {
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

