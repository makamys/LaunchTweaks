package io.github.makamys.launchtweaks.modules.staller;

import static io.github.makamys.launchtweaks.LaunchTweaksAgent.log;
import static io.github.makamys.launchtweaks.LaunchTweaksAgent.debugLog;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import io.github.makamys.launchtweaks.IModule;

public class Staller implements IModule {
    public static int countdown;
    private final static Map<String, Set<String>> targets = new HashMap<>();
    
    public static IModule create(Map<String, String> opts) {
        if(opts.containsKey("stall")) {
            return new Staller(opts);
        } else {
            return null;
        }
    }
    
    public Staller(Map<String, String> opts) {
        if(opts.containsKey("stall.target")) {
            parseTargets(opts.get("stall.target"));
        } else {
            String mainClass = detectMainClass();
            if(mainClass != null) {
                log("Detected main class " + mainClass);
                parseTargets(mainClass);
            } else {
                log("Failed to detect main class! You will need to provide it using an argument, for example: stall.target=example.org.Main");
            }
        }
        
        if(opts.containsKey("stall.countdown")) {
            countdown = Integer.parseInt(opts.get("stall.countdown"));
        } else {
            countdown = 10;
        }
    }
    
    private static String detectMainClass() {
        try {
            return System.getProperty("sun.java.command").split(" ")[0];
        } catch(Exception e) {
            return null;
        }
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
    
    @Override
    public boolean wantsToTransform(String className) {
        // TODO inner classes haven't been tested
        className = className.replace('/', '.');
        return targets.containsKey(className);
    }
    
    /**
     * <pre>
     * public static void foo(String[] args) {
     * +   LaunchTransformer.Hooks.preMain();
     *     doStuff();
     * }
     * </pre>
     */
    @Override
    public void transform(ClassNode classNode) {
        String name = classNode.name.replace('/', '.');
        log("Transforming " + name + " to add hook for waiting before launch");
        Set<String> targetMethods = targets.get(name);
        
        debugLog("Target methods: " + targetMethods);
        
        int transformedMethods = 0;

        for(MethodNode m : classNode.methods) {
            debugLog("Considering method: " + m.name + " " + m.desc);
            if (targetMethods.contains(m.name) || targetMethods.contains(m.name + m.desc)) {
                debugLog("Method will be patched!");
                m.instructions.insert(new MethodInsnNode(INVOKESTATIC, "io/github/makamys/launchtweaks/modules/staller/Staller$Hooks", "preMain", "()V", false));
                transformedMethods++;
            } else {
                debugLog("Skipping method");
            }
        }
        
        log("Transformed " + transformedMethods + " method" + (transformedMethods == 1 ? "" : "s") + " in " + name);
    }
    
    public static class Hooks {
        public static void preMain() {
            log("Delaying launch, attach a profiler now!");
            for(int i = countdown; i > 0; i--) {
                log(i + " seconds left...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
