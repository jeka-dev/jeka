package org.jerkar.api.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

/**
 * Utility class serving as factory for creating specific {@link FileFilter}
 * 
 * @author Jerome Angibaud
 */
public final class JkFileFilters {

    /**
     * Creates a {@link FileFilter} that accept files having a name
     * ending with one of the specified suffixes.
     */
    public static FileFilter endingBy(final String... suffixes) {
        return file -> {
            for (final String suffix : suffixes) {
                if (file.getName().endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Creates a file filter that accept all files.
     */
    public static FileFilter acceptAll() {
        return new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return true;
            }

            @Override
            public String toString() {
                return "Accept-All filter";
            }
        };
    }

    /**
     * Creates a file filter that accept only the specified file.
     */
    public static FileFilter acceptOnly(final File fileToAccept) {
        return new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.equals(fileToAccept);
            }

            @Override
            public String toString() {
                return "Accept only " + fileToAccept.getAbsolutePath();
            }
        };
    }

    /**
     * Creates a file filter that accepts file accepted by both specified filters
     */
    public static FileFilter and(final FileFilter filter1, final FileFilter filter2) {
        return new FileFilter() {

            @Override
            public boolean accept(File candidate) {
                return filter1.accept(candidate) && filter2.accept(candidate);
            }

            @Override
            public String toString() {
                return "{" + filter1 + "," + filter2 + "}";
            }
        };
    }

    /**
     * Creates a fileFilter that accept file rejected by specified filter.
     */
    public static FilenameFilter reverse(final FilenameFilter filter) {
        return (dir, name) -> !filter.accept(dir, name);
    }

    /**
     * Creates a fileFilter that accept file rejected by specified filter.
     */
    public static FileFilter reverse(final FileFilter filter) {
        return new FileFilter() {

            @Override
            public boolean accept(File candidate) {
                return !filter.accept(candidate);
            }

            @Override
            public String toString() {
                return "revert of (" + filter + ")";
            }
        };
    }

}
