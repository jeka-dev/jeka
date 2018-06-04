package org.jerkar.tool;

import java.lang.annotation.*;

/**
 * Documents the annotated element so that Jerkar can display some information
 * when 'help' or 'helpPlugins' are invoked from command line.
 *
 * @author Jerome Angibaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface JkDocPluginDeps {

    /**
     * The text to display when help is requested.
     */
    Class<? extends JkPlugin>[] value();

}
