package dev.jeka.core.api.file;

import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;

/**
 * A set of {@link JkPathTree}.
 *
 * @author Jerome Angibaud
 */
public final class JkPathTreeSet {

    private final List<JkPathTree> pathTrees;

    private JkPathTreeSet(List<JkPathTree> dirs) {
        if (dirs == null) {
            throw new IllegalArgumentException("dirs can't be null.");
        }
        this.pathTrees = Collections.unmodifiableList(dirs);
    }

    /**
     * Creates a {@link JkPathTreeSet} from an iterable of {@link JkPathTree}.
     */
    public static final JkPathTreeSet of(Iterable<JkPathTree> dirs) {
        return new JkPathTreeSet(JkUtilsIterable.listOf(dirs));
    }

    /**
     * Creates an empty {@link JkPathTreeSet}.
     */
    public static final JkPathTreeSet ofEmpty() {
        return new JkPathTreeSet(Collections.emptyList());
    }

    /**
     * Creates a {@link JkPathTreeSet} to an array of {@link JkPathTree}.
     */
    public static final JkPathTreeSet of(JkPathTree... trees) {
        return new JkPathTreeSet(Arrays.asList(trees));
    }

    /**
     * Creates a {@link JkPathTreeSet} from an array of folder.
     */
    public static final JkPathTreeSet of(Path... folders) {
        final List<JkPathTree> dirs = new ArrayList<>(folders.length);
        for (final Path folder : folders) {
            dirs.add(JkPathTree.of(folder));
        }
        return new JkPathTreeSet(dirs);
    }

    // -------------------------- additional elements in set ----------------------------------

    /**
     * Creates a {@link JkPathTreeSet} which is a concatenation of this
     * {@link JkPathTreeSet} and the {@link JkPathTree} array passed as
     * parameter.
     */
    public final JkPathTreeSet and(JkPathTree... trees) {
        final List<JkPathTree> list = new LinkedList<>(this.pathTrees);
        list.addAll(Arrays.asList(trees));
        return new JkPathTreeSet(list);
    }

