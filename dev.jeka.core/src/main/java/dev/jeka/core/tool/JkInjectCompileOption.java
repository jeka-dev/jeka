package dev.jeka.core.tool;

import java.lang.annotation.*;

/**
 * Specifies the options of the Jeka compiler for def classes.
 * Normally, one option by annotation is expected tough this annotation is repeatable.
 * You can declare it has follow :
 * <pre><code>
 * @JkCompileOption("-deprecation")
 * @JkCompileOption({"-processorPath", "/foo/bar"})
 * </code></pre>
 *
 *
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(JkInjectCompileOption.JkCompileOptions.class)
public @interface JkInjectCompileOption {

    /**
     * The dependency to import. It can be a module dependency (as "com.google.guava:guava:18.0")
     * or a file dependency (as "../lib-folder.mylib.jar").
     */
    String[] value();


    /**
     * Repeatable container.
     */
    @Target(ElementType.TYPE)
    @interface JkCompileOptions {
        JkInjectCompileOption[] value();
    }

}