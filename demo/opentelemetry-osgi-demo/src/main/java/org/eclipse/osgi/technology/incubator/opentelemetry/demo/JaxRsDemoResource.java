package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

/**
 * Demo JAX-RS resource class annotated with standard JAX-RS annotations.
 * The JAX-RS weaver detects these annotations at class-load time and
 * instruments each HTTP method to produce OpenTelemetry spans and metrics.
 * This class is invoked by {@link JaxRsDispatcherServlet} which provides
 * a lightweight annotation-driven dispatch without requiring a full
 * JAX-RS runtime like CXF or Jersey.
 */
@Path("/api/rest")
public class JaxRsDemoResource {

    private final AtomicLong idSequence = new AtomicLong();
    private final Map<Long, String> items = new ConcurrentHashMap<>();

    @GET
    @Path("/status")
    public String getStatus() {
        return """
                {"service":"opentelemetry-osgi-demo","status":"ok","items":%d}
                """.formatted(items.size()).strip();
    }

    @GET
    @Path("/items")
    public String listItems() {
        List<String> entries = new ArrayList<>();
        items.forEach((id, name) ->
                entries.add("""
                        {"id":%d,"name":"%s"}""".formatted(id, name)));
        return "[" + String.join(",", entries) + "]";
    }

    @POST
    @Path("/items")
    public String createItem(String body) {
        long id = idSequence.incrementAndGet();
        String name = body != null && !body.isBlank() ? body.strip() : "item-" + id;
        items.put(id, name);
        return """
                {"id":%d,"name":"%s","created":true}
                """.formatted(id, name).strip();
    }

    @GET
    @Path("/items/detail")
    public String getItem(String idParam) {
        try {
            long id = Long.parseLong(idParam);
            String name = items.get(id);
            if (name != null) {
                return """
                        {"id":%d,"name":"%s"}
                        """.formatted(id, name).strip();
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        return """
                {"error":"not found"}
                """.strip();
    }

    @PUT
    @Path("/items/detail")
    public String updateItem(String idParam, String body) {
        try {
            long id = Long.parseLong(idParam);
            if (items.containsKey(id)) {
                String name = body != null && !body.isBlank() ? body.strip() : "updated-" + id;
                items.put(id, name);
                return """
                        {"id":%d,"name":"%s","updated":true}
                        """.formatted(id, name).strip();
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        return """
                {"error":"not found"}
                """.strip();
    }

    @DELETE
    @Path("/items/detail")
    public String deleteItem(String idParam) {
        try {
            long id = Long.parseLong(idParam);
            String removed = items.remove(id);
            if (removed != null) {
                return """
                        {"id":%d,"deleted":true}
                        """.formatted(id).strip();
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        return """
                {"error":"not found"}
                """.strip();
    }
}
