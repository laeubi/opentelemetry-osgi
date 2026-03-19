package io.opentelemetry.osgi.agent;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Instruments {@link org.osgi.framework.BundleActivator#start(BundleContext)} and
 * {@link org.osgi.framework.BundleActivator#stop(BundleContext)} to create trace spans
 * for activator execution.
 *
 * <p>This produces child spans under the Bundle.start/stop spans from
 * {@link BundleLifecycleInstrumentation}, showing how much time the activator
 * consumes within the overall bundle lifecycle.
 */
public class BundleActivatorInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("org.osgi.framework.BundleActivator");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return implementsInterface(named("org.osgi.framework.BundleActivator"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                namedOneOf("start", "stop").and(takesArguments(1)),
                BundleActivatorInstrumentation.class.getName() + "$ActivatorAdvice");
    }

    @SuppressWarnings("unused")
    public static class ActivatorAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static Scope onEnter(
                @Advice.This Object activatorObj,
                @Advice.Argument(0) Object contextObj,
                @Advice.Origin("#m") String methodName) {
            BundleContext context = (BundleContext) contextObj;
            Bundle bundle = context.getBundle();
            Span span = OsgiSingletons.tracer()
                    .spanBuilder("BundleActivator." + methodName + " "
                            + bundle.getSymbolicName())
                    .setAttribute("osgi.bundle.id", bundle.getBundleId())
                    .setAttribute("osgi.bundle.symbolic_name",
                            bundle.getSymbolicName() != null ? bundle.getSymbolicName() : "unknown")
                    .setAttribute("osgi.bundle.version", bundle.getVersion().toString())
                    .setAttribute("osgi.activator.class", activatorObj.getClass().getName())
                    .startSpan();
            return span.makeCurrent();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(
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
        }
    }
}