    /**
     * Creates a {@link JkPathTreeSet} which is a concatenation of this
     * {@link JkPathTreeSet} and zip files passed as parameter.
     */
    public final JkPathTreeSet andZips(Iterable<Path> zipFiles) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(zipFiles);
        final List<JkPathTree> list = new LinkedList<>(this.pathTrees);
        paths.forEach(zipFile -> list.add(JkPathTree.ofZip(zipFile)));
        return new JkPathTreeSet(list);
    }

    /**
     * @see #andZips(Iterable)
     */
    public final JkPathTreeSet andZip(Path... zips) {
        return andZips(Arrays.asList(zips));
    }

    /**
     * Creates a {@link JkPathTreeSet} which is a concatenation of this
     * {@link JkPathTreeSet} and the folder array passed as parameter.
     */
    public final JkPathTreeSet and(Path... folders) {
        final List<JkPathTree> dirs = new ArrayList<>(folders.length);
        for (final Path folder : folders) {
            dirs.add(JkPathTree.of(folder));
        }
        return this.and(dirs.toArray(new JkPathTree[folders.length]));
    }

    /**
     * Creates a {@link JkPathTreeSet} which is a concatenation of this
     * {@link JkPathTreeSet} and the {@link JkPathTreeSet} array passed as
     * parameter.
     */
    public final JkPathTreeSet and(JkPathTreeSet... otherDirSets) {
        final List<JkPathTree> list = new LinkedList<>(this.pathTrees);
        for (final JkPathTreeSet otherDirSet : otherDirSets) {
            list.addAll(otherDirSet.pathTrees);
        }
        return new JkPathTreeSet(list);
    }

    // ------------------------ additional filters -------------------------------------------


    public JkPathTreeSet andAccept(Iterable<String> globPatterns) {
        final List<JkPathTree> list = new LinkedList<>();
        for (final JkPathTree tree : this.pathTrees) {
            list.add(tree.andMatching(true, globPatterns));
        }
        return new JkPathTreeSet(list);
    }

    /**
     * Creates a {@link JkPathTree} which is a copy of this {@link JkPathTree}
     * augmented with the specified {@link JkPathMatcher}
     */
    public JkPathTreeSet andMatcher(PathMatcher matcher) {
        final List<JkPathTree> list = new LinkedList<>();
        for (final JkPathTree tree : this.pathTrees) {
            list.add(tree.andMatcher(matcher));
        }
        return new JkPathTreeSet(list);
    }

    // ------------------------- Replacing filter ----------------------------------

    /**
     * Creates a {@link JkPathTree} which is a copy of this {@link JkPathTree}
     * replacing matcher by the specified one.
     */
    public JkPathTreeSet withMatcher(PathMatcher matcher) {
        final List<JkPathTree> list = new LinkedList<>();
        for (final JkPathTree tree : this.pathTrees) {
            list.add(JkPathTree.of(tree.getRoot()).withMatcher(JkPathMatcher.of(matcher)));
        }
        return new JkPathTreeSet(list);
    }

    // ---------------------------- iterate over files -----------------------------------

    /**
     * Returns a concatenation of {@link #getFiles()} for all trees involved in this set.
     */
    public List<Path> getFiles() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkPathTree dirView : this.pathTrees) {
            if (dirView.exists()) {
                result.addAll(dirView.getFiles());
            }
        }
        return result;
    }

    /**
     * Returns a concatenation of {@link #getRelativeFiles()} ()} for all trees involved in this set.
     */
    public List<Path> getRelativeFiles() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkPathTree dir : this.pathTrees) {
            if (dir.exists()) {
                result.addAll(dir.getRelativeFiles());
            }
        }
        return result;
    }

    // ----------------------- write out ---------------------------------------------

    /**
     * Zips the content of all trees involved in this set.
     */
    public JkPathTreeSet zipTo(Path dir) {
        this.pathTrees.forEach(tree -> tree.zipTo(dir));
        return this;
    }

    /**
     * Copies the content of all trees involved in this set.
     */
    public JkPathTreeSet copyTo(Path dir, CopyOption... copyOptions) {
        this.pathTrees.forEach(tree -> tree.copyTo(dir, copyOptions));
        return this;
    }


    // -------------------------------------- iterates over trees ----------------------

    /**
     * Returns {@link JkPathTree} instances constituting this {@link JkPathTreeSet}.
     */
    public List<JkPathTree> getPathTrees() {
        return pathTrees;
    }

    /**
     * Returns root dir or zip file for each {@link JkPathTree} tree involved in this
     * {@link JkPathTreeSet}.
     */
    public List<Path> getRootDirsOrZipFiles() {
        final List<Path> result = new LinkedList<>();
        for (final JkPathTree tree : pathTrees) {
            result.add(tree.getRootDirOrZipFile());
        }
        return result;
    }

    // ------------------ Misc -----------------------------------------

    /**
     * Returns <code>true</code> if no tree of this set has an existing baseTree.
     */
    public boolean hasNoExistingRoot() {
        for (final JkPathTree tree : pathTrees) {
            if (tree.exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * See {@link JkPathTree#count(int, boolean)}
     */
    public int count(int max, boolean includeFolder) {
        int result = 0;
        for (final JkPathTree dirView : pathTrees) {
            result += dirView.count(max - result, includeFolder);
        }
        return result;
    }

    public JkPathTreeSet resolve(Path path) {
        List<JkPathTree> list = new LinkedList<>();
        for (JkPathTree tree : pathTrees) {
            list.add(tree.resolve(path));
        }
        return new JkPathTreeSet(list);
    }


    @Override
    public String toString() {
        return this.pathTrees.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pathTrees == null) ? 0 : pathTrees.hashCode());
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
        final JkPathTreeSet other = (JkPathTreeSet) obj;
        if (pathTrees == null) {
            if (other.pathTrees != null) {
                return false;
            }
        } else if (!pathTrees.equals(other.pathTrees)) {
            return false;
        }
        return true;
    }

}
