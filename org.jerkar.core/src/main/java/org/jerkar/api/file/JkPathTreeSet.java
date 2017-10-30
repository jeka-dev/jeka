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
 * A set of {@link JkPathTree}.
 *
 * @author Jerome Angibaud
 */
public final class JkPathTreeSet {

    private final List<JkPathTree> jkFileTrees;

    private JkPathTreeSet(List<JkPathTree> dirs) {
        if (dirs == null) {
            throw new IllegalArgumentException("dirs can't be null.");
        }
        this.jkFileTrees = Collections.unmodifiableList(dirs);
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
    public static final JkPathTreeSet empty() {
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
     * Creates a {@link JkPathTreeSet} which is a concatenation ofMany this
     * {@link JkPathTreeSet} and the {@link JkPathTree} array passed as
     * parameter.
     */
    public final JkPathTreeSet and(JkPathTree... trees) {
        final List<JkPathTree> list = new LinkedList<>(this.jkFileTrees);
        list.addAll(Arrays.asList(trees));
        return new JkPathTreeSet(list);
    }

    /**
     * Creates a {@link JkPathTreeSet} which is a concatenation ofMany this
     * {@link JkPathTreeSet} and zip files passed as parameter.
     */
    public final JkPathTreeSet andZips(Iterable<Path> zipFiles) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(zipFiles);
        final List<JkPathTree> list = new LinkedList<>(this.jkFileTrees);
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
     * Creates a {@link JkPathTreeSet} which is a concatenation ofMany this
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
     * Creates a {@link JkPathTreeSet} which is a concatenation ofMany this
     * {@link JkPathTreeSet} and the {@link JkPathTreeSet} array passed as
     * parameter.
     */
    public final JkPathTreeSet and(JkPathTreeSet... otherDirSets) {
        final List<JkPathTree> list = new LinkedList<>(this.jkFileTrees);
        for (final JkPathTreeSet otherDirSet : otherDirSets) {
            list.addAll(otherDirSet.jkFileTrees);
        }
        return new JkPathTreeSet(list);
    }

    // ------------------------ additional filters -------------------------------------------


    public JkPathTreeSet accept(Iterable<String> globPatterns) {
        final List<JkPathTree> list = new LinkedList<>();
        for (final JkPathTree tree : this.jkFileTrees) {
            list.add(tree.accept(globPatterns));
        }
        return new JkPathTreeSet(list);
    }

    /**
     * Creates a {@link JkPathTree} which is a copy of this {@link JkPathTree}
     * augmented with the specified {@link JkPathMatcher}
     */
    public JkPathTreeSet andMatcher(PathMatcher matcher) {
        final List<JkPathTree> list = new LinkedList<>();
        for (final JkPathTree tree : this.jkFileTrees) {
            list.add(tree.andMatcher(matcher));
        }
        return new JkPathTreeSet(list);
    }

    // ---------------------------- iterate over files -----------------------------------

    /**
     * Returns a concatenation of {@link #files()} for all trees involved in this set.
     */
    public List<Path> files() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkPathTree dirView : this.jkFileTrees) {
            if (dirView.exists()) {
                result.addAll(dirView.files());
            }
        }
        return result;
    }

    /**
     * Returns a concatenation of {@link #relativeFiles()} ()} for all trees involved in this set.
     */
    public List<Path> relativeFiles() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkPathTree dir : this.jkFileTrees) {
            if (dir.exists()) {
                result.addAll(dir.relativeFiles());
            }
        }
        return result;
    }

    // ----------------------- write out ---------------------------------------------

    /**
     * Zips the content of all trees involved in this set.
     */
    public JkPathTreeSet zipTo(Path dir) {
        this.jkFileTrees.forEach(tree -> tree.zipTo(dir));
        return this;
    }

    // -------------------------------------- iterates over trees ----------------------

    /**
     * Returns {@link JkPathTree} instances constituting this {@link JkPathTreeSet}.
     */
    public List<JkPathTree> fileTrees() {
        return jkFileTrees;
    }

    /**
     * Returns root dir or zip file for each {@link JkPathTree} tree involved in this
     * {@link JkPathTreeSet}.
     */
    public List<Path> rootDirsOrZipFiles() {
        final List<Path> result = new LinkedList<>();
        for (final JkPathTree tree : jkFileTrees) {
            result.add(tree.rootDirOrZipFile());
        }
        return result;
    }

    // ------------------ Misc -----------------------------------------

    /**
     * Returns <code>true</code> if no tree of this set has an existing baseTree.
     */
    public boolean hasNoExistingRoot() {
        for (final JkPathTree tree : jkFileTrees) {
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
        for (final JkPathTree dirView : jkFileTrees) {
            result += dirView.count(max - result, includeFolder);
        }
        return result;
    }

    public JkPathTreeSet resolve(Path path) {
        List<JkPathTree> list = new LinkedList<>();
        for (JkPathTree tree : jkFileTrees) {
            list.add(tree.resolve(path));
        }
        return new JkPathTreeSet(list);
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
        final JkPathTreeSet other = (JkPathTreeSet) obj;
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
