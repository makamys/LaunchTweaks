package io.github.makamys.stallman;

import static org.objectweb.asm.Opcodes.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class StallmanAgent {
    private static Instrumentation instrumentation;

    public static void premain(final String agentArgs, final Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    public static void agentmain(final String agentArgs, final Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    private static synchronized void start(final String agentArgs, final Instrumentation instrumentation) {
        if(StallmanAgent.instrumentation != null) {
            return;
        }
        StallmanAgent.instrumentation = instrumentation;
        instrumentation.addTransformer(new LaunchTransformer());
    }

    private static class LaunchTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(final ClassLoader loader, final String className,
                                final Class<?> classBeingRedefined,
                                final ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            if(className.equals("GradleStart")) {
                classfileBuffer = transformGradleStart(classfileBuffer);
            }
            return classfileBuffer;
        }
        
        /**
         * <pre>
         * public static void main(String[] args) throws Throwable {
         * +   LaunchTransformer.Hooks.preMain();
         *     // launch
         *     (new GradleStart()).launch(args);
         * }
         * </pre>
         */
        private static byte[] transformGradleStart(byte[] bytes) {
            System.out.println("Transforming GradleStart to add hook for waiting before launch");

            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, 0);
            for(MethodNode m : classNode.methods) {
                if (m.name.equals("main")) {
                    m.instructions.insert(new MethodInsnNode(INVOKESTATIC, "io/github/makamys/stallman/StallmanAgent$LaunchTransformer$Hooks", "preMain", "()V", false));
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        }

        @SuppressWarnings("unused")
        public static class Hooks {

            public static void preMain() {
                System.out.println("Delaying launch, attach a profiler now!");
                for(int i = 10; i > 0; i--) {
                    System.out.println(i + " seconds left...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
