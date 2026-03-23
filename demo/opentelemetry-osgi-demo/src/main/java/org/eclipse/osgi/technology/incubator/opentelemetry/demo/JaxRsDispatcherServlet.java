package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Lightweight JAX-RS dispatcher servlet that reads {@code @Path} and
 * HTTP method annotations via reflection and routes incoming requests
 * to matching resource methods.
 * This avoids the need for a full JAX-RS runtime (CXF, Jersey) while
 * still exercising the JAX-RS weaver instrumentation on real annotated
 * resource classes.
 */
@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        "osgi.http.whiteboard.servlet.pattern=/api/*",
        "osgi.http.whiteboard.servlet.name=JaxRsDispatcher"
    }
)
public class JaxRsDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(JaxRsDispatcherServlet.class.getName());

    private final List<RouteEntry> routes = new ArrayList<>();
    private JaxRsDemoResource resource;

    @Activate
    public void activate() {
        resource = new JaxRsDemoResource();
        scanRoutes(resource);
        LOG.info("JaxRsDispatcherServlet activated with " + routes.size()
                + " routes on /api/*");
    }

    @Deactivate
    public void deactivate() {
        routes.clear();
        LOG.info("JaxRsDispatcherServlet deactivated");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String method = req.getMethod();
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }

        // Match against registered routes
        String classPath = getClassPath(resource.getClass());
        // Strip the class path prefix from the request path for matching
        // The servlet is mapped to /api/*, so pathInfo is everything after /api
        // The resource has @Path("/api/rest"), so we need to match /rest/...
        String matchPath = pathInfo;
        if (classPath.startsWith("/api")) {
            String suffix = classPath.substring(4); // strip /api prefix
            if (!matchPath.startsWith(suffix)) {
                sendNotFound(resp);
                return;
            }
            matchPath = matchPath.substring(suffix.length());
        }
        if (matchPath.isEmpty()) {
            matchPath = "/";
        }

        for (RouteEntry route : routes) {
            if (route.httpMethod.equals(method) && pathMatches(route.path, matchPath)) {
                invokeRoute(route, req, resp, matchPath);
                return;
            }
        }

        sendNotFound(resp);
    }

    private void invokeRoute(RouteEntry route, HttpServletRequest req,
            HttpServletResponse resp, String matchPath) throws IOException {
        try {
            Object result;
            Class<?>[] paramTypes = route.method.getParameterTypes();

            if (paramTypes.length == 0) {
                result = route.method.invoke(resource);
            } else if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                String param = extractParam(req, matchPath);
                result = route.method.invoke(resource, param);
            } else if (paramTypes.length == 2 && paramTypes[0] == String.class
                    && paramTypes[1] == String.class) {
                String param = extractParam(req, matchPath);
                String body = readBody(req);
                result = route.method.invoke(resource, param, body);
            } else {
                String body = readBody(req);
                result = route.method.invoke(resource, body);
            }

            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter w = resp.getWriter()) {
                w.print(result != null ? result.toString() : "{}");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Route invocation failed: " + e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter w = resp.getWriter()) {
                w.print("{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    private String extractParam(HttpServletRequest req, String matchPath) {
        String param = req.getParameter("id");
        if (param == null) {
            // Try extracting from path: /items/detail?id=X or last segment
            String[] parts = matchPath.split("/");
            if (parts.length > 0) {
                param = parts[parts.length - 1];
            }
        }
        return param;
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private boolean pathMatches(String routePath, String requestPath) {
        // Normalize both paths
        String normalizedRoute = routePath.isEmpty() ? "/" : routePath;
        String normalizedRequest = requestPath.isEmpty() ? "/" : requestPath;

        // Strip trailing slashes for comparison
        if (normalizedRoute.length() > 1 && normalizedRoute.endsWith("/")) {
            normalizedRoute = normalizedRoute.substring(0, normalizedRoute.length() - 1);
        }
        if (normalizedRequest.length() > 1 && normalizedRequest.endsWith("/")) {
            normalizedRequest = normalizedRequest.substring(0, normalizedRequest.length() - 1);
        }

        return normalizedRoute.equals(normalizedRequest);
    }

    private void sendNotFound(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        try (PrintWriter w = resp.getWriter()) {
            w.print("{\"error\":\"not found\",\"routes\":" + routes.size() + "}");
        }
    }

    private void scanRoutes(Object target) {
        Class<?> clazz = target.getClass();
        String classPath = "";
        Path classPathAnn = clazz.getAnnotation(Path.class);
        if (classPathAnn != null) {
            classPath = classPathAnn.value();
        }

        for (Method m : clazz.getDeclaredMethods()) {
            String httpMethod = getHttpMethod(m);
            if (httpMethod == null) {
                continue;
            }
            String methodPath = "";
            Path methodPathAnn = m.getAnnotation(Path.class);
            if (methodPathAnn != null) {
                methodPath = methodPathAnn.value();
            }

            String fullPath = classPath;
            if (!methodPath.isEmpty()) {
                if (!fullPath.endsWith("/") && !methodPath.startsWith("/")) {
                    fullPath += "/";
                }
                fullPath += methodPath;
            }

            // Strip the /api prefix since the servlet is mapped to /api/*
            String routePath = fullPath;
            if (routePath.startsWith("/api")) {
                routePath = routePath.substring(4);
                if (routePath.startsWith("/rest")) {
                    routePath = routePath.substring(5);
                }
            }

            m.setAccessible(true);
            routes.add(new RouteEntry(httpMethod, routePath, m));
            LOG.info("  Route: " + httpMethod + " " + fullPath + " → " + m.getName() + "()");
        }
    }

    private String getClassPath(Class<?> clazz) {
        Path ann = clazz.getAnnotation(Path.class);
        return ann != null ? ann.value() : "";
    }

    private String getHttpMethod(Method m) {
        if (m.isAnnotationPresent(GET.class)) return "GET";
        if (m.isAnnotationPresent(POST.class)) return "POST";
        if (m.isAnnotationPresent(PUT.class)) return "PUT";
        if (m.isAnnotationPresent(DELETE.class)) return "DELETE";
        if (m.isAnnotationPresent(PATCH.class)) return "PATCH";
        if (m.isAnnotationPresent(HEAD.class)) return "HEAD";
        if (m.isAnnotationPresent(OPTIONS.class)) return "OPTIONS";
        return null;
    }

    private record RouteEntry(String httpMethod, String path, Method method) {}
}
