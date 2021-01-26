package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        checkCompatibility();;
    }

    @JkDoc("Displays help about this plugin.")
    public void help() {
        HelpDisplayer.helpPlugin(this);
    }

    /**
     * Override this method to initialize the plugin.
     * This method is invoked right after plugin option fields has been injected and prior
     * {@link JkCommandSet#setup()} is invoked.
     */
    protected void beforeSetup() throws Exception {
    }

    /**
     * Override this method to perform some actions, once the plugin has been setup by
     * {@link JkCommandSet#setup()} method.<p>
     * Typically, some plugins have to configure other ones (For instance, <i>java</i> plugin configures
     * <i>scaffold</i> plugin to instruct what to use as a template build class). Those kind of
     * configuration is better done here as the setup made in {@link JkCommandSet} is likely
     * to impact the result of the configuration.
     */
    protected void afterSetup() throws Exception {
    }

    /**
     * Returns a the lowest Jeka version which is compatible with this plugin. If not <code>null</code>  and
     * running Jeka version is lower then a warning log will be emitted.
     */
    protected String getLowestJekaCompatibleVersion() {
        return null;
    }

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
    protected String getBreakingVersionRegistryUrl() {
        return null;
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

    private void checkCompatibility() {

        // Check Jeka version is not too low
        JkVersion jekaVersion = JkVersion.of(JkInfo.getJekaVersion());
        if (getLowestJekaCompatibleVersion() != null && !jekaVersion.isUnspecified()) {
            JkVersion minVersion = JkVersion.of(getLowestJekaCompatibleVersion());
            if (jekaVersion.isSnapshot()) {
                JkLog.warn("You are not running a release Jeka version. Plugin " + this +
                        " which is compatible with Jeka version " + minVersion + " or greater may not work properly.");
            } else if (jekaVersion.compareTo(minVersion) < 0) {
                JkLog.warn("You are running Jeka version " + jekaVersion + " but " + this
                        + " plugin is supposed to work with " + minVersion + " or higher. It may not work properly.");
            }
        }

        // Check Jeka version is not too high
        if (this.getBreakingVersionRegistryUrl() == null) {
            return;
        }
        JkVersion pluginVersion = JkVersion.of(
                JkManifest.of().setManifestFromClass(this.getClass())
                        .getMainAttribute(JkManifest.IMPLEMENTATION_VERSION));

        // -- check only if both version are not SNAPSHOT
        if (pluginVersion.isSnapshot() || jekaVersion.isSnapshot()) {
            return;
        }
        PluginAndJekaVersion effectiveVersions = new PluginAndJekaVersion(pluginVersion, jekaVersion);
        CompatibilityCache cache = new CompatibilityCache(this);
        PluginAndJekaVersion cachedBreakingVersion = cache.findBreakingVersion(effectiveVersions);

        // -- Not in cache, read from url then put in cache
        if (cachedBreakingVersion == null) {
            CompatibilityBreak compatibilityBreak = CompatibilityBreak.of(this.getBreakingVersionRegistryUrl());
            PluginAndJekaVersion remoteBreakingVersion
                    = compatibilityBreak.getBreakingJekaVersion(effectiveVersions);
            if (remoteBreakingVersion != null) {
                logJekaBreakingVersion(effectiveVersions, remoteBreakingVersion);
            }
            cache.addEntry(effectiveVersions, remoteBreakingVersion);

        // -- Present in cache and cache says the two versions are not compatible
        } else if (cachedBreakingVersion != PluginAndJekaVersion.EMPTY) {
            logJekaBreakingVersion(effectiveVersions, cachedBreakingVersion);
        }
    }

    private void logJekaBreakingVersion(PluginAndJekaVersion effectiveVersions, PluginAndJekaVersion breakingVersion ) {
        JkLog.error("You are running Jeka version " + effectiveVersions.jekaVersion + " but plugin "
                + this.getClass().getName() + " version " + effectiveVersions.pluginVersion
                + " does not work with jeka version " + breakingVersion.jekaVersion + " or higher.");
        JkLog.error("Please use a Jeka version lower than " + breakingVersion.jekaVersion + " or a plugin version higher than "
                + breakingVersion.pluginVersion);
    }

    private static class CompatibilityCache {

        private final Path cachePath;

        CompatibilityCache(JkPlugin plugin) {
            cachePath = JkLocator.getJekaHomeDir().resolve("plugins-compatibility")
                    .resolve(this.getClass().getName() + "-compatibility.txt");
        }

        boolean exist() {
            return Files.exists(cachePath);
        }

        // Returns null if no such entry found. Return EMPTY if entry found but no breaking version detected
        PluginAndJekaVersion findBreakingVersion(PluginAndJekaVersion effectiveVersions) {
            if (!exist()) {
                return null;
            }
            List<String> lines = JkUtilsPath.readAllLines(cachePath);
            for (String line : lines) {
                String[] items = line.split(":");
                if (items.length <=2) {
                    continue;
                }
                PluginAndJekaVersion cachedBreakingVersion = new PluginAndJekaVersion(items[0].trim(), items[1].trim());

                // found in cache
                if (cachedBreakingVersion.equals(effectiveVersions)) {
                    if (items.length > 2) {
                        return new PluginAndJekaVersion(items[3].trim(), items[4].trim());
                    }
                    return PluginAndJekaVersion.EMPTY; // already searched and that was ok
                }
            }
            return null;
        }

        void addEntry(PluginAndJekaVersion effectiveVersions, PluginAndJekaVersion breakingVersions) {
            if (!exist()) {
                JkUtilsPath.createFileSafely(cachePath);
            }
            String line = effectiveVersions.pluginVersion + " : " + effectiveVersions.jekaVersion;
            if (breakingVersions != null) {
                line = line + " : " + breakingVersions.pluginVersion + " : " + breakingVersions.jekaVersion + "\n";
            }
            JkUtilsPath.write(cachePath, line.getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND);
        }
    }

    static class CompatibilityBreak {

        private final Map<JkVersion, JkVersion> versionMap;

        private CompatibilityBreak(Map<JkVersion, JkVersion> versionMap) {
            this.versionMap = versionMap;
        }

        static CompatibilityBreak of(String url) {
            try (InputStream is = JkUtilsIO.inputStream(new URL(url))){
                return of(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        static CompatibilityBreak of(InputStream inputStream) {
            List<String> lines = JkUtilsIO.readAsLines(inputStream);
            Map<JkVersion, JkVersion> versionMap = new LinkedHashMap<>();
            for (String line : lines) {
                String[] items = line.split(":");
                if (items.length != 2) {
                    continue;
                }
                JkVersion pluginVersion = JkVersion.of(items[0].trim());
                JkVersion jekaVersion = JkVersion.of(items[1].trim());
                versionMap.put(pluginVersion, jekaVersion);
            }
            return new CompatibilityBreak(versionMap);
        }

        PluginAndJekaVersion getBreakingJekaVersion(PluginAndJekaVersion effectiveVersions) {
            Map.Entry<JkVersion, JkVersion> result = null;
            for (Map.Entry<JkVersion, JkVersion> entry : this.versionMap.entrySet()) {
                if (effectiveVersions.pluginVersion.compareTo(entry.getKey()) > 0) {
                    continue;
                }
                if (effectiveVersions.jekaVersion.compareTo(entry.getValue()) < 0) {
                    continue;
                }
                if (result == null) {
                    result = entry;
                } else if (entry.getValue().compareTo(result.getValue()) < 0) {
                    result = entry;
                }
            }
            return result == null ? null : new PluginAndJekaVersion(result.getKey(), result.getValue());
        }
    }

    static class PluginAndJekaVersion {

        static final PluginAndJekaVersion EMPTY = new PluginAndJekaVersion((JkVersion) null, null);

        final JkVersion pluginVersion;

        final JkVersion jekaVersion;

        PluginAndJekaVersion(JkVersion pluginVersion, JkVersion jekaVersion) {
            this.pluginVersion = pluginVersion;
            this.jekaVersion = jekaVersion;
        }

        PluginAndJekaVersion(String pluginVersion, String jekaVersion) {
            this(JkVersion.of(pluginVersion), JkVersion.of(jekaVersion));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PluginAndJekaVersion that = (PluginAndJekaVersion) o;
            if (pluginVersion != null ? !pluginVersion.equals(that.pluginVersion) : that.pluginVersion != null)
                return false;
            return jekaVersion != null ? jekaVersion.equals(that.jekaVersion) : that.jekaVersion == null;
        }

        @Override
        public int hashCode() {
            int result = pluginVersion != null ? pluginVersion.hashCode() : 0;
            result = 31 * result + (jekaVersion != null ? jekaVersion.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "PluginAndJekaVersion{" +
                    "pluginVersion=" + pluginVersion +
                    ", jekaVersion=" + jekaVersion +
                    '}';
        }
    }

}
