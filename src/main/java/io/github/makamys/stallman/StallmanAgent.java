package io.github.makamys.stallman;

import static org.objectweb.asm.Opcodes.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class StallmanAgent {
    private static Instrumentation instrumentation;
    final static Map<String, Set<String>> targets = new HashMap<>();
    static int countdown = 10;
    
    private static final boolean debug = Boolean.parseBoolean(System.getProperty("stallman.debug", "false"));

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
        String[] args = (agentArgs == null ? "" : agentArgs).split(",");
        if(args.length > 0) {
            parseTargets(args[0]);
        } else {
            System.out.println("No targets specified! Example: -javaagent:stallman.jar=example.org.Main");
        }
        applyOptions(parseOptions(args));
        
        instrumentation.addTransformer(new LaunchTransformer());
    }
    
    private static void applyOptions(Map<String, String> opts) {
        debugPrint("Options: " + opts);
        if(opts.containsKey("countdown")) {
            countdown = Integer.parseInt(opts.get("countdown"));
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for(int i = 1; i < args.length; i++) {
            String[] kv = args[i].split("=");
            String k = kv[0];
            String v = kv.length > 1 ? kv[1] : "";
            options.put(k, v);
        }
        return options;
    }

    private static void parseTargets(String targetsSpec) {
        if(targetsSpec == null) return;
        
        for(String targetSpec : targetsSpec.split(";")) {
            String[] parts = targetSpec.split("#");
            String klass = parts[0];
            String method = parts.length > 1 ? parts[1] : "main([Ljava/lang/String;)V";
            
            targets.computeIfAbsent(klass, k -> new HashSet<String>()).add(method);
        }
    }

    static void debugPrint(String s) {
        if(debug) {
            System.out.println("[debug] " + s);
        }
    }

    private static class LaunchTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(final ClassLoader loader, final String className,
                                final Class<?> classBeingRedefined,
                                final ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            // TODO inner classes, or even, non top level classes haven't been tested
            if(targets.containsKey(className)) {
                classfileBuffer = transformClass(className, classfileBuffer);
            }
            return classfileBuffer;
        }
        
        /**
         * <pre>
         * public static void foo(String[] args) {
         * +   LaunchTransformer.Hooks.preMain();
         *     doStuff();
         * }
         * </pre>
         */
        private static byte[] transformClass(String name, byte[] bytes) {
            System.out.println("Transforming " + name + " to add hook for waiting before launch");
            Set<String> targetMethods = targets.get(name);
            
            debugPrint("Target methods: " + targetMethods);
            
            int transformedMethods = 0;

            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, 0);
            for(MethodNode m : classNode.methods) {
                debugPrint("Considering method: " + m.name + " " + m.desc);
                if (targetMethods.contains(m.name) || targetMethods.contains(m.name + m.desc)) {
                    debugPrint("Method will be patched!");
                    m.instructions.insert(new MethodInsnNode(INVOKESTATIC, "io/github/makamys/stallman/StallmanAgent$LaunchTransformer$Hooks", "preMain", "()V", false));
                    transformedMethods++;
                } else {
                    debugPrint("Skipping method");
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            
            System.out.println("Transformed " + transformedMethods + " method" + (transformedMethods == 1 ? "" : "s") + " in " + name);
            
            return writer.toByteArray();
        }

        @SuppressWarnings("unused")
        public static class Hooks {

            public static void preMain() {
                System.out.println("Delaying launch, attach a profiler now!");
                for(int i = countdown; i > 0; i--) {
                    System.out.println(i + " seconds left...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
