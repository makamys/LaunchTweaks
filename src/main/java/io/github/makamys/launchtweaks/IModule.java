package io.github.makamys.launchtweaks;

import org.objectweb.asm.tree.ClassNode;

public interface IModule {
    public default boolean wantsToTransform(String className) { return false; }
    public default void transform(ClassNode classNode) {}
}
