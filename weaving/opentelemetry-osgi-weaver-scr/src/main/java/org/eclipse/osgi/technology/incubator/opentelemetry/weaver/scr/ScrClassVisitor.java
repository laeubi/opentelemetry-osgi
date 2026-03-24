package org.eclipse.osgi.technology.incubator.opentelemetry.weaver.scr;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link ClassVisitor} that delegates method instrumentation to
 * {@link ScrMethodVisitor} for methods carrying DS lifecycle annotations.
 * <p>
 * All non-synthetic methods and constructors are passed to the method visitor,
 * which detects {@code @Activate}, {@code @Deactivate}, and {@code @Modified}
 * annotations and wraps annotated methods with OpenTelemetry instrumentation.
 */
class ScrClassVisitor extends ClassVisitor {

    private String className;
    private boolean transformed;

    ScrClassVisitor(ClassVisitor delegate) {
        super(Opcodes.ASM9, delegate);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
            String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if ((access & Opcodes.ACC_SYNTHETIC) != 0 || (access & Opcodes.ACC_BRIDGE) != 0) {
            return mv;
        }
        if ("<clinit>".equals(name)) {
            return mv;
        }

        ScrMethodVisitor smv = new ScrMethodVisitor(mv, access, name, descriptor, className);
        smv.setTransformCallback(() -> transformed = true);
        return smv;
    }

    boolean isTransformed() {
        return transformed;
    }
}
