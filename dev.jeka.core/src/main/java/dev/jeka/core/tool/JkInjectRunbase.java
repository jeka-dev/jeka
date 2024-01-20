package dev.jeka.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds an entry to the jeka-src classpath where this annotation is declared.
 * Typically, the annotated element is a <code>JkClass</code> class from 'jeka/def' source directory.
 * But when used on a public field of type  <code>JkClass</code> within a <code>JkClass</code> class,
 * the annotated field is injected with an initialised <code>JkClass</code> instance of the imported project.<p>
 *
 * Example :
 * <pre><code>
 * public class FatJarBuild extends JkClass {
 *
 *     ProjectJkBean projectPlugin = getPlugin(ProjectJkBean.class);
 *
 *     @JkDefImport("../anotherJekaProject")
 *     private AClassicBuild sampleBuild;
 *
 *     ...
 * </code></pre>
 *
 * 
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface JkInjectRunbase {

    /** Dependee project relative path */
    String value();

}
