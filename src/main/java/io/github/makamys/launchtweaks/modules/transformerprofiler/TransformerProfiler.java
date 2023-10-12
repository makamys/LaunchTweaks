package io.github.makamys.launchtweaks.modules.transformerprofiler;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.Map;

import static io.github.makamys.launchtweaks.LaunchTweaksAgent.log;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import io.github.makamys.launchtweaks.IModule;

public class TransformerProfiler implements IModule {
    public static IModule create(Map<String, String> opts) {
        if(opts.containsKey("profile")) {
            return new TransformerProfiler();
        } else {
            return null;
        }
    }
    
    @Override
    public boolean wantsToTransform(String className) {
        return className.equals("net/minecraft/launchwrapper/LaunchClassLoader");
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
    @Override
    public void transform(ClassNode classNode) {
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
    }
    
    private static InsnList createCall(boolean pre) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(ALOAD, 1)); // name
        list.add(new VarInsnNode(ALOAD, 2)); // transformedName
        list.add(new VarInsnNode(ALOAD, 5)); // transformer
        list.add(new MethodInsnNode(INVOKESTATIC, "io/github/makamys/launchtweaks/modules/transformerprofiler/TransformerProfilerHooks", pre ? "preTransform" : "postTransform", "(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/launchwrapper/IClassTransformer;)V"));
        
        return list;
    }
}
