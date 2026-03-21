package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Demo HTTP servlet registered via the OSGi HTTP Whiteboard pattern.
 * <p>
 * This servlet serves as a demonstration target for the weaving-based
 * servlet instrumentation. The weaving hook automatically instruments
 * {@link HttpServlet#service(HttpServletRequest, HttpServletResponse)}
 * to produce OpenTelemetry spans and HTTP metrics without any manual
 * instrumentation code in this class.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code GET /demo} — returns a JSON status response</li>
 *   <li>{@code GET /demo/slow} — simulates a slow request (500ms delay)</li>
 *   <li>{@code GET /demo/error} — simulates a server error (HTTP 500)</li>
 * </ul>
 */
@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        "osgi.http.whiteboard.servlet.pattern=/demo/*",
        "osgi.http.whiteboard.servlet.name=OTelDemoServlet"
    }
)
public class HttpDemoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(HttpDemoServlet.class.getName());

    private final AtomicLong requestCount = new AtomicLong();

    @Activate
    public void activate() {
        LOG.info("HttpDemoServlet activated — listening on /demo/*");
    }

    @Deactivate
    public void deactivate() {
        LOG.info("HttpDemoServlet deactivated (served " + requestCount.get() + " requests)");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long count = requestCount.incrementAndGet();
        String pathInfo = req.getPathInfo();

        if ("/slow".equals(pathInfo)) {
            handleSlow(resp, count);
        } else if ("/error".equals(pathInfo)) {
            handleError(resp, count);
        } else {
            handleStatus(resp, count);
        }
    }

    private void handleStatus(HttpServletResponse resp, long count) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter w = resp.getWriter()) {
            w.printf("""
                {
                  "service": "opentelemetry-osgi-demo",
                  "status": "ok",
                  "requestCount": %d,
                  "timestamp": "%s"
                }
                """, count, Instant.now());
        }
    }

    private void handleSlow(HttpServletResponse resp, long count) throws IOException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter w = resp.getWriter()) {
            w.printf("""
                {
                  "service": "opentelemetry-osgi-demo",
                  "status": "ok",
                  "type": "slow",
                  "delayMs": 500,
                  "requestCount": %d,
                  "timestamp": "%s"
                }
                """, count, Instant.now());
        }
    }

    private void handleError(HttpServletResponse resp, long count) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        try (PrintWriter w = resp.getWriter()) {
            w.printf("""
                {
                  "service": "opentelemetry-osgi-demo",
                  "status": "error",
                  "message": "Simulated error for telemetry demo",
                  "requestCount": %d,
                  "timestamp": "%s"
                }
                """, count, Instant.now());
        }
    }
}
