package io.opentelemetry.osgi.agent;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.osgi.framework.Bundle;

/**
 * Instruments {@link Bundle#start()}, {@link Bundle#stop()},
 * {@link Bundle#update()}, and {@link Bundle#uninstall()} to create
 * trace spans for every bundle lifecycle operation.
 *
 * <p>Since {@code Framework extends Bundle}, the system bundle's start/stop
 * are also traced. After the system bundle (id 0) starts successfully,
 * the full bundle inventory is logged.
 */
public class BundleLifecycleInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("org.osgi.framework.Bundle");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return implementsInterface(named("org.osgi.framework.Bundle"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                namedOneOf("start", "stop", "update", "uninstall"),
                BundleLifecycleInstrumentation.class.getName() + "$LifecycleAdvice");
    }

    @SuppressWarnings("unused")
    public static class LifecycleAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static Scope onEnter(
                @Advice.This Object bundleObj,
                @Advice.Origin("#m") String methodName) {
            Bundle bundle = (Bundle) bundleObj;
            Span span = OsgiSingletons.tracer()
                    .spanBuilder("Bundle." + methodName + " " + bundle.getSymbolicName())
                    .setAttribute("osgi.bundle.id", bundle.getBundleId())
                    .setAttribute("osgi.bundle.symbolic_name",
                            bundle.getSymbolicName() != null ? bundle.getSymbolicName() : "unknown")
                    .setAttribute("osgi.bundle.version", bundle.getVersion().toString())
                    .setAttribute("osgi.bundle.location", bundle.getLocation())
                    .startSpan();
            return span.makeCurrent();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(
                @Advice.This Object bundleObj,
                @Advice.Origin("#m") String methodName,
                @Advice.Enter Scope scope,
                @Advice.Thrown Throwable throwable) {
            if (scope == null) {
                return;
            }
            Span span = Span.current();
            if (throwable != null) {
                span.setStatus(StatusCode.ERROR, throwable.getMessage());
                span.recordException(throwable);
            }
            span.end();
            scope.close();

            // After system bundle starts, emit the full bundle inventory
            if ("start".equals(methodName) && throwable == null) {
                Bundle bundle = (Bundle) bundleObj;
                if (bundle.getBundleId() == 0) {
                    OsgiSingletons.logBundleInventory(bundle.getBundleContext());
                }
            }
        }
    }
}
