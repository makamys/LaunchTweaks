package io.github.makamys.launchtweaks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public class TransformerProfiler {

    private static Map<IClassTransformer, Long> times = new HashMap<IClassTransformer, Long>();
    
    private static long transformStartTime = -1;
    
    private static boolean inited;
    
    private static void init() {
        if(!inited) {
            inited = true;
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                @Override
                public void run() {
                    FileWriter fw = null;
                    try {
                        fw = new FileWriter(new File(Launch.minecraftHome, "transformer_profiler.csv"));
                        fw.write("Transformer,Total time (ms)\n");
                        for(Entry<IClassTransformer, Long> entry : times.entrySet()) {
                            fw.write(entry.getKey().getClass().getCanonicalName() + "," + (entry.getValue() / 1000000) + "\n");
                        }
                    } catch(IOException e) {
                        System.err.println("Failed to write transformer profiler results");
                        e.printStackTrace();
                    } finally {
                        try {
                            if(fw != null) fw.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }}, "LaunchClassLoader transformer profiler save thread"));
        }
    }
    
    public static void preTransform(String name, String transformedName, IClassTransformer transformer) {
        init();
        transformStartTime = System.nanoTime();
    }

    public static void postTransform(String name, String transformedName, IClassTransformer transformer) {
        long transformTime = System.nanoTime() - transformStartTime;
        times.put(transformer, times.getOrDefault(transformer, 0L) + transformTime);
    }

}
