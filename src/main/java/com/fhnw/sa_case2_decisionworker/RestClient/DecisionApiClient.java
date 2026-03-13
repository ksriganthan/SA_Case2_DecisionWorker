package com.fhnw.sa_case2_decisionworker.RestClient;



import com.fhnw.sa_case2_decisionworker.DTO.DecisionMade;
import com.fhnw.sa_case2_decisionworker.DTO.ShippingDecisionArgs;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

public class DecisionApiClient {

    private final String serviceUrl;

    public DecisionApiClient(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public DecisionMade requestConsignment(ShippingDecisionArgs request) {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(serviceUrl);

            return target.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(request, MediaType.APPLICATION_JSON), DecisionMade.class);

        } catch (WebApplicationException e) {
            // HTTP Fehler (z.B. 501 = fachliche Ablehnung)
            throw e;
        } catch (ProcessingException e) {
            // Timeout / Connection / DNS / etc.
            throw e;
        } finally {
            client.close();
        }
    }
}