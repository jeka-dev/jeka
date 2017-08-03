package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsXml;
import org.jerkar.tool.JkException;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.eclipse.DotClasspathModel.ClasspathEntry.Kind;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class DotClasspathModel {

    static final String CLASSPATHENTRY = "classpathentry";

    static final String JERKAR_HOME = "JERKAR_HOME";

    static final String JERKAR_REPO = "JERKAR_REPO";

    private final List<ClasspathEntry> classpathentries = new LinkedList<ClasspathEntry>();

    private DotClasspathModel(List<ClasspathEntry> classpathentries) {
        this.classpathentries.addAll(classpathentries);
    }

    public static DotClasspathModel from(File dotClasspathFile) {
        final Document document = JkUtilsXml.documentFrom(dotClasspathFile);
        return from(document);
    }

    private static DotClasspathModel from(Document document) {
        final NodeList nodeList = document.getElementsByTagName(CLASSPATHENTRY);
        final List<ClasspathEntry> classpathEntries = new LinkedList<ClasspathEntry>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final Element element = (Element) node;
            classpathEntries.add(ClasspathEntry.from(element));
        }
        return new DotClasspathModel(classpathEntries);
    }

    public String outputPath() {
        for (final ClasspathEntry classpathEntry : classpathentries) {
            if (classpathEntry.kind.equals(ClasspathEntry.Kind.OUTPUT)) {
                return classpathEntry.path;
            }
        }
        return null;
    }

    public Sources sourceDirs(File baseDir, Sources.TestSegregator segregator) {
        final List<JkFileTree> prods = new LinkedList<JkFileTree>();
        final List<JkFileTree> tests = new LinkedList<JkFileTree>();
        for (final ClasspathEntry classpathEntry : classpathentries) {
            if (classpathEntry.kind.equals(ClasspathEntry.Kind.SRC) && !classpathEntry.isOptional()) {
                if (segregator.isTest(classpathEntry.path)) {
                    tests.add(classpathEntry.srcAsJkDir(baseDir));
                } else {
                    prods.add(classpathEntry.srcAsJkDir(baseDir));
                }
            }
        }
        return new Sources(JkFileTreeSet.of(prods), JkFileTreeSet.of(tests));
    }

    public List<String> projectDependencies() {
        final List<String> result = new LinkedList<String>();
        for (final ClasspathEntry classpathEntry : classpathentries) {
            if (classpathEntry.kind.equals(ClasspathEntry.Kind.SRC)) {
                if (classpathEntry.path.startsWith("/")) {
                    result.add(classpathEntry.path);
                }
            }
        }
        return result;
    }

    public List<Lib> libs(File baseDir, ScopeResolver scopeResolver) {
        final List<Lib> result = new LinkedList<Lib>();
        final Map<String, File> projects = Project.findProjects(baseDir.getParentFile());
        for (final ClasspathEntry classpathEntry : classpathentries) {

            if (classpathEntry.kind.equals(ClasspathEntry.Kind.CON)) {
                final JkScope scope = scopeResolver.scopeOfCon(classpathEntry.path);
                if (classpathEntry.path.startsWith(ClasspathEntry.JRE_CONTAINER_PREFIX)) {
                    continue;
                }
                for (final File file : classpathEntry.conAsFiles(baseDir)) {
                    result.add(Lib.file(file, scope, classpathEntry.exported));
                }

            } else if (classpathEntry.kind.equals(ClasspathEntry.Kind.LIB)) {
                final JkScope scope = scopeResolver.scopeOfLib(ClasspathEntry.Kind.LIB,
                        classpathEntry.path);
                result.add(Lib.file(classpathEntry.libAsFile(baseDir, projects), scope,
                        classpathEntry.exported));

            } else if (classpathEntry.kind.equals(ClasspathEntry.Kind.VAR)) {
                final String var = JkUtilsString.substringBeforeFirst(classpathEntry.path, "/");
                final String varFile;
                if (JERKAR_HOME.equals(var)) {
                    varFile = JkUtilsFile.canonicalPath(JkLocator.jerkarHome());
                } else if (JERKAR_REPO.equals(var)) {
                    varFile = JkUtilsFile.canonicalPath(JkLocator.jerkarRepositoryCache());
                } else {
                    final String optionName = DotClasspathGenerator.OPTION_VAR_PREFIX + var;
                    varFile = JkOptions.get(optionName);
                    if (varFile == null) {
                        throw new JkException(
                                "No option found with name "
                                        + optionName
                                        + ". It is needed in order to build this project as it is mentionned in Eclipse .classpath."
                                        + " Please set this option either in command line as -"
                                        + optionName
                                        + "=/absolute/path/for/this/var or in [jerkar_home]/options.properties");
                    }
                }

                final File file = new File(varFile, JkUtilsString.substringAfterFirst(
                        classpathEntry.path, "/"));
                if (!file.exists()) {
                    JkLog.warn("Can't find Eclipse classpath entry : " + file.getAbsolutePath());
                }
                final JkScope scope = scopeResolver.scopeOfLib(Kind.VAR, classpathEntry.path);
                result.add(Lib.file(file, scope, classpathEntry.exported));

            } else if (classpathEntry.kind.equals(ClasspathEntry.Kind.SRC)) {
                if (classpathEntry.isProjectSrc(baseDir.getParentFile(), projects)) {
                    final String projectPath = classpathEntry
                            .projectRelativePath(baseDir, projects);
                    result.add(Lib.project(projectPath, JkJavaBuild.COMPILE,
                            classpathEntry.exported));
                }

            }
        }
        return result;
    }

    static class ClasspathEntry {

        public final static String JRE_CONTAINER_PREFIX = "org.eclipse.jdt.launching.JRE_CONTAINER";

        public enum Kind {
            SRC, CON, LIB, VAR, OUTPUT, UNKNOWN
        }

        private final Kind kind;

        private final boolean exported;

        private final String path;

        private final String excluding;

        private final String including;

        private final Map<String, String> attributes = new HashMap<String, String>();

        public ClasspathEntry(Kind kind, String path, String excluding, String including,
                boolean exported) {
            super();
            this.kind = kind;
            this.path = path;
            this.excluding = excluding;
            this.including = including;
            this.exported = exported;
        }

        public static ClasspathEntry of(Kind kind, String path) {
            return new ClasspathEntry(kind, path, null, null, false);
        }

        public static ClasspathEntry from(Element classpathEntryEl) {
            final String kindString = classpathEntryEl.getAttribute("kind");
            final String path = classpathEntryEl.getAttribute("path");
            final String including = classpathEntryEl.getAttribute("including");
            final String excluding = classpathEntryEl.getAttribute("excluding");
            final Kind kind;
            if ("lib".equals(kindString)) {
                kind = Kind.LIB;
            } else if ("con".equals(kindString)) {
                kind = Kind.CON;
            } else if ("src".equals(kindString)) {
                kind = Kind.SRC;
            } else if ("var".equals(kindString)) {
                kind = Kind.VAR;
            } else if ("output".equals(kindString)) {
                kind = Kind.OUTPUT;
            } else {
                kind = Kind.UNKNOWN;
            }
            final String exportedString = classpathEntryEl.getAttribute("exported");
            final boolean export = "true".equals(exportedString);
            final ClasspathEntry result = new ClasspathEntry(kind, path, excluding, including,
                    export);
            final NodeList nodeList = classpathEntryEl.getElementsByTagName("attributes");
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Element attributeEl = (Element) nodeList.item(i);
                final String name = attributeEl.getAttribute("name");
                final String value = attributeEl.getAttribute("value");
                result.attributes.put(name, value);
            }
            return result;
        }

        public JkFileTree srcAsJkDir(File baseDir) {
            if (!this.kind.equals(Kind.SRC)) {
                throw new IllegalStateException(
                        "Can only get source dir to classpath entry of kind 'src'.");
            }
            final File dir = new File(baseDir, path);
            JkFileTree jkFileTree = JkFileTree.of(dir);
            if (!excluding.isEmpty()) {
                final String[] patterns = excluding.split("\\|");
                jkFileTree = jkFileTree.exclude(patterns);
            }
            if (!including.isEmpty()) {
                final String[] patterns = including.split("\\|");
                jkFileTree = jkFileTree.include(patterns);
            }
            return jkFileTree;
        }

        public boolean isOptional() {
            return "true".equals(this.attributes.get("optional"));
        }

        public boolean sameTypeAndPath(ClasspathEntry other) {
            if (!this.kind.equals(other.kind)) {
                return false;
            }
            return this.path.equals(other.path);
        }

        public List<File> conAsFiles(File baseDir) {
            if (!this.kind.equals(Kind.CON)) {
                throw new IllegalStateException(
                        "Can only get files to classpath entry of kind 'con'.");
            }
            if (!Lib.CONTAINER_DIR.exists() && !Lib.CONTAINER_USER_DIR.exists()) {
                JkLog.warn("Eclipse containers directory " + Lib.CONTAINER_USER_DIR.getPath()
                        + " or  " + Lib.CONTAINER_DIR.getPath() + " does not exists... Ignore");
                return Collections.emptyList();
            }
            final String folderName = path.replace('/', '_').replace('\\', '_');
            File conFolder = new File(Lib.CONTAINER_USER_DIR, folderName);
            if (!conFolder.exists()) {
                conFolder = new File(Lib.CONTAINER_DIR, folderName);
                if (!conFolder.exists()) {
                    JkLog.warn("Eclipse containers directory " + conFolder.getPath() + " or "
                            + new File(Lib.CONTAINER_USER_DIR, folderName).getPath()
                            + "  do not exists... ignogre.");
                    return Collections.emptyList();
                }
            }
            final JkFileTree dirView = JkFileTree.of(conFolder).include("**/*.jar");
            final List<File> result = new LinkedList<File>();
            for (final File file : dirView.files(false)) {
                result.add(file);
            }
            return result;
        }

        public File libAsFile(File baseDir, Map<String, File> projectLocationMap) {
            final String pathInProject;
            final File pathAsFile = new File(path);
            if (pathAsFile.isAbsolute()  && pathAsFile.exists()) {
                return pathAsFile;
            }
            if (path.startsWith("/")) {
                final int secondSlashIndex = path.indexOf("/", 1);
                pathInProject = path.substring(secondSlashIndex + 1);
                final File otherProjectDir = projectLocation(baseDir.getParentFile(),
                        projectLocationMap);
                return new File(otherProjectDir, pathInProject);
            }
            return new File(baseDir, path);
        }

        public boolean isProjectSrc(File parent, Map<String, File> projectLocationMap) {
            return path.startsWith("/");
        }

        public String projectRelativePath(File baseDir, Map<String, File> projectLocationMap) {
            final File projectDir = projectLocation(baseDir.getParentFile(), projectLocationMap);
            return "../" + projectDir.getName();
        }

        private File projectLocation(File parent, Map<String, File> projectLocationMap) {
            final int secondSlashIndex = path.indexOf("/", 1);
            final String projectName;
            if (secondSlashIndex == -1) {
                projectName = path.substring(1);
            } else {
                projectName = path.substring(1, secondSlashIndex);
            }
            final File otherProjectDir = projectLocationMap.get(projectName);
            if (otherProjectDir == null) {
                throw new IllegalStateException("Project " + projectName + " not found in "
                        + parent.getPath());
            }
            return otherProjectDir;
        }

    }

}