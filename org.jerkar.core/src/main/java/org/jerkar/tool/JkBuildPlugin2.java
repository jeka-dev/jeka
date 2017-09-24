package org.jerkar.tool;

/**
 * Plugin for {@link JkBuild}. Applying such a plugin to a {@link JkBuild} instance is supposed to modify the specified {@link JkBuild} in
 * accordance with what the plugin is suppsed to do.
 *
 * @param <T> The type of {@link JkBuild} the plugin apply.
 */
public interface JkBuildPlugin2<T extends JkBuild> {

    /**
     * Modify the enclosed build instance ( {@link #build()}) in accordance with what this plugin is supposed to do.
     */
    void apply(JkBuild build);

}
