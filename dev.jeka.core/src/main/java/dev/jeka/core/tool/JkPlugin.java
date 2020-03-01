package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Plugin instances are owned by a <code>JkCommandSet</code> instance. The relationship is bidirectional :
 * <code>JkCommandSet</code> instances can invoke plugin methods and vice-versa.<p>
 *
 * Therefore plugins can interact with (or load) other plugins from the owning <code>JkCommandSet</code> instance
 * (which is a quite common pattern).
 */
public abstract class JkPlugin {

    private static final String CLASS_PREFIX = JkPlugin.class.getSimpleName();

    private final JkCommandSet commandSet;

    /*
     * Plugin instances are likely to be configured by the owning <code>JkCommandSet</code> instance, before options
     * are injected.
     * If a plugin needs to initialize state before options are injected, you have to do it in the
     * constructor.
     */
    protected JkPlugin(JkCommandSet commandSet) {
        this.commandSet = commandSet;
        if (getLowestJekaCompatibleVersion() != null && JkInfo.getJekaVersion() != null) {
            JkVersion runningJekaVersion = JkVersion.of(JkInfo.getJekaVersion());
            JkVersion minVersion = JkVersion.of(getLowestJekaCompatibleVersion());
            if (runningJekaVersion.isSnapshot()) {
                JkLog.warn("You are not running a release Jeka version. Plugin " + this +
                        " which is compatible with Jeka version " + minVersion + " or greater may not work properly.");
            } else if (runningJekaVersion.compareTo(minVersion) < 0) {
                JkLog.warn("You are running Jeka version " + runningJekaVersion + " but " + this
                        + " plugin is supposed to work with " + minVersion + " or higher. It may not work properly." );
            }
        }
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
     * Returns a the lowest Jeka version which is compatible with this plugin. If not <code>null</code>  and
     * running Jeka version is lower then a warning log will be emitted.
     */
    protected String getLowestJekaCompatibleVersion() {
        return null;
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

    protected JkCommandSet getCommandSet() {
        return commandSet;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
