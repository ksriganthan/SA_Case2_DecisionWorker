package com.fhnw.sa_case2_decisionworker.Worker;


import com.fhnw.sa_case2_decisionworker.RestClient.DecisionApiClient;
import com.fhnw.sa_case2_decisionworker.Service.DecisionService;
import org.camunda.bpm.client.ExternalTaskClient;

public class DecisionWorker {

    public static void main(String[] args) {

        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl("http://group6:p5TuHbjEadLeT6L@192.168.111.3:8080/engine-rest")
                .asyncResponseTimeout(1000)
                .build();

        DecisionApiClient apiClient =
                new DecisionApiClient("http://localhost:8081/decision/make");
        DecisionService shippingService = new DecisionService(apiClient);

        client.subscribe("shippingDecision")
                .lockDuration(1000)
                .handler(new DecisionExternalTaskHandler(shippingService))
                .open();
    }
}