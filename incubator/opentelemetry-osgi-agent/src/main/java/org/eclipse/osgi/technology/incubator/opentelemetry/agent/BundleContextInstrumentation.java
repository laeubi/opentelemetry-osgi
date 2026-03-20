package org.eclipse.osgi.technology.incubator.opentelemetry.agent;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;

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
 * Instruments {@link BundleContext#registerService} and {@link BundleContext#installBundle}
 * to create trace spans for service registration and bundle installation.
 *
 * <p>Handles all overloads of registerService (String, String[], Class) and
 * both overloads of installBundle (with/without InputStream).
 */
public class BundleContextInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("org.osgi.framework.BundleContext");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return implementsInterface(named("org.osgi.framework.BundleContext"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                named("registerService"),
                BundleContextInstrumentation.class.getName() + "$RegisterServiceAdvice");
        transformer.applyAdviceToMethod(
                named("installBundle"),
                BundleContextInstrumentation.class.getName() + "$InstallBundleAdvice");
    }

    @SuppressWarnings("unused")
    public static class RegisterServiceAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static Scope onEnter(
                @Advice.This Object contextObj,
                @Advice.AllArguments Object[] args) {
            BundleContext context = (BundleContext) contextObj;
            Bundle bundle = context.getBundle();
            String serviceName = extractServiceName(args);
            Span span = OsgiSingletons.tracer()
                    .spanBuilder("BundleContext.registerService " + serviceName)
                    .setAttribute("osgi.bundle.id", bundle.getBundleId())
                    .setAttribute("osgi.bundle.symbolic_name",
                            bundle.getSymbolicName() != null ? bundle.getSymbolicName() : "unknown")
                    .setAttribute("osgi.service.name", serviceName)
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

        private static String extractServiceName(Object[] args) {
            if (args.length > 0) {
                Object first = args[0];
                if (first instanceof String s) {
                    return s;
                }
                if (first instanceof Class<?> c) {
                    return c.getName();
                }
                if (first instanceof String[] arr && arr.length > 0) {
                    return String.join(", ", arr);
                }
            }
            return "unknown";
        }
    }

    @SuppressWarnings("unused")
    public static class InstallBundleAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static Scope onEnter(
                @Advice.This Object contextObj,
                @Advice.Argument(0) String location) {
            BundleContext context = (BundleContext) contextObj;
            Bundle bundle = context.getBundle();
            Span span = OsgiSingletons.tracer()
                    .spanBuilder("BundleContext.installBundle")
                    .setAttribute("osgi.bundle.id", bundle.getBundleId())
                    .setAttribute("osgi.bundle.symbolic_name",
                            bundle.getSymbolicName() != null ? bundle.getSymbolicName() : "unknown")
                    .setAttribute("osgi.bundle.install_location", location)
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
