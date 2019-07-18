package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Plugin instances are owned by a JkCommands instance. The relationship is bidirectional. JkCommands instances may
 * invoke plugin methods or fields and plugin instances may invoke owner methods.
 *
 * Therefore plugins can interact with or load other plugins into the owner instance, which is quite common with Jeka.
 */
public abstract class JkPlugin {

    private static final String CLASS_PREFIX = JkPlugin.class.getSimpleName();

    private final JkCommands commands;

    /*
     * Right after to be instantiated, plugin instances are likely to be configured by the owning commands.
     * Therefore, every plugin members that are likely to be configured by the owning commands must be
     * initialized in the constructor.
     */
    protected JkPlugin(JkCommands commands) {
        this.commands = commands;
    }

    @JkDoc("Displays help about this plugin.")
    public void help() {
        HelpDisplayer.helpPlugin(this);
    }

    /**
     * Override this method to modify the commands itself or its bound plugins.
     */
    protected void activate() {
    }

    /**
     * This method is invoked right after plugin options has been injected
     */
    protected void init() {
    }

    public final String name() {
        final String className = this.getClass().getSimpleName();
        if (! className.startsWith(CLASS_PREFIX) || className.equals(CLASS_PREFIX)) {
            throw new IllegalStateException(String.format("Plugin class not properly named. Name should be formatted as " +
                    "'%sXxxx' where xxxx is the name of the plugin (uncapitalized). Was %s.", CLASS_PREFIX, className));
        }
        final String suffix = className.substring(CLASS_PREFIX.length());
        return JkUtilsString.uncapitalize(suffix);
    }

    protected JkCommands getCommands() {
        return commands;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
