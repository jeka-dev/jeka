package dev.jeka.core.api.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Provides a view on files and sub-folders contained in a given directory or zip file. A
 * <code>JkPathTree</code> may have some include/exclude filters to include only
 * or exclude specified files.<br/>
 * When speaking about files contained in a {@link JkPathTree}, we mean all
 * files contained in its root directory or subdirectories, matching positively
 * the filter defined on it.
 *
 * @author Jerome Angibaud
 */
public class JkPathTree extends JkAbstractPathTree<JkPathTree> {

    protected JkPathTree(Path rootDir, JkPathMatcher matcher) {
        super(() -> rootDir, matcher);
    }

    /**
     * Creates a {@link JkPathTree} having the specified root directory.
     */
    public static JkPathTree of(Path rootDir) {
        return new JkPathTree(rootDir, JkPathMatcher.ACCEPT_ALL);
    }

    /**
     * Same as {@link #of(Path) but speciying the root dir with a String.
     * @see #of(Path)
     */
    public static JkPathTree of(String rootDir) {
        return of(Paths.get(rootDir));
    }

    @Override
    protected JkPathTree newInstance(Supplier<Path> pathSupplier, JkPathMatcher pathMatcher) {
        return new JkPathTree(pathSupplier.get(), pathMatcher);
    }

    @Override
    protected JkPathTree withRoot(Path newRoot) {
        return newInstance(() -> newRoot, this.getMatcher());
    }

    /**
     * Returns a {@link JkPathTreeSet} containing this tree as its single
     * element.
     */
    public JkPathTreeSet toSet() {
        return JkPathTreeSet.of(this);
    }


}
