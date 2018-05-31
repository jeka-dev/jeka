package org.jerkar.tool;

import org.jerkar.api.utils.JkUtilsString;

public abstract class JkPlugin {

    private static final String CLASS_PREFIX = JkPlugin.class.getSimpleName();

    protected final JkBuild build;

    protected JkPlugin(JkBuild build) {
        this.build = build;
        preConfigure();
    }

    protected void addDefaultAction(Runnable action) {
        build.addDefaultOperation(action);
    }

    /**
     * Called prior environment options has been set. Override this method to define sensitive default overridable
     * by environment option.
     */
    protected void preConfigure() {
    }

    /**
     * Called once options has been injected into this plugin instance. Typically this should
     * contain the code that initialize this plugin and decorate the build instance.
     */
    protected void decorate() {
    }

    final String name() {
        final String className = this.getClass().getSimpleName();
        if (! className.startsWith(CLASS_PREFIX) || className.equals(CLASS_PREFIX)) {
            throw new IllegalStateException(String.format("Plugin class not properly named. Name should be formatted as " +
                    "'%sXxxx' where xxxx is the name of the plugin (uncapitalized). Was %s.", CLASS_PREFIX, className));
        }
        final String suffix = className.substring(CLASS_PREFIX.length());
        return JkUtilsString.uncapitalize(suffix);
    }
}
