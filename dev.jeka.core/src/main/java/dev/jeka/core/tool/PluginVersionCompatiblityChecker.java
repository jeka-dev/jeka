package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class PluginCompatibilityBreakChecker {

    private PluginCompatibilityBreakChecker() {}

    static void checkCompatibility(Class pluginClass, JkDependencyResolver resolver) {
        JkManifest manifest = JkManifest.of().loadFromClass(pluginClass);
        String breakingChangeUrl = manifest.getMainAttribute(JkPlugin.MANIFEST_BREAKING_CHANGE_URL_ENTRY);
        String lowestJekaCompatibleVersion = manifest.getMainAttribute(
                JkPlugin.MANIFEST_LOWEST_JEKA_COMPATIBLE_VERSION_ENTRY);

        // Check Jeka version is not too low
        JkVersion jekaVersion = JkVersion.of(JkInfo.getJekaVersion());
        if (lowestJekaCompatibleVersion != null && !jekaVersion.isUnspecified()) {
            JkVersion minVersion = JkVersion.of(lowestJekaCompatibleVersion);
            if (jekaVersion.isSnapshot()) {
                JkLog.warn("You are not running a release Jeka version. Plugin " + pluginClass +
                        " which is compatible with Jeka version " + minVersion + " or greater may not work properly.");
            } else if (jekaVersion.compareTo(minVersion) < 0) {
                JkLog.warn("You are running Jeka version " + jekaVersion + " but " + pluginClass
                        + " plugin is supposed to work with " + minVersion + " or higher. It may not work properly.");
                printUpperJekaVersion(resolver, minVersion);
            }
        }

        // Check Jeka version is not too high
        if (breakingChangeUrl == null) {
            return;
        }
        JkVersion pluginVersion = JkVersion.of(
                JkManifest.of().loadFromClass(pluginClass)
                        .getMainAttribute(JkManifest.IMPLEMENTATION_VERSION));

        // -- check only if both version are not SNAPSHOT
        if (pluginVersion.isSnapshot() || jekaVersion.isSnapshot()) {
            return;
        }
        PluginAndJekaVersion effectiveVersions = new PluginAndJekaVersion(pluginVersion, jekaVersion);
        CompatibilityCache cache = new CompatibilityCache(pluginClass);
        PluginAndJekaVersion cachedBreakingVersion = cache.findBreakingVersion(effectiveVersions);

        // -- Not in cache, read from url then put in cache
        if (cachedBreakingVersion == null) {
            CompatibilityBreak compatibilityBreak;
            try {
                compatibilityBreak = CompatibilityBreak.of(breakingChangeUrl);
            } catch (UncheckedIOException e) {
                JkLog.warn("Unable to access " + breakingChangeUrl + ". No version compatibility won't be checked.");
                return;
            }
            PluginAndJekaVersion remoteBreakingVersion
                    = compatibilityBreak.getBreakingJekaVersion(effectiveVersions);
            if (remoteBreakingVersion != null) {
                logJekaBreakingVersion(effectiveVersions, remoteBreakingVersion, pluginClass);
            }
            cache.addEntry(effectiveVersions, remoteBreakingVersion);

            // -- Present in cache and cache says the two versions are not compatible
        } else if (cachedBreakingVersion != PluginAndJekaVersion.EMPTY) {
            logJekaBreakingVersion(effectiveVersions, cachedBreakingVersion, pluginClass);
        }
    }

    private static void logJekaBreakingVersion(PluginAndJekaVersion effectiveVersions,
                                               PluginAndJekaVersion breakingVersion, Class pluginClass) {
        JkLog.error("You are running Jeka version " + effectiveVersions.jekaVersion + " but plugin "
                + pluginClass.getName() + " version " + effectiveVersions.pluginVersion
                + " does not work with jeka version " + breakingVersion.jekaVersion + " or higher.");
        JkLog.error("Please use a Jeka version lower than " + breakingVersion.jekaVersion + " or a plugin version higher than "
                + breakingVersion.pluginVersion);
    }

    private static class CompatibilityCache {

        private final Path cachePath;

        CompatibilityCache(Class<JkPlugin> pluginClass) {
            cachePath = JkLocator.getJekaHomeDir().resolve("plugins-compatibility")
                    .resolve(pluginClass.getName() + "-compatibility.txt");
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
                    if (items.length > 3) {
                        return new PluginAndJekaVersion(items[2].trim(), items[3].trim());
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
                throw new UncheckedIOException(e);
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
                if (result == null || entry.getValue().compareTo(result.getValue()) < 0) {
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

    private static void printUpperJekaVersion(JkDependencyResolver dependencyResolver, JkVersion minVersion) {
        JkLog.warn("Jeka version upper or equals to " + minVersion + " are ; ");
        List<String> versions = dependencyResolver.searchVersions(JkModuleId.of("dev.jeka:jeka-core"));
        versions.stream()
                .map(version -> JkVersion.of(version))
                .filter(version -> version.compareTo(minVersion) >= 0)
                .map(Objects::toString)
                .forEach(JkLog::warn);
    }

}
