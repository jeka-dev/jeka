package dev.jeka.core.api.file;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This processor basically copies some resource files to a target folder
 * (generally the class folder). It can also proceed to token replacement, i.e
 * replacing strings between <code>${</code> and <code>}</code> by a specified
 * values.<br/>
 * The processor is constructed from a <code>{@link JkPathTreeSet}</code> and for
 * each of them, we can associate a token map for interpolation.<br/>
 *
 * @author Jerome Angibaud
 */
public final class JkResourceProcessor<T> {

    private final List<JkInterpolator> interpolators = new LinkedList<>();

    // Charset for interpolation
    private Charset interpolationCharset = Charset.forName("UTF-8");

    /**
     * For parent chaining
     */
    public final T __;

    private JkResourceProcessor(T parent) {
        this.__ = parent;
    }

    /**
     * Applies the specified consumer to this object.
     */
    public JkResourceProcessor<T> apply(Consumer<JkResourceProcessor> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * Creates an empty resource processor
     */
    public static JkResourceProcessor<Void> of() {
        return ofParent(null);
    }

    /**
     * Same as {@link #of()} with providing a parent chaining
     */
    public static <T> JkResourceProcessor<T> ofParent(T parent) {
        return new JkResourceProcessor<>(parent);
    }

    /**
     * Adds specified interpolators to this resource processor.
     */
    public JkResourceProcessor<T> addInterpolators(Iterable<JkInterpolator> interpolators) {
        JkUtilsIterable.addAllWithoutDuplicate(this.interpolators, interpolators);
        return this;
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor<T> addInterpolators(JkInterpolator ... interpolators) {
        return addInterpolators(Arrays.asList(interpolators));
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor<T> addInterpolator(PathMatcher pathMatcher, Map<String, String> keyValues) {
        return addInterpolators(JkInterpolator.of(pathMatcher, keyValues));
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor<T> addInterpolator(String acceptPattern, Map<String, String> keyValues) {
        return addInterpolator(JkPathMatcher.of(true, acceptPattern), keyValues);
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor<T> addInterpolator(String acceptPattern, String... keyValues) {
        return addInterpolator(acceptPattern, JkUtilsIterable.mapOfAny(keyValues));
    }

    /**
     * Returns the charset used for interpolation
     */
    public Charset getInterpolationCharset() {
        return interpolationCharset;
    }

    /**
     * Set the charset used for interpolation. This charset is not used if no interpolation occurs.
     */
    public JkResourceProcessor<T> setInterpolationCharset(Charset interpolationCharset) {
        JkUtilsAssert.notNull(interpolationCharset, "interpolation charset cannot be null.");
        this.interpolationCharset = interpolationCharset;
        return this;
    }

    /**
     * Actually processes the resources, meaning copies the getResources to the
     * specified output directory along replacing specified tokens.
     */
    public void generate(JkPathTreeSet resourceTrees, Path outputDir) {
        JkLog.startTask("Copying resource files to %s", outputDir);
        for (final JkPathTree resourceTree : resourceTrees.toList()) {
            final AtomicInteger count = new AtomicInteger(0);
            if (!resourceTree.exists()) {
                continue;
            }
            resourceTree.stream().forEach(path -> {
                final Path relativePath = resourceTree.getRoot().relativize(path);
                final Path out = outputDir.resolve(relativePath);
                final Map<String, String> data = JkInterpolator.of(relativePath.toString(),
                        interpolators);
                if (Files.isDirectory(path)) {
                    JkUtilsPath.createDirectories(out);
                } else {
                    JkPathFile.of(path).copyReplacingTokens(out, data, interpolationCharset);
                    count.incrementAndGet();
                }
            });
            JkLog.info("%s processed from %s.", JkUtilsString.plurialize(count.get(), "file"),
                    Paths.get("").toAbsolutePath().relativize(resourceTree.getRoot()));
        }
        JkLog.endTask();
    }

    /**
     * Defines values to be interpolated (replacing key by their
     * value), and the file filter to apply it. Keys are generally formatted as <code>${keyName}</code>
     * but can be of any form.
     */
    private static class JkInterpolator {

        private final Map<String, String> keyValues;

        private final PathMatcher matcher;

        private JkInterpolator(PathMatcher matcher, Map<String, String> keyValues) {
            super();
            this.keyValues = keyValues;
            this.matcher = matcher;
        }

        public static JkInterpolator of(PathMatcher pathMatcher, Map<String, String> keyValues) {
            return new JkInterpolator(pathMatcher, keyValues);
        }

        public static JkInterpolator of(Map<String, String> keyValues) {
            return of(JkPathMatcher.of(), keyValues);
        }

        public static JkInterpolator of() {
            return of(JkPathMatcher.of(), Collections.emptyMap());
        }

        /**
         * Returns a copy of this {@link JkInterpolator} but adding key values to interpolate.
         */
        public JkInterpolator and(String key, String value, String... others) {
            final Map<String, String> map = JkUtilsIterable.mapOf(key, value, (Object[]) others);
            map.putAll(keyValues);
            return new JkInterpolator(this.matcher, map);
        }

        private static Map<String, String> of(String path,
                                              Iterable<JkInterpolator> interpolators) {
            final Map<String, String> result = new HashMap<>();
            for (final JkInterpolator interpolator : interpolators) {
                if (interpolator.matcher.matches(Paths.get(path))) {
                    result.putAll(interpolator.keyValues);
                }
            }
            return result;
        }

    }

}
