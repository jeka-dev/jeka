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
     * When publishing a plugin, authors can not guess which future version of Jeka will break compatibility.
     * To keep track of breaking change, a register can be maintained and be accessible at the returned url.
     * <p>
     * The register is expected to be a simple flat file.
     * Each row is structured as <code>pluginVersion : jekaVersion</code>.
     * <p>
     * The example below means that :<ul>
     *     <li>Every plugin version equal or greater than 1.2.1.RELEASE is incompatible with any version of jeka equals or greater than 0.9.1.RELEASE</li>
     *     <li>And every plugin version equal or greater than 1.3.0.RELEASE is incompatible with any version of jeka equals or greater than 0.9.5.M1</li>
     * </ul>
     *
     * <pre><code>
     *     1.2.1.RELEASE : 0.9.1.RELEASE
     *     1.3.0.RELEASE : 0.9.5.M1
     * </code></pre>
     */
    protected String getBreakingVersionRegisterUrl() {
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
        if (this.getBreakingVersionRegisterUrl() == null) {
            return;
        }
        JkVersion pluginVersion = JkVersion.of(
                JkManifest.of().setManifestFromClass(this.getClass())
                        .getMainAttribute(JkManifest.IMPLEMENTATION_VERSION));

        // -- check only if both version are not SNAPSHOT
        if (pluginVersion.isSnapshot() || jekaVersion.isSnapshot()) {
            return;
        }
        CompatibilityCache cache = new CompatibilityCache(this);
        String cacheBreakingVersion = cache.breakingVersion(pluginVersion, jekaVersion);

        // -- Not in cache, read from url then put in cache
        if (cacheBreakingVersion == null) {
            CompatibilityBreak compatibilityBreak = CompatibilityBreak.of(this.getBreakingVersionRegisterUrl());
            String urlBreakingVersion = compatibilityBreak.getBreakingJekaVersion(pluginVersion, jekaVersion);
            if (urlBreakingVersion != null) {
                logJekaBreakingVersion(jekaVersion, pluginVersion, urlBreakingVersion);
            }
            cache.addEntry(pluginVersion, jekaVersion, urlBreakingVersion);

        // -- Present in cache and cache says the two versions are not compatible
        } else if (!cacheBreakingVersion.isEmpty()) {
            logJekaBreakingVersion(jekaVersion, pluginVersion, cacheBreakingVersion);
        }
    }


    private void logJekaBreakingVersion(JkVersion jekaVersion, JkVersion pluginVersion, String breakingVersion) {
        JkLog.warn("You are running Jeka version " + jekaVersion + " but plugin " + this.getClass().getName()
                + " version " + pluginVersion + " is know to not work with jeka version " + breakingVersion
                + " higher.");
        JkLog.warn("Please use a Jeka version lower than " + breakingVersion + " or a higher plugin version.");
    }

    private static class CompatibilityCache {

        private final Path cachePath;

        CompatibilityCache(JkPlugin plugin) {
            cachePath = JkLocator.getJekaHomeDir().resolve("cache")
                    .resolve(this.getClass().getName() + "-compatibility.txt");
        }

        boolean exist() {
            return Files.exists(cachePath);
        }

        String breakingVersion(JkVersion pluginVersion, JkVersion jekaVersion) {
            List<String> lines = JkUtilsPath.readAllLines(cachePath);
            for (String line : lines) {
                String[] items = line.split(":");
                if (items.length <=2) {
                    continue;
                }
                JkVersion plugin = JkVersion.of(items[0].trim());
                JkVersion jeka = JkVersion.of(items[1].trim());

                // found in cache
                if (pluginVersion.getValue().equals(plugin) && jekaVersion.getValue().equals(jeka)) {
                    if (items.length > 2) {
                        String breakingVersion = items[3].trim();
                        return breakingVersion;
                    }
                    return "";
                }
            }
            return null;
        }

        void addEntry(JkVersion pluginVersion, JkVersion jekaVersion, String breakingVersion) {
            if (!exist()) {
                JkUtilsPath.createFile(cachePath);
            }
            String line = pluginVersion.getValue() + " : " + jekaVersion;
            if (breakingVersion != null) {
                line = line + " : " + breakingVersion;
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

        String getBreakingJekaVersion(JkVersion pluginVersion, JkVersion jekaVersion) {
            JkVersion result = null;
            for (Map.Entry<JkVersion, JkVersion> entry : this.versionMap.entrySet()) {
                if (pluginVersion.compareTo(entry.getKey()) > 0) {
                    continue;
                }
                if (jekaVersion.compareTo(entry.getValue()) < 0) {
                    continue;
                }
                if (result == null) {
                    result = entry.getValue();
                } else if (entry.getValue().compareTo(result) < 0) {
                    result = entry.getValue();
                }
            }
            return result == null ? null : result.getValue();
        }
    }

}
