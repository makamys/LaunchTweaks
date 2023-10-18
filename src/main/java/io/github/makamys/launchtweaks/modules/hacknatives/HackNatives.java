package io.github.makamys.launchtweaks.modules.hacknatives;

import static io.github.makamys.launchtweaks.LaunchTweaksAgent.log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.makamys.launchtweaks.IModule;

public class HackNatives implements IModule {
    private final List<String> dirs;
    private static final String DEFAULT_DIRS = "natives:natives" + File.separator + "lwjgl2"; // RFG
    
    public static IModule create(Map<String, String> opts) {
        if(opts.containsKey("hackNatives")) {
            return new HackNatives(opts);
        } else {
            return null;
        }
    }
    
    private static List<String> parseDirSpec(String dirSpec) {
        if(dirSpec == null) return null;
        return Arrays.stream(dirSpec.split(":"))/*.map(x -> new File(x).getAbsolutePath()).*/.collect(Collectors.toList());
    }
    
    public HackNatives(Map<String, String> opts) {
        String dirSpec = opts.get("hackNatives.dir");
        if(dirSpec == null) {
            dirSpec = DEFAULT_DIRS;
        }
        dirs = parseDirSpec(dirSpec);
        
        hackNatives(dirs);
    }
    
    // From https://github.com/MinecraftForge/MinecraftForge/blob/4839d18c7335a62f7611fdbd77c95e36583d5eef/src/userdev/java/net/minecraftforge/userdev/LaunchTesting.java
    private static void hackNatives(List<String> extraNativesDirs)
    {
        String paths = System.getProperty("java.library.path");

        if(paths == null) paths = "";
        
        for(String dir : extraNativesDirs) {
            if(!paths.isEmpty()) paths += File.pathSeparator;
            paths += dir;
        }

        System.setProperty("java.library.path", paths);

        // hack the classloader now.
        try
        {
            final Method initializePathMethod = ClassLoader.class.getDeclaredMethod("initializePath", String.class);
            initializePathMethod.setAccessible(true);
            final Object usrPathsValue = initializePathMethod.invoke(null, "java.library.path");
            final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
            usrPathsField.setAccessible(true);
            usrPathsField.set(null, usrPathsValue);
        }
        catch(Throwable t) {}
    }

}
