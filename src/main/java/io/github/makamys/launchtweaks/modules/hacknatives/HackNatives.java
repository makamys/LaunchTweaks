package io.github.makamys.launchtweaks.modules.hacknatives;

import static io.github.makamys.launchtweaks.LaunchTweaksAgent.log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import io.github.makamys.launchtweaks.IModule;
import io.github.makamys.launchtweaks.modules.staller.Staller;

public class HackNatives implements IModule {
    private String dir;
    private static final String DEFAULT_DIR = "run/natives"; // RFG
    
    public static IModule create(Map<String, String> opts) {
        if(opts.containsKey("hackNatives")) {
            return new HackNatives(opts);
        } else {
            return null;
        }
    }
    
    public HackNatives(Map<String, String> opts) {
        dir = opts.get("hackNatives.dir");
        if(dir == null) {
            log("hackNatives.dir was not specified, using default value of " + DEFAULT_DIR);
        }
        
        hackNatives();
    }
    
    // From https://github.com/MinecraftForge/MinecraftForge/blob/4839d18c7335a62f7611fdbd77c95e36583d5eef/src/userdev/java/net/minecraftforge/userdev/LaunchTesting.java
    private void hackNatives()
    {
        String paths = System.getProperty("java.library.path");
        String nativesDir = dir;

        if (paths == null || paths.isEmpty())
            paths = nativesDir;
        else
            paths += File.pathSeparator + nativesDir;

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
