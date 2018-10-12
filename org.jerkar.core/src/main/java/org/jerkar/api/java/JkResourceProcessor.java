package org.jerkar.api.java;

import org.jerkar.api.file.JkPathFile;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;

//import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final JkPathTreeSet resourceTrees;

    private final Collection<JkInterpolator> interpolators;

    private JkResourceProcessor(JkPathTreeSet trees, Collection<JkInterpolator> interpolators) {
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
        return new JkResourceProcessor(trees, Collections.EMPTY_LIST);
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
    public void generateTo(Path outputDir, Charset charset) {
        JkLog.execute("Coping resource files to " + outputDir, () -> {
            final AtomicInteger count = new AtomicInteger(0);
            for (final JkPathTree resourceTree : this.resourceTrees.getPathTrees()) {
                if (!resourceTree.exists()) {
                    continue;
                }
                resourceTree.stream().forEach(path -> {
                    final Path relativePath = resourceTree.getRoot().relativize(path);
                    final Path out = outputDir.resolve(relativePath);
                    final Map<String, String> data = JkInterpolator.interpolateData(relativePath.toString(),
                            interpolators);
                    if (Files.isDirectory(path)) {
                        JkUtilsPath.createDirectories(out);
                    } else {
                        JkPathFile.of(path).copyReplacingTokens(out, data, charset);
                        count.incrementAndGet();
                    }
                });
            }
            JkLog.info(count.intValue() + " file(s) copied.");
        });


    }

    /**
     * @see JkResourceProcessor#and(JkPathTreeSet)
     */
    public JkResourceProcessor and(JkPathTreeSet trees) {
        return new JkResourceProcessor(this.resourceTrees.and(trees), this.interpolators);
    }

    /**
     * @see JkResourceProcessor#and(JkPathTreeSet)
     */
    public JkResourceProcessor and(JkPathTree tree) {
        return and(tree.toSet());
    }

    /**
     * @see JkResourceProcessor#and(JkPathTree)
     */
    public JkResourceProcessor and(Path dir) {
        return and(JkPathTree.of(dir));
    }



    /**
     * Creates a <code>JkResourceProcessor</code> identical at this one but
     * adding the specified interpolator.
     */
    public JkResourceProcessor and(JkInterpolator interpolator) {
        final List<JkInterpolator> list = new LinkedList<>(this.interpolators);
        list.add(interpolator);
        return new JkResourceProcessor(this.resourceTrees, list);
    }

    /**
     * Creates a <code>JkResourceProcessor</code> identical at this one but
     * adding the specified interpolator.
     */
    public JkResourceProcessor and(Iterable<JkInterpolator> interpolators) {
        final List<JkInterpolator> list = new LinkedList<>(this.interpolators);
        JkUtilsIterable.addAllWithoutDuplicate(list, interpolators);
        return new JkResourceProcessor(this.resourceTrees, list);
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

        /**
         * Returns a copy of this {@link JkInterpolator} but adding key values to interpolate.
         */
        public JkInterpolator and(String key, String value, String... others) {
            final Map<String, String> map = JkUtilsIterable.mapOf(key, value, (Object[]) others);
            map.putAll(keyValues);
            return new JkInterpolator(this.matcher, map);
        }

        private static Map<String, String> interpolateData(String path,
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
