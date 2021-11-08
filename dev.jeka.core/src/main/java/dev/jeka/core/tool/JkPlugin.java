package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Plugin instances are owned by a <code>JkClass</code> instance. The relationship is bidirectional :
 * <code>JkClass</code> instances can invoke plugin methods and vice-versa.<p>
 *
 * Therefore, plugins can interact with (or load) other plugins from the owning <code>JkClass</code> instance
 * (which is a quite common pattern).
 */
public abstract class JkPlugin {

    /**
     * When publishing a plugin, authors can not guess which future version of Jeka will break compatibility.
     * To keep track of breaking change, a registry can be maintained to be accessible at the specified url.
     * <p>
     * The register is expected to be a simple flat file.
     * Each row is structured as <code>pluginVersion : jekaVersion</code>.
     * <p>
     * The example below means that :<ul>
     *     <li>Every plugin version equal or lower than 1.2.1.RELEASE is incompatible with any version of jeka equals or greater than 0.9.1.RELEASE</li>
     *     <li>Every plugin version equal or lower than 1.3.0.RELEASE is incompatible with any version of jeka equals or greater than 0.9.5.M1</li>
     * </ul>
     *
     * <pre><code>
     *     1.2.1.RELEASE : 0.9.1.RELEASE
     *     1.3.0.RELEASE : 0.9.5.M1
     * </code></pre>
     */
    public static final String MANIFEST_BREAKING_CHANGE_URL_ENTRY = "Jeka-Breaking-Change-Url";

    /**
     * Manifest entry containing the lowest Jeka version which is compatible with a plugin. If value not <code>null</code>  and
     * running Jeka version is lower then a warning log will be emitted.
     */
    public static final String MANIFEST_LOWEST_JEKA_COMPATIBLE_VERSION_ENTRY = "Jeka-Lowest-Compatible-Version";

    private static final String CLASS_PREFIX = JkPlugin.class.getSimpleName();

    private final JkClass jkClass;

    /*
     * Plugin instances are likely to be configured by the owning <code>JkClass</code> instance, before options
     * are injected.
     * If a plugin needs to initialize state before options are injected, you have to do it in the
     * constructor.
     */
    protected JkPlugin(JkClass jkClass) {
        this.jkClass = jkClass;
    }

    @JkDoc("Displays help about this plugin.")
    public void help() {
        HelpDisplayer.helpPlugin(this);
    }

    /**
     * Override this method to initialize the plugin.
     * This method is invoked right after plugin option fields has been injected and prior
     * {@link JkClass#setup()} is invoked.
     */
    protected void beforeSetup() throws Exception {
    }

    /**
     * Override this method to perform some actions, once the plugin has been setup by
     * {@link JkClass#setup()} method.<p>
     * Typically, some plugins have to configure other ones (For instance, <i>java</i> plugin configures
     * <i>scaffold</i> plugin to instruct what to use as a template build class). Those kind of
     * configuration is better done here as the setup made in {@link JkClass} is likely
     * to impact the result of the configuration.
     */
    protected void afterSetup() throws Exception {
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

    public JkClass getJkClass() {
        return jkClass;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }

    /**
     * Convenient method to set a Jeka Plugin compatibility range with Jeka versions.
     * @param lowestVersion Can be null
     * @param breakingChangeUrl Can be null
     * @see JkPlugin#MANIFEST_LOWEST_JEKA_COMPATIBLE_VERSION_ENTRY
     * @See JkPlugin#MANIFEST_BREAKING_CHANGE_URL_ENTRY
     */
    public static void setJekaPluginCompatibilityRange(JkManifest manifest, String lowestVersion, String breakingChangeUrl) {
        manifest
                .addMainAttribute(JkPlugin.MANIFEST_LOWEST_JEKA_COMPATIBLE_VERSION_ENTRY, lowestVersion)
                .addMainAttribute(JkPlugin.MANIFEST_BREAKING_CHANGE_URL_ENTRY, breakingChangeUrl);
    }



}
