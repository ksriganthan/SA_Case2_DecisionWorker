package com.fhnw.sa_case2_decisionworker.Worker;

import com.fhnw.sa_case2_decisionworker.DTO.DecisionMade;
import com.fhnw.sa_case2_decisionworker.DTO.ShippingResult;
import com.fhnw.sa_case2_decisionworker.Service.DecisionService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.util.HashMap;
import java.util.Map;

public class DecisionExternalTaskHandler implements ExternalTaskHandler {

    private final DecisionService decisionService;

    public DecisionExternalTaskHandler(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {

        // Prozessvariablen aus BPMN (passen zu eurem Modell)
        Long weight = externalTask.getVariable("weight");
        DecisionMade.DestinationCountry country = DecisionMade.DestinationCountry.valueOf(externalTask.getVariable("country"));

        System.out.println("weight          : " + weight);
        System.out.println("country       : " + country);
        try {

            ShippingResult result = decisionService.sendDecisionOrder(
                    country,
                    weight
            );

            Map<String, Object> vars = new HashMap<>();
             // Als plain String speichern, damit Camunda ${decisionType == 'MANUAL'} auswerten kann
            // Bei MANUAL-Entscheidungen können shippingMethod/carrier/ruleId null sein
            vars.put("decisionType",   result.getDecisionType()   != null ? result.getDecisionType().name()   : null);
            vars.put("shippingType", result.getShippingType() != null ? result.getShippingType().name() : null);
            vars.put("carrier",  result.getCarrier());
            vars.put("ruleId",   result.getRuleId());


            externalTaskService.complete(externalTask, vars);

        } catch (IllegalArgumentException e) {
            // Fachlicher Fehler in Inputs -> kein Retry sinnvoll
            externalTaskService.handleFailure(
                    externalTask,
                    "Invalid process variables",
                    e.getMessage(),
                    0,
                    0L
            );

        } catch (WebApplicationException | ProcessingException e) {
            // Technischer Fehler -> Retry Strategie
            Integer retries = externalTask.getRetries();
            int remainingRetries = (retries == null) ? 3 : retries - 1;

            System.out.println("Technical error, remaining retries: " + remainingRetries);

            externalTaskService.handleFailure(
                    externalTask,
                    "REST not reachable / technical error",
                    e.getMessage(),
                    remainingRetries,
                    60_000L
            );
        }
    }
}