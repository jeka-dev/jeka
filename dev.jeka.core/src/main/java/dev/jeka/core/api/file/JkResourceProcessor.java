package dev.jeka.core.api.file;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

//import java.io.File;

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
public final class JkResourceProcessor {

    private JkPathTreeSet resourceTrees;

    private final List<JkInterpolator> interpolators;

    // Charset for interpolation
    private Charset interpolationCharset = Charset.forName("UTF-8");

    private JkResourceProcessor(JkPathTreeSet trees, List<JkInterpolator> interpolators) {
        super();
        this.resourceTrees = trees;
        this.interpolators = interpolators;
    }

    /**
     * Creates a <code>JkResourceProcessor</code> jump the given
     * <code>JkPathTreeSet</code> without processing any token replacement.
     */
    @SuppressWarnings("unchecked")
    public static JkResourceProcessor of(JkPathTreeSet trees) {
        return new JkResourceProcessor(trees, Collections.emptyList());
    }

    /**
     * Creates a <code>JkResourceProcessor</code> jump the given
     * <code>JkPathTree</code> without processing any token replacement.
     */
    public static JkResourceProcessor of(JkPathTree tree) {
        return of(tree.toSet());
    }

    /**
     * Actually processes the resources, meaning copies the getResources to the
     * specified output directory along replacing specified tokens.
     */
    public void generateTo(Path outputDir) {
        JkLog.startTask("Coping resource files to " + outputDir);
        final AtomicInteger count = new AtomicInteger(0);
        for (final JkPathTree resourceTree : this.resourceTrees.getPathTrees()) {
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
        }
        JkLog.info(count.intValue() + " file(s) copied.");
        JkLog.endTask();
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
    public JkResourceProcessor setInterpolationCharset(Charset interpolationCharset) {
        JkUtilsAssert.notNull(interpolationCharset, "interpolation charset cannot be null.");
        this.interpolationCharset = interpolationCharset;
        return this;
    }

    /**
     * Adds the specified resources to this resource processor.
     */
    public JkResourceProcessor addResources(JkPathTreeSet trees) {
        this.resourceTrees = this.resourceTrees.and(trees);
        return this;
    }

    /**
     * @see JkResourceProcessor#addResources(JkPathTreeSet)
     */
    public JkResourceProcessor addResources(JkPathTree tree) {
        return addResources(tree.toSet());
    }

    /**
     * @see JkResourceProcessor#addResources(JkPathTree)
     */
    public JkResourceProcessor addResources(Path dir) {
        return addResources(JkPathTree.of(dir));
    }

    /**
     * Adds specified interpolators to this resource processor.
     */
    public JkResourceProcessor addInterpolators(Iterable<JkInterpolator> interpolators) {
        JkUtilsIterable.addAllWithoutDuplicate(this.interpolators, interpolators);
        return this;
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor addInterpolators(JkInterpolator ... interpolators) {
        return addInterpolators(Arrays.asList(interpolators));
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor addInterpolator(PathMatcher pathMatcher, Map<String, String> keyValues) {
        return addInterpolators(JkInterpolator.of(pathMatcher, keyValues));
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor addInterpolator(String acceptPattern, Map<String, String> keyValues) {
        return addInterpolator(JkPathMatcher.of(true, acceptPattern), keyValues);
    }

    /**
     * @see #addInterpolators(Iterable)
     */
    public JkResourceProcessor addInterpolator(String acceptPattern, String... keyValues) {
        return addInterpolator(acceptPattern, JkUtilsIterable.mapOfAny(keyValues));
    }

    /**
     * Defines values to be interpolated (replacing key by their
     * value), and the file filter to apply it. Keys are generally formatted as <code>${keyName}</code>
     * but can be of any form.
     */
    public static class JkInterpolator {

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
