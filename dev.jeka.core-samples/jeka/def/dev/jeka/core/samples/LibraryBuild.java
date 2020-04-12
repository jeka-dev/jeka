package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.TEST;


/**
 * This stands for a build of a Java library, meaning that it not meant to be consumed by end-user but
 * as a dependency of other module. In such, this library is published on a non-public Maven repository.
 *
 * @author Jerome Angibaud
 * @formatter:off
 */
public class LibraryBuild extends JkCommandSet {

    public final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    @Override
    protected void setup() {
       javaPlugin.getProject()
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                        .and("com.google.guava:guava:21.0")
                        .and("com.sun.jersey:jersey-server:1.19.4")
                        .and("junit:junit:4.13", TEST));
    }

    public void cleanPack() {
        clean();javaPlugin.pack();
    }
    
    public static void main(String[] args) {
	    JkInit.instanceOf(LibraryBuild.class, args).cleanPack();
    }


}
