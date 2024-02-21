package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsIterable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class for instantiating builds while displaying meaningful information about environment on console.
 */
public final class JkInit {

    private JkInit() {
    }

    /**
     * Run JeKA and returns a usable {@link JkRunbase} from where we can
     * instantiate KBean programmatically.<p>
     *
     * This method is meant to bbe used inside a regular Main method.
     */
    public static JkRunbase runbase(boolean skipCompile, String ...args) {
        String[] extraArg = skipCompile ? new String[] {"-sk"} : new String[0];
        String[] effectiveArgs = JkUtilsIterable.concat(args, extraArg);
        Path baseDir = Paths.get("");
        return Main.exec(baseDir, effectiveArgs);
    }

    /**
     * Runs JeKA and returns a usable {@link JkRunbase} and returns
     * a kbean instance of the specified class. The specified KBean
     * is configured to be the default one. <p>
     *
     * This method is meant to bbe used inside a regular Main method.
     */
    public static <T extends KBean> T kbean(Class<T> clazz, String... args) {
        String[] effectiveArgs = JkUtilsIterable.concat(args, new String[] {"-kb=" + clazz.getName()});
        return runbase(true, effectiveArgs).load(clazz);
    }

    /**
     * Similar to {@link #kbean(Class, String...)} but this the possibility
     * to add conveniently extra args in addition to the ones supplied
     * by Main method.
     */
    public static <T extends KBean> T kbean(Class<T> clazz, String[] args, String... extraArgs) {
        String[] effectiveArgs = JkUtilsIterable.concat(args, extraArgs);
        return kbean(clazz, effectiveArgs);
    }

}
