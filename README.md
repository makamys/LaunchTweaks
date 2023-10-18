# LaunchTweaks

A Java agent that modifies the earliest part of an application's lifetime, the launch process. It mainly targets LaunchWrapper (used by Minecraft versions prior to `[idk, 1.13 or something?]`), but some of its features will work with any Java application.

## Usage

LaunchTweaks consists of separate modules that have to be enabled through the agent's arguments.

For example, the following JVM argument will enable the agent with the `stall` and `profile` modules, and sets the `stall` module's `countdown` parameter to 5.

```
-javaagent:/path/to/LaunchTweaks.jar=profile,stall,stall.countdown=5
```

The agent writes its logging output to stdout as well as the `LaunchTweaks.log` file in the current working directory.

## Modules

### `profile`

Profiles how long each class transformer takes, and writes the results to `transformer_profiler.csv` in the current working directory.

### `stall`

Stalls the launch of the application by 10 seconds (configurable), allowing you to attach a profiler before it starts. Works with any Java application.

Arguments:
- `countdown`: How many seconds to delay the launch by.
  - Example: `stall.countdown=5`
- `target`: Set the main method(s) explicitly instead of having them be automatically detected.
  - Example: `stall.target=example.org.Main#main([Ljava/lang/String;)V,example.org.Main2`

### `hackNatives`

Implements ForgeGradle's native library hack. This allows RFG to be used seamlessly in Eclipse (which RFG does not generate run configurations for).

It may also be useful for people stuck with ForgeGradle suffering from https://github.com/MinecraftForge/ForgeGradle/issues/652, since it backports the new version of the hack.

Arguments:
- `dirs`: Comma-separated list of directories to add to the native library path.
  - Example: `some/relative/path:/some/absolute/path`

## License

This project is licensed under the [Unlicense](UNLICENSE).
