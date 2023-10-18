# LaunchTweaks

A Java agent that modifies to earliest part of an application, the launch process. It mainly targets LaunchWrapper (used by Minecraft versions prior to `[idk, 1.13 or something?]`), but some its features will work with any Java application.

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