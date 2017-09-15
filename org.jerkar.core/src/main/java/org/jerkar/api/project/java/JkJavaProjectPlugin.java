package org.jerkar.api.project.java;

import java.util.function.Consumer;

/**
 * Plugin mechanism for {@link JkJavaProject} is dumb simple. Plugins just has to implements
 * this interface and modify the project in accordance.
 */
@FunctionalInterface
public interface JkJavaProjectPlugin extends Consumer<JkJavaProject> {


}
