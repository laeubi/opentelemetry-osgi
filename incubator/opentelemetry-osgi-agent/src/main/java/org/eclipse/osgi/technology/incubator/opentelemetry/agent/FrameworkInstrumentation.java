package org.eclipse.osgi.technology.incubator.opentelemetry.agent;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

/**
 * Instruments {@link Framework#init()} to capture the system BundleContext
 * as soon as the OSGi framework is initialized.
 *
 * <p>Per the OSGi spec, after {@code Framework.init()} returns successfully,
 * {@code getBundleContext()} returns the system bundle's context — giving full
 * access to the framework for registering metrics and collecting inventory.
 *
 * <p>Vendor-agnostic: matches any class implementing the standard
 * {@code org.osgi.framework.launch.Framework} interface (Felix, Equinox, etc.).
 */
public class FrameworkInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed("org.osgi.framework.launch.Framework");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return implementsInterface(named("org.osgi.framework.launch.Framework"));
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                named("init"),
                FrameworkInstrumentation.class.getName() + "$InitAdvice");
    }

    @SuppressWarnings("unused")
    public static class InitAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
        public static void onInitExit(
                @Advice.This Object frameworkObj,
                @Advice.Thrown Throwable throwable) {
            if (throwable != null) {
                return;
            }
            Framework framework = (Framework) frameworkObj;
            BundleContext context = framework.getBundleContext();
            if (context != null) {
                OsgiSingletons.onFrameworkInit(context);
            }
        }
    }
}
