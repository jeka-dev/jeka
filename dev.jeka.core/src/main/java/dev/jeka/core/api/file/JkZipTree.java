package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * A {@link JkPathTree} for zip path tress located in a zip file.
 *  Instances are supposed to be closed by the user code, in a <i>try-with-resource</i> statement or
 *  in a <i>finally</i> clause.
 */
public class JkZipTree extends JkPathTree<JkZipTree> implements Closeable {

    private JkZipTree(JkUtilsPath.JkZipRoot zipRoot, JkPathMatcher pathMatcher) {
        super(zipRoot, pathMatcher);
    }

    /**
     * Creates a path tree from a zip file. The zip file content will be seen as a regular folder.
     *
     * Warn : Don't forget to close this resource when you are finished with.
     */
    public static JkZipTree of(Path zipFile) {
        return new JkZipTree(JkUtilsPath.zipRoot(zipFile), JkPathMatcher.ACCEPT_ALL);
    }

    @Override
    protected JkZipTree newInstance(Supplier<Path> supplier, JkPathMatcher pathMatcher) {
        return new JkZipTree((JkUtilsPath.JkZipRoot) supplier, pathMatcher);
    }

    @Override
    protected JkZipTree withRoot(Path newRoot) {
        return newInstance(zipRoot().withRootInsideZip(newRoot), this.getMatcher());
    }

    @Override
    public JkZipTree createIfNotExist() {
        JkUtilsPath.JkZipRoot zipRoot = zipRoot();
        if (!Files.exists(zipRoot.getZipFile())) {
            JkUtilsPath.createDirectories(zipRoot.get().getParent());
        }
        if (!Files.exists(zipRoot.get())) {
            JkUtilsPath.createDirectories(zipRoot.get());
        }
        return this;
    }

    @Override
    public void close() {
        zipRoot().close();
    }

    private JkUtilsPath.JkZipRoot zipRoot() {
        return  (JkUtilsPath.JkZipRoot) this.rootSupplier;
    }

}
