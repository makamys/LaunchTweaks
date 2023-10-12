package io.github.makamys.launchtweaks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import io.github.makamys.launchtweaks.modules.staller.Staller;
import io.github.makamys.launchtweaks.modules.transformerprofiler.TransformerProfiler;

public class LaunchTweaksAgent {
    private static Instrumentation instrumentation;
    
    private static final boolean debug = Boolean.parseBoolean(System.getProperty("launchTweaks.debug", "false"));
    private static final File LOG_FILE = new File("LaunchTweaks.log");
    private static boolean hasWritten = false;
    
    private static List<IModule> modules = new ArrayList<>();

    public static void premain(final String agentArgs, final Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    public static void agentmain(final String agentArgs, final Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    private static synchronized void start(final String agentArgs, final Instrumentation instrumentation) {
        if(LaunchTweaksAgent.instrumentation != null) {
            return;
        }
        log("Initializing LaunchTweaks agent");
        LaunchTweaksAgent.instrumentation = instrumentation;
        
        String[] args = agentArgs == null ? new String[0] : agentArgs.split(",");
        applyOptions(parseOptions(args));
        
        log("Initialized with " + modules.size() + " module" + pluralSuffix(modules.size()));
        
        instrumentation.addTransformer(new LaunchTransformer());
    }
    
    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for(int i = 0; i < args.length; i++) {
            String[] kv = args[i].split("=");
            String k = kv[0];
            String v = kv.length > 1 ? kv[1] : "";
            options.put(k, v);
        }
        return options;
    }
    
    private static void applyOptions(Map<String, String> opts) {
        debugLog("Options: " + opts);
        
        addModule(TransformerProfiler.create(opts));
        addModule(Staller.create(opts));
    }
    
    private static void addModule(IModule module) {
        if(module != null) {
            debugLog("Adding module " + module.getClass().getName());
            modules.add(module);
        }
    }
    
    private static void log(String msg, String level) {
        System.out.println("[" + level + "] [LaunchTweaks] " + msg);
        try (FileWriter out = new FileWriter(LOG_FILE, hasWritten)){
            out.write(msg + "\n");
            out.flush();
            hasWritten = true;
        } catch (IOException e) {
            
        }
    }

    public static void log(String msg) {
        log(msg, " INFO");
    }

    public static void debugLog(String msg) {
        if(debug) {
            log(msg, "DEBUG");
        }
    }
    
    static String pluralSuffix(int n) {
        return n == 1 ? "" : "s";
    }

    private static class LaunchTransformer implements ClassFileTransformer {
        List<IModule> transformationWanters = new ArrayList<>();
        
        @Override
        public byte[] transform(final ClassLoader loader, final String className,
                                final Class<?> classBeingRedefined,
                                final ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            try {
                for(IModule module : modules) {
                    if(module.wantsToTransform(className)) {
                        transformationWanters.add(module);
                    }
                }
                if(!transformationWanters.isEmpty()) {
                    log("Transforming " + className + " with " + transformationWanters.size() + " transformer" + pluralSuffix(transformationWanters.size()));
                    
                    if(debug) {
                        dumpBytes(className, classfileBuffer, true);
                    }
                    
                    ClassNode classNode = new ClassNode();
                    ClassReader classReader = new ClassReader(classfileBuffer);
                    classReader.accept(classNode, 0);
                    
                    for(IModule module : transformationWanters) {
                        debugLog("Running transformer " + module.getClass().getName());
                        module.transform(classNode);
                    }
                    
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    classNode.accept(writer);
                    
                    classfileBuffer = writer.toByteArray();
                    
                    if(debug) {
                        dumpBytes(className, classfileBuffer, false);
                    }
                    
                    return classfileBuffer;
                }
            } finally {
                transformationWanters.clear();
            }
            return classfileBuffer;
        }
        
        private static void dumpBytes(String name, byte[] bytes, boolean b) {
            try(OutputStream os = new BufferedOutputStream(new FileOutputStream("dump_" + name.replaceAll("/", ".") + "_" + (b ? "pre" : "post") + ".class"))) {
                os.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
