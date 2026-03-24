package org.eclipse.osgi.technology.incubator.opentelemetry.weaver.scr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.technology.incubator.opentelemetry.weaving.OpenTelemetryProxy;
import org.eclipse.osgi.technology.incubator.opentelemetry.weaving.SafeClassWriter;
import org.eclipse.osgi.technology.incubator.opentelemetry.weaving.Weaver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * Weaver that instruments Declarative Services component lifecycle methods.
 * <p>
 * Inspired by biz.aQute.trace, this weaver parses the {@code Service-Component}
 * manifest header to locate DS XML descriptors, extracts implementation class
 * names, and instruments methods annotated with {@code @Activate},
 * {@code @Deactivate}, and {@code @Modified} to produce OpenTelemetry spans
 * and metrics for each lifecycle operation.
 * <p>
 * The parsed component class names are cached per bundle for efficiency.
 */
public class ScrWeaver implements Weaver {

    private static final Logger LOG = Logger.getLogger(ScrWeaver.class.getName());

    private static final Pattern IMPLEMENTATION_CLASS = Pattern.compile(
            "<implementation\\s+class\\s*=\\s*\"([^\"]+)\"");

    private final ConcurrentHashMap<Long, Set<String>> cache = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "scr";
    }

    @Override
    public boolean canWeave(String className, WovenClass wovenClass) {
        Bundle bundle = wovenClass.getBundleWiring().getBundle();
        Set<String> componentClasses = getComponentClasses(bundle);
        return componentClasses.contains(className);
    }

    @Override
    public void weave(WovenClass wovenClass, OpenTelemetryProxy telemetry) {
        ScrInstrumentationHelper.setProxy(telemetry);
        addDynamicImports(wovenClass);

        byte[] original = wovenClass.getBytes();
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new SafeClassWriter(reader, wovenClass);
        ScrClassVisitor visitor = new ScrClassVisitor(writer);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);

        if (visitor.isTransformed()) {
            wovenClass.setBytes(writer.toByteArray());
            LOG.log(Level.FINE, () -> "Wove SCR lifecycle: " + wovenClass.getClassName());
        }
    }

    private Set<String> getComponentClasses(Bundle bundle) {
        return cache.computeIfAbsent(bundle.getBundleId(), id -> {
            String header = bundle.getHeaders().get("Service-Component");
            if (header == null) {
                return Set.of();
            }
            return parseServiceComponentHeader(bundle, header);
        });
    }

    private Set<String> parseServiceComponentHeader(Bundle bundle, String header) {
        Set<String> classes = new HashSet<>();
        String[] paths = header.split("\\s*,\\s*");
        for (String path : paths) {
            path = path.trim();
            if (path.isEmpty()) {
                continue;
            }
            try {
                if (path.contains("*")) {
                    parseWildcardEntries(bundle, path, classes);
                } else {
                    URL url = bundle.getEntry(path);
                    if (url != null) {
                        parseXml(url, classes);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to parse DS XML: " + path
                        + " in bundle " + bundle.getSymbolicName(), e);
            }
        }
        return Collections.unmodifiableSet(classes);
    }

    private void parseWildcardEntries(Bundle bundle, String path, Set<String> classes) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            return;
        }
        String dir = path.substring(0, lastSlash);
        String pattern = path.substring(lastSlash + 1);
        Enumeration<URL> entries = bundle.findEntries(dir, pattern, false);
        if (entries != null) {
            while (entries.hasMoreElements()) {
                try {
                    parseXml(entries.nextElement(), classes);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to parse DS XML entry", e);
                }
            }
        }
    }

    private void parseXml(URL url, Set<String> classes) throws IOException {
        try (InputStream is = url.openStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            Matcher matcher = IMPLEMENTATION_CLASS.matcher(sb.toString());
            if (matcher.find()) {
                classes.add(matcher.group(1));
            }
        }
    }

    private void addDynamicImports(WovenClass wovenClass) {
        List<String> imports = wovenClass.getDynamicImports();
        imports.add("io.opentelemetry.api");
        imports.add("io.opentelemetry.api.trace");
        imports.add("io.opentelemetry.api.metrics");
        imports.add("io.opentelemetry.api.common");
        imports.add("io.opentelemetry.context");
        imports.add("org.eclipse.osgi.technology.incubator.opentelemetry.weaving");
        imports.add("org.eclipse.osgi.technology.incubator.opentelemetry.weaver.scr");
    }
}
