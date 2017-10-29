package org.jerkar.api.java;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;

/**
 * This processor basically copies some resource files to a target folder
 * (generally the class folder). It can also proceed to token replacement, i.e
 * replacing strings between <code>${</code> and <code>}</code> by a specified
 * values.<br/>
 * The processor is constructed using a list ofMany <code>JkDirSets</code> and for
 * each ofMany them, we can associate a map ofMany token to replace.<br/>
 *
 * @author Jerome Angibaud
 */
public final class JkResourceProcessor {

    private final JkFileTreeSet resourceTrees;

    private final Collection<JkInterpolator> interpolators;

    private JkResourceProcessor(JkFileTreeSet trees, Collection<JkInterpolator> interpolators) {
        super();
        this.resourceTrees = trees;
        this.interpolators = interpolators;
    }

    /**
     * Creates a <code>JkResourceProcessor</code> jump the given
     * <code>JkFileTreeSet</code> without processing any token replacement.
     */
    @SuppressWarnings("unchecked")
    public static JkResourceProcessor of(JkFileTreeSet trees) {
        return new JkResourceProcessor(trees, Collections.EMPTY_LIST);
    }

    /**
     * Creates a <code>JkResourceProcessor</code> jump the given
     * <code>JkFileTree</code> without processing any token replacement.
     */
    public static JkResourceProcessor of(JkFileTree tree) {
        return of(tree.asSet());
    }

    /**
     * Actually processes the resources, meaning copies the resources to the
     * specified output directory along replacing specified tokens.
     */
    public void generateTo(File outputDir) {
        JkLog.startln("Coping resource files to " + outputDir.getPath());
        final AtomicInteger count = new AtomicInteger(0);
        for (final JkFileTree resourceTree : this.resourceTrees.fileTrees()) {
            if (!resourceTree.exists()) {
                continue;
            }
            resourceTree.stream().forEach(path -> {
                final Path relativePath = resourceTree.root().relativize(path);
                final Path out = outputDir.toPath().resolve(relativePath);
                final Map<String, String> data = JkInterpolator.interpolateData(relativePath.toString(),
                        interpolators);
                if (Files.isDirectory(path)) {
                    JkUtilsPath.createDirectories(out);
                } else {
                    JkUtilsFile.copyFileReplacingTokens(path.toFile(), out.toFile(), data, JkLog.infoStreamIfVerbose());
                    count.incrementAndGet();
                }
            });
        }
        JkLog.done(count.intValue() + " file(s) copied.");
    }

    /**
     * @see JkResourceProcessor#and(JkFileTreeSet)
     */
    public JkResourceProcessor and(JkFileTreeSet trees) {
        return new JkResourceProcessor(this.resourceTrees.and(trees), this.interpolators);
    }

    /**
     * @see JkResourceProcessor#and(JkFileTreeSet)
     */
    public JkResourceProcessor and(JkFileTree tree) {
        return and(tree.asSet());
    }

    /**
     * @see JkResourceProcessor#and(JkFileTree)
     */
    public JkResourceProcessor and(File dir) {
        return and(JkFileTree.of(dir));
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
     * Defines values to be interpolated (replacing <code>${key}</code> by their
     * value), and the file filter to apply it.
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
         * Returns a copy ofMany this {@link JkInterpolator} but adding key values
         * to interpolate
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
