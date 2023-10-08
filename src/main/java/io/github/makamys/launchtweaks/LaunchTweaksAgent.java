package io.github.makamys.launchtweaks;

import static org.objectweb.asm.Opcodes.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class LaunchTweaksAgent {
    private static Instrumentation instrumentation;
    
    private static final boolean debug = Boolean.parseBoolean(System.getProperty("launchTweaks.debug", "false"));
    private static final File LOG_FILE = new File("LaunchTweaks.log");
    private static boolean hasWritten = false;

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
        log("Initializing LaunchTweaksAgent");
        LaunchTweaksAgent.instrumentation = instrumentation;
        
        instrumentation.addTransformer(new LaunchTransformer());
    }

    static void log(String msg) {
        System.out.println("[LaunchTweaks] " + msg);
        try (FileWriter out = new FileWriter(LOG_FILE, hasWritten)){
            out.write(msg + "\n");
            out.flush();
            hasWritten = true;
        } catch (IOException e) {
            
        }
    }

    private static class LaunchTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(final ClassLoader loader, final String className,
                                final Class<?> classBeingRedefined,
                                final ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            if(className.equals("net/minecraft/launchwrapper/LaunchClassLoader")) {
                classfileBuffer = transformClass(className, classfileBuffer);
            }
            return classfileBuffer;
        }
        
        /**
         * <pre>
         * private byte[] runTransformers(final String name, final String transformedName, byte[] basicClass) {
         * ...
         * +   TransformerProfiler.preTransform(name, transformedName, transformer);
         *     basicClass = transformer.transform(name, transformedName, basicClass);
         * +   TransformerProfiler.postTransform(name, transformedName, transformer);
         * ...
         * }
         * </pre>
         */
        private static byte[] transformClass(String name, byte[] bytes) {
            log("Transforming " + name);
            
            if(debug) {
                dumpBytes(name, bytes, true);
            }

            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, 0);
            for(MethodNode m : classNode.methods) {
                if(m.name.equals("runTransformers")) {
                    for(AbstractInsnNode node : m.instructions) {
                        if(node.getOpcode() == Opcodes.INVOKEINTERFACE) {
                            MethodInsnNode methodNode = (MethodInsnNode)node;
                            if(methodNode.owner.equals("net/minecraft/launchwrapper/IClassTransformer") && methodNode.name.equals("transform") && methodNode.desc.equals("(Ljava/lang/String;Ljava/lang/String;[B)[B")) {
                                log("Injecting TransformerProfiler call before and after IClassTransformer#transform call!");
                                m.instructions.insertBefore(methodNode, createCall(true));
                                m.instructions.insert(methodNode, createCall(false));
                            }
                        }
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            
            bytes = writer.toByteArray();
            
            if(debug) {
                dumpBytes(name, bytes, false);
            }
            
            return bytes;
        }
        
        private static void dumpBytes(String name, byte[] bytes, boolean b) {
            try(OutputStream os = new BufferedOutputStream(new FileOutputStream("dump_" + name.replaceAll("/", ".") + "_" + (b ? "pre" : "post") + ".class"))) {
                os.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static InsnList createCall(boolean pre) {
            InsnList list = new InsnList();
            list.add(new VarInsnNode(ALOAD, 1)); // name
            list.add(new VarInsnNode(ALOAD, 2)); // transformedName
            list.add(new VarInsnNode(ALOAD, 5)); // transformer
            list.add(new MethodInsnNode(INVOKESTATIC, "io/github/makamys/launchtweaks/TransformerProfiler", pre ? "preTransform" : "postTransform", "(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/launchwrapper/IClassTransformer;)V"));
            
            return list;
        }
    }
}
