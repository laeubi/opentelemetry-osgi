package io.opentelemetry.osgi.agent;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;

import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Entry point for OSGi framework instrumentation.
 * Registered via SPI in META-INF/services.
 *
 * <p>Only activates when the OSGi Framework API is present on the classloader,
 * ensuring zero overhead for non-OSGi applications.
 */
public class OsgiInstrumentationModule extends InstrumentationModule {

    public OsgiInstrumentationModule() {
        super("osgi-framework");
    }

    @Override
    public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
        return hasClassesNamed("org.osgi.framework.launch.Framework");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return List.of(
                new FrameworkInstrumentation(),
                new BundleLifecycleInstrumentation(),
                new BundleActivatorInstrumentation(),
                new BundleContextInstrumentation());
    }
}
