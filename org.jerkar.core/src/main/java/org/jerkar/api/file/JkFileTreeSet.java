package org.jerkar.api.file;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;

/**
 * A set ofMany {@link JkFileTree}.
 *
 * @author Jerome Angibaud
 */
public final class JkFileTreeSet {

    private final List<JkFileTree> jkFileTrees;

    private JkFileTreeSet(List<JkFileTree> dirs) {
        if (dirs == null) {
            throw new IllegalArgumentException("dirs can't be null.");
        }
        this.jkFileTrees = Collections.unmodifiableList(dirs);
    }

    /**
     * Creates a {@link JkFileTreeSet} from an iterable ofMany {@link JkFileTree}.
     */
    public static final JkFileTreeSet of(Iterable<JkFileTree> dirs) {
        return new JkFileTreeSet(JkUtilsIterable.listOf(dirs));
    }

    /**
     * Creates an empty {@link JkFileTreeSet}.
     */
    public static final JkFileTreeSet empty() {
        return new JkFileTreeSet(Collections.emptyList());
    }

    /**
     * Creates a {@link JkFileTreeSet} to an array of {@link JkFileTree}.
     */
    public static final JkFileTreeSet of(JkFileTree... trees) {
        return new JkFileTreeSet(Arrays.asList(trees));
    }

    /**
     * Creates a {@link JkFileTreeSet} to an array ofMany folder.
     */
    public static final JkFileTreeSet of(Path... folders) {
        final List<JkFileTree> dirs = new ArrayList<>(folders.length);
        for (final Path folder : folders) {
            dirs.add(JkFileTree.of(folder));
        }
        return new JkFileTreeSet(dirs);
    }

    // -------------------------- additional elements in set ----------------------------------

    /**
     * Creates a {@link JkFileTreeSet} which is a concatenation ofMany this
     * {@link JkFileTreeSet} and the {@link JkFileTree} array passed as
     * parameter.
     */
    public final JkFileTreeSet and(JkFileTree... trees) {
        final List<JkFileTree> list = new LinkedList<>(this.jkFileTrees);
        list.addAll(Arrays.asList(trees));
        return new JkFileTreeSet(list);
    }

    /**
     * Creates a {@link JkFileTreeSet} which is a concatenation ofMany this
     * {@link JkFileTreeSet} and zip files passed as parameter.
     */
    public final JkFileTreeSet andZips(Iterable<Path> zipFiles) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(zipFiles);
        final List<JkFileTree> list = new LinkedList<>(this.jkFileTrees);
        paths.forEach(zipFile -> list.add(JkFileTree.ofZip(zipFile)));
        return new JkFileTreeSet(list);
    }

    /**
     * @see #andZips(Iterable)
     */
    public final JkFileTreeSet andZip(Path... zips) {
        return andZips(Arrays.asList(zips));
    }

    /**
     * Creates a {@link JkFileTreeSet} which is a concatenation ofMany this
     * {@link JkFileTreeSet} and the folder array passed as parameter.
     */
    public final JkFileTreeSet and(Path... folders) {
        final List<JkFileTree> dirs = new ArrayList<>(folders.length);
        for (final Path folder : folders) {
            dirs.add(JkFileTree.of(folder));
        }
        return this.and(dirs.toArray(new JkFileTree[folders.length]));
    }

    /**
     * Creates a {@link JkFileTreeSet} which is a concatenation ofMany this
     * {@link JkFileTreeSet} and the {@link JkFileTreeSet} array passed as
     * parameter.
     */
    public final JkFileTreeSet and(JkFileTreeSet... otherDirSets) {
        final List<JkFileTree> list = new LinkedList<>(this.jkFileTrees);
        for (final JkFileTreeSet otherDirSet : otherDirSets) {
            list.addAll(otherDirSet.jkFileTrees);
        }
        return new JkFileTreeSet(list);
    }

    // ------------------------ additional filters -------------------------------------------

    /**
     * Creates a {@link JkFileTree} which is a copy of this {@link JkFileTree}
     * augmented with the specified {@link JkPathFilter}
     */
    public JkFileTreeSet andFilter(JkPathFilter filter) {
        final List<JkFileTree> list = new LinkedList<>();
        for (final JkFileTree tree : this.jkFileTrees) {
            list.add(tree.andFilter(filter));
        }
        return new JkFileTreeSet(list);
    }

    /**
     * Creates a {@link JkFileTree} which is a copy of this {@link JkFileTree}
     * augmented with the specified {@link JkPathMatcher}
     */
    public JkFileTreeSet andFilter(PathMatcher matcher) {
        final List<JkFileTree> list = new LinkedList<>();
        for (final JkFileTree tree : this.jkFileTrees) {
            list.add(tree.andMatcher(matcher));
        }
        return new JkFileTreeSet(list);
    }

    // ---------------------------- iterate over files -----------------------------------

    /**
     * Returns a concatenation ofMany {@link #files()} for all tree involved in this set.
     */
    public List<Path> files() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkFileTree dirView : this.jkFileTrees) {
            if (dirView.exists()) {
                result.addAll(dirView.files());
            }
        }
        return result;
    }

    /**
     * Returns a concatenation ofMany {@link #relativeFiles()} ()} for all tree involved in this set.
     */
    public List<Path> relativeFiles() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkFileTree dir : this.jkFileTrees) {
            if (dir.exists()) {
                result.addAll(dir.relativeFiles());
            }
        }
        return result;
    }

    // ----------------------- write out ---------------------------------------------

    public JkFileTreeSet zipTo(Path dir) {
        this.jkFileTrees.forEach(tree -> tree.zipTo(dir));
        return this;
    }


    // -------------------------------------- iterates over trees ----------------------

    /**
     * Returns {@link JkFileTree} instances constituting this {@link JkFileTreeSet}.
     */
    public List<JkFileTree> fileTrees() {
        return jkFileTrees;
    }

    /**
     * Returns asScopedDependency ofMany each {@link JkFileTree} instances constituting this
     * {@link JkFileTreeSet}.
     */
    public List<Path> rootFiles() {
        final List<Path> result = new LinkedList<>();
        for (final JkFileTree tree : jkFileTrees) {
            result.add(tree.rootDirOrZipFile());
        }
        return result;
    }

    // ------------------ Misc -----------------------------------------

    /**
     * Returns <code>true</code> if no tree ofMany this set has an existing baseTree.
     */
    public boolean hasNoExistingRoot() {
        for (final JkFileTree tree : jkFileTrees) {
            if (tree.exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * See {@link JkFileTree#count(int, boolean)}
     */
    public int count(int max, boolean includeFolder) {
        int result = 0;
        for (final JkFileTree dirView : jkFileTrees) {
            result += dirView.count(max - result, includeFolder);
        }
        return result;
    }

    public JkFileTreeSet resolve(Path path) {
        List<JkFileTree> list = new LinkedList<>();
        for (JkFileTree tree : jkFileTrees) {
            list.add(tree.resolve(path));
        }
        return new JkFileTreeSet(list);
    }


    @Override
    public String toString() {
        return this.jkFileTrees.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((jkFileTrees == null) ? 0 : jkFileTrees.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JkFileTreeSet other = (JkFileTreeSet) obj;
        if (jkFileTrees == null) {
            if (other.jkFileTrees != null) {
                return false;
            }
        } else if (!jkFileTrees.equals(other.jkFileTrees)) {
            return false;
        }
        return true;
    }

}
