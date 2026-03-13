package com.fhnw.sa_case2_decisionworker.Service;


import com.fhnw.sa_case2_decisionworker.DTO.DecisionMade;
import com.fhnw.sa_case2_decisionworker.DTO.ShippingDecisionArgs;
import com.fhnw.sa_case2_decisionworker.DTO.ShippingResult;
import com.fhnw.sa_case2_decisionworker.RestClient.DecisionApiClient;

public class DecisionService {

    private final DecisionApiClient apiClient;


    public DecisionService(DecisionApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public ShippingResult sendShippingOrder(DecisionMade.DestinationCountry country, Long weight) {

        // minimale fachliche Validierung
        if (country == null || country.name().isEmpty()) {
            throw new IllegalArgumentException("country is missing");
        }
        if (weight == null || weight <= 0) {
            throw new IllegalArgumentException("weight must be > 0");
        }

        // Mapping -> Request DTO für Spedition
        ShippingDecisionArgs req = new ShippingDecisionArgs();
        req.setCountry(country);
        req.setWeight(Math.toIntExact(weight)); // BPMN: long -> API: Integer

        DecisionMade response = apiClient.requestConsignment(req);

        System.out.println("[SPEDITION] Response erhalten:");
        System.out.println("  decisionType      : " + response.getDecisionType());
        System.out.println("  shippingMethod   : " + response.getShippingMethod());
        System.out.println("  carrier : " + response.getCarrier());
        System.out.println("  ruldId : " + response.getRuleId());

        // Mapping -> fachliches Resultat
        ShippingResult result = new ShippingResult();
        result.setDecisionType(response.getDecisionType());
        result.setShippingMethod(response.getShippingMethod());
        result.setCarrier(response.getCarrier());
        result.setRuleId(response.getRuleId());

        return result;
    }
}