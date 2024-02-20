package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Class for instantiating builds while displaying meaningful information about environment on console.
 */
public final class JkInit {

    private JkInit() {
    }

    public static JkRunbase runbase(boolean skipCompile, String ...args) {
        String[] extraArg = skipCompile ? new String[] {"-sk"} : new String[0];
        String[] effectiveArgs = JkUtilsIterable.concat(args, extraArg);
        return PicocliMain.doMain(effectiveArgs);
    }

    public static <T extends KBean> T kbean(Class<T> clazz, String... args) {
        String[] effectiveArgs = JkUtilsIterable.concat(args, new String[] {"-kb=" + clazz.getName()});
        return runbase(true, effectiveArgs).load(clazz);
    }

    public static <T extends KBean> T kbean(Class<T> clazz, String[] args, String... extraArgs) {
        String[] effectiveArgs = JkUtilsIterable.concat(args, extraArgs);
        return kbean(clazz, effectiveArgs);
    }

}
