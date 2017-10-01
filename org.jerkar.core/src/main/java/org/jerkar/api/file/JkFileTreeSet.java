package org.jerkar.api.file;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsIterable;

/**
 * A set of {@link JkFileTree}.
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
     * Creates a {@link JkFileTreeSet} to a sequence of {@link JkFileTree}.
     */
    public static final JkFileTreeSet of(Iterable<JkFileTree> dirs) {
        return new JkFileTreeSet(JkUtilsIterable.listOf(dirs));
    }

    /**
     * Creates an empty {@link JkFileTreeSet}.
     */
    @SuppressWarnings("unchecked")
    public static final JkFileTreeSet empty() {
        return new JkFileTreeSet(Collections.EMPTY_LIST);
    }

    /**
     * Creates a {@link JkFileTreeSet} to an array of {@link JkFileTree}.
     */
    public static final JkFileTreeSet of(JkFileTree... dirViews) {
        return new JkFileTreeSet(Arrays.asList(dirViews));
    }

    /**
     * Creates a {@link JkFileTreeSet} to an array of folder.
     */
    public static final JkFileTreeSet of(File... folders) {
        final List<JkFileTree> dirs = new ArrayList<>(folders.length);
        for (final File folder : folders) {
            dirs.add(JkFileTree.of(folder));
        }
        return new JkFileTreeSet(dirs);
    }

    /**
     * Creates a {@link JkFileTreeSet} which is a concatenation of this
     * {@link JkFileTreeSet} and the {@link JkFileTree} array passed as
     * parameter.
     */
    public final JkFileTreeSet and(JkFileTree... trees) {
        final List<JkFileTree> list = new LinkedList<>(this.jkFileTrees);
        list.addAll(Arrays.asList(trees));
        return new JkFileTreeSet(list);
    }

    /**
     * Creates a {@link JkFileTreeSet} which is a concatenation of this
     * {@link JkFileTreeSet} and the folder array passed as parameter.
     */
    public final JkFileTreeSet and(File... folders) {
        final List<JkFileTree> dirs = new ArrayList<>(folders.length);
        for (final File folder : folders) {
            dirs.add(JkFileTree.of(folder));
        }
        return this.and(dirs.toArray(new JkFileTree[folders.length]));
    }

    /**
     * Creates a {@link JkFileTreeSet} which is a concatenation of this
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




    /**
     * Creates a {@link JkFileTree} which is a copy of this {@link JkFileTree}
     * augmented with the specified {@link JkPathFilter}
     */
    public JkFileTreeSet andFilter(JkPathFilter filter) {
        final List<JkFileTree> list = new LinkedList<>();
        for (final JkFileTree dirView : this.jkFileTrees) {
            list.add(dirView.andFilter(filter));
        }
        return new JkFileTreeSet(list);
    }

    /**
     * Returns files contained in this {@link JkFileTreeSet} as a list of file.
     */
    public List<File> files(boolean includeFolders) {
        final LinkedList<File> result = new LinkedList<>();
        for (final JkFileTree dirView : this.jkFileTrees) {
            if (dirView.rootDir().exists()) {
                result.addAll(dirView.files(includeFolders));
            }
        }
        return result;
    }

    /**
     * Returns files contained in this {@link JkFileTreeSet} as a list of file.
     */
    public List<Path> filesOnly() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkFileTree dirView : this.jkFileTrees) {
            if (dirView.rootDir().exists()) {
                result.addAll(dirView.filesOnly());
            }
        }
        return result;
    }


    /**
     * Returns path of each files file contained in this {@link JkFileTreeSet}
     * relative to the asScopedDependency of their respective {@link JkFileTree}.
     */
    public List<Path> allRelativePaths() {
        final LinkedList<Path> result = new LinkedList<>();
        for (final JkFileTree dir : this.jkFileTrees) {
            if (dir.rootDir().exists()) {
                result.addAll(dir.filesOnlyRelative());
            }
        }
        return result;
    }

    /**
     * Returns {@link JkFileTree} instances constituting this
     * {@link JkFileTreeSet}.
     */
    public List<JkFileTree> fileTrees() {
        return jkFileTrees;
    }

    /**
     * Returns asScopedDependency of each {@link JkFileTree} instances constituting this
     * {@link JkFileTreeSet}.
     */
    public List<File> roots() {
        final List<File> result = new LinkedList<>();
        for (final JkFileTree dirView : jkFileTrees) {
            result.add(dirView.rootDir());
        }
        return result;
    }

    /**
     * Returns <code>true</code> if no tree of this set has an existing baseTree.
     */
    public boolean hasNoExistingRoot() {
        for (final JkFileTree dirView : jkFileTrees) {
            if (dirView.rootDir().exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of files contained in this {@link JkFileTreeSet}.
     */
    public int countFiles(boolean includeFolder) {
        int result = 0;
        for (final JkFileTree dirView : jkFileTrees) {
            result += dirView.fileCount(includeFolder);
        }
        return result;
    }

    /**
     * Returns a {@link JkZipper} made of the files contained in this
     * {@link JkFileTreeSet}.
     */
    public JkZipper zip() {
        return JkZipper.of(this);
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
