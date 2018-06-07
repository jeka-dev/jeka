package org.jerkar.tool;

import org.jerkar.api.utils.JkUtilsString;

public abstract class JkPlugin {

    private static final String CLASS_PREFIX = JkPlugin.class.getSimpleName();

    // Build instance owning  this plugin instance.
    protected final JkBuild build;

    /**
     * Right after to be instantiated, plugin instances are likely to configured by the owning build.
     * Therefore, every plugin members that are likely to be configured by the owning build must be
     * initialized in the constructor.
     */
    protected JkPlugin(JkBuild build) {
        this.build = build;
    }

    protected void addDefaultAction(Runnable action) {
        build.addDefaultOperation(action);
    }

    protected void decorateBuild() {
    }

    public final String name() {
        return nameOf(this.getClass());
    }

    static final String nameOf(Class<? extends JkPlugin> pluginClass) {
        final String className = pluginClass.getSimpleName();
        if (! className.startsWith(CLASS_PREFIX) || className.equals(CLASS_PREFIX)) {
            throw new IllegalStateException(String.format("Plugin class not properly named. Name should be formatted as " +
                    "'%sXxxx' where xxxx is the name of the plugin (uncapitalized). Was %s.", CLASS_PREFIX, className));
        }
        final String suffix = className.substring(CLASS_PREFIX.length());
        return JkUtilsString.uncapitalize(suffix);
    }
}
