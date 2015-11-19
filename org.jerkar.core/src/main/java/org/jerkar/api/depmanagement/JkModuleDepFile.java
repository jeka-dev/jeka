package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsObject;

/**
 * A file coming from a module dependency
 * 
 */
public final class JkModuleDepFile implements Serializable {

    private static final long serialVersionUID = 1L;

    public static JkModuleDepFile of(JkVersionedModule versionedModule, File localFile) {
        return new JkModuleDepFile(versionedModule, localFile);
    }

    private final JkVersionedModule versionedModule;

    private final File localFile;

    private JkModuleDepFile(JkVersionedModule versionedModule, File localFile) {
        super();
        this.versionedModule = versionedModule;
        this.localFile = localFile;
    }

    public JkVersionedModule versionedModule() {
        return versionedModule;
    }

    public File localFile() {
        return localFile;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + JkUtilsObject.hashCode(localFile);
        result = prime * result + versionedModule.hashCode();
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
        final JkModuleDepFile other = (JkModuleDepFile) obj;
        if (!JkUtilsObject.equals(this.localFile, other.localFile)) {
            return false;
        }
        if (!versionedModule.equals(other.versionedModule)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "JkModuleDepFile [versionedModule=" + versionedModule + ", localFile=" + localFile
                + "]";
    }

    public static List<File> localFiles(Iterable<JkModuleDepFile> artifacts) {
        final List<File> result = new LinkedList<File>();
        for (final JkModuleDepFile artifact : artifacts) {
            result.add(artifact.localFile);
        }
        return result;
    }

    public static Set<JkVersionedModule> versionedModules(Set<JkModuleDepFile> artifacts) {
        final Set<JkVersionedModule> result = new HashSet<JkVersionedModule>();
        for (final JkModuleDepFile artifact : artifacts) {
            result.add(artifact.versionedModule());
        }
        return result;
    }

}
