package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Generates periodic HTTP traffic to the JAX-RS demo resource endpoints.
 * Exercises all HTTP methods (GET, POST, PUT, DELETE) at regular intervals
 * so the JAX-RS weaver instrumentation produces a steady stream of spans
 * and metrics visible in Grafana.
 */
@Component(immediate = true)
public class JaxRsTrafficGeneratorComponent {

    private static final Logger LOG = Logger.getLogger(JaxRsTrafficGeneratorComponent.class.getName());
    private static final String BASE_URL = "http://localhost:8181/api/rest";
    private static final int INTERVAL_SECONDS = 12;
    private static final int INITIAL_DELAY_SECONDS = 25;

    private ScheduledExecutorService scheduler;
    private HttpClient httpClient;
    private int cycle;

    @Activate
    public void activate() {
        LOG.info("JaxRsTrafficGeneratorComponent activated — generating traffic every "
                + INTERVAL_SECONDS + "s to " + BASE_URL);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jaxrs-traffic-generator");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::generateTraffic,
                INITIAL_DELAY_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        LOG.info("JaxRsTrafficGeneratorComponent deactivated");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void generateTraffic() {
        cycle++;

        // GET status
        sendRequest("GET", BASE_URL + "/status", null, "status");

        // POST create item
        sendRequest("POST", BASE_URL + "/items",
                "demo-item-" + cycle, "create");

        // GET list items
        sendRequest("GET", BASE_URL + "/items", null, "list");

        // GET specific item
        sendRequest("GET", BASE_URL + "/items/detail?id=" + cycle,
                null, "get-item");

        // PUT update item
        if (cycle % 2 == 0) {
            sendRequest("PUT", BASE_URL + "/items/detail?id=" + cycle,
                    "updated-" + cycle, "update");
        }

        // DELETE item every 4th cycle
        if (cycle % 4 == 0 && cycle > 1) {
            sendRequest("DELETE", BASE_URL + "/items/detail?id=" + (cycle - 1),
                    null, "delete");
        }
    }

    private void sendRequest(String method, String url, String body, String type) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10));

            switch (method) {
                case "POST" -> builder.POST(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                case "PUT" -> builder.PUT(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                case "DELETE" -> builder.DELETE();
                default -> builder.GET();
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            LOG.fine("[JAX-RS Traffic] " + method + " " + type + " → " + response.statusCode());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[JAX-RS Traffic] Failed " + method + " " + type
                    + ": " + e.getMessage());
        }
    }
}
