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
 * Generates periodic HTTP traffic to the demo servlet endpoints.
 * <p>
 * This component sends requests to the {@link HttpDemoServlet} at regular
 * intervals, exercising all three endpoints ({@code /demo}, {@code /demo/slow},
 * {@code /demo/error}) so the weaving-based servlet instrumentation produces
 * a steady stream of spans and HTTP metrics visible in Grafana.
 * <p>
 * The caller starts 15 seconds after activation to allow the HTTP service
 * and servlet registration to complete first.
 */
@Component(immediate = true)
public class HttpTrafficGeneratorComponent {

    private static final Logger LOG = Logger.getLogger(HttpTrafficGeneratorComponent.class.getName());
    private static final String BASE_URL = "http://localhost:8181/demo";
    private static final int INTERVAL_SECONDS = 10;
    private static final int INITIAL_DELAY_SECONDS = 15;

    private ScheduledExecutorService scheduler;
    private HttpClient httpClient;
    private int cycle;

    @Activate
    public void activate() {
        LOG.info("HttpTrafficGeneratorComponent activated — will generate traffic every "
                + INTERVAL_SECONDS + "s to " + BASE_URL);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "http-traffic-generator");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::generateTraffic,
                INITIAL_DELAY_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        LOG.info("HttpTrafficGeneratorComponent deactivated");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void generateTraffic() {
        cycle++;
        sendRequest(BASE_URL, "status");
        sendRequest(BASE_URL + "/slow", "slow");
        if (cycle % 3 == 0) {
            sendRequest(BASE_URL + "/error", "error");
        }
    }

    private void sendRequest(String url, String type) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            LOG.fine("[HTTP Traffic] " + type + " → " + response.statusCode());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[HTTP Traffic] Failed to reach " + type + ": " + e.getMessage());
        }
    }
}
