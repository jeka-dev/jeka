package org.jerkar.api.ide.eclipse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.ide.eclipse.DotClasspathModel.ClasspathEntry.Kind;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsXml;
import org.jerkar.tool.JkException;
import org.jerkar.tool.JkOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class DotClasspathModel {

    private static final String OPTION_VAR_PREFIX = "eclipse.var.";

    static final String CLASSPATHENTRY = "classpathentry";

    static final String JERKAR_HOME = "JERKAR_HOME";

    static final String JERKAR_REPO = "JERKAR_REPO";

    private final List<ClasspathEntry> classpathentries = new LinkedList<>();

    private DotClasspathModel(List<ClasspathEntry> classpathentries) {
        this.classpathentries.addAll(classpathentries);
    }

    static DotClasspathModel from(Path dotClasspathFile) {
        final Document document = JkUtilsXml.documentFrom(dotClasspathFile.toFile());
        return from(document);
    }

    static DotClasspathModel from(Document document) {
        final NodeList nodeList = document.getElementsByTagName(CLASSPATHENTRY);
        final List<ClasspathEntry> classpathEntries = new LinkedList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final Element element = (Element) node;
            classpathEntries.add(ClasspathEntry.from(element));
        }
        return new DotClasspathModel(classpathEntries);
    }

    static DotClasspathModel from(String dotClasspathString) {
        final Document document = JkUtilsXml.documentFrom(dotClasspathString);
        return from(document);
    }

    String outputPath() {
        for (final ClasspathEntry classpathEntry : classpathentries) {
            if (classpathEntry.kind.equals(ClasspathEntry.Kind.OUTPUT)) {
                return classpathEntry.path;
            }
        }
        return null;
    }

    public Sources sourceDirs(Path baseDir, Sources.TestSegregator segregator) {
        final List<JkFileTree> prods = new LinkedList<>();
        final List<JkFileTree> tests = new LinkedList<>();
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
        final List<String> result = new LinkedList<>();
        for (final ClasspathEntry classpathEntry : classpathentries) {
            if (classpathEntry.kind.equals(ClasspathEntry.Kind.SRC)) {
                if (classpathEntry.path.startsWith("/")) {
                    result.add(classpathEntry.path);
                }
            }
        }
        return result;
    }

    public List<Lib> libs(Path baseDir, ScopeResolver scopeResolver) {
        final List<Lib> result = new LinkedList<>();
        final Map<String, Path> projects = JkEclipseProject.findProjectPath(baseDir.getParent());
        for (final ClasspathEntry classpathEntry : classpathentries) {

            if (classpathEntry.kind.equals(ClasspathEntry.Kind.CON)) {
                final JkScope scope = scopeResolver.scopeOfCon(classpathEntry.path);
                if (classpathEntry.path.startsWith(ClasspathEntry.JRE_CONTAINER_PREFIX)) {
                    continue;
                }
                for (final Path file : classpathEntry.conAsFiles()) {
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
                    varFile = JkLocator.jerkarHomeDir().toAbsolutePath().normalize().toString();
                } else if (JERKAR_REPO.equals(var)) {
                    varFile = JkLocator.jerkarRepositoryCache().normalize().toString();
                } else {
                    final String optionName = OPTION_VAR_PREFIX + var;
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

                final Path file = Paths.get(varFile).resolve(JkUtilsString.substringAfterFirst(
                        classpathEntry.path, "/"));
                if (!Files.exists(file)) {
                    JkLog.warn("Can't find Eclipse classpath entry : " + file.toAbsolutePath());
                }
                final JkScope scope = scopeResolver.scopeOfLib(Kind.VAR, classpathEntry.path);
                result.add(Lib.file(file, scope, classpathEntry.exported));

            } else if (classpathEntry.kind.equals(ClasspathEntry.Kind.SRC)) {
                if (classpathEntry.isProjectSrc()) {
                    final String projectPath = classpathEntry.projectRelativePath(baseDir, projects);
                    result.add(Lib.project(projectPath, JkJavaDepScopes.COMPILE,
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

        private final Map<String, String> attributes = new HashMap<>();

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

        public JkFileTree srcAsJkDir(Path baseDir) {
            if (!this.kind.equals(Kind.SRC)) {
                throw new IllegalStateException(
                        "Can only get source dir to classpath entry ofMany kind 'src'.");
            }
            final Path dir = baseDir.resolve(path);
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

        public List<Path> conAsFiles() {
            if (!this.kind.equals(Kind.CON)) {
                throw new IllegalStateException(
                        "Can only get files to classpath entry ofMany kind 'con'.");
            }
            if (!Files.exists(Lib.CONTAINER_DIR) && !Files.exists(Lib.CONTAINER_USER_DIR)) {
                JkLog.warn("Eclipse containers directory " + Lib.CONTAINER_USER_DIR
                        + " or  " + Lib.CONTAINER_DIR + " does not exists... Ignore");
                return Collections.emptyList();
            }
            final String folderName = path.replace('/', '_').replace('\\', '_');
            Path conFolder = Lib.CONTAINER_USER_DIR.resolve(folderName);
            if (!Files.exists(conFolder)) {
                conFolder = Lib.CONTAINER_DIR.resolve(folderName);
                if (!Files.exists(conFolder)) {
                    JkLog.warn("Eclipse containers directory " + conFolder + " or "
                            + Lib.CONTAINER_USER_DIR.resolve(folderName)
                            + "  do not exists... ignogre.");
                    return Collections.emptyList();
                }
            }
            final JkFileTree dirView = JkFileTree.of(conFolder).include("**/*.jar");
            final List<Path> result = new LinkedList<>();
            result.addAll(dirView.files());
            return result;
        }

        public Path libAsFile(Path baseDir, Map<String, Path> projectLocationMap) {
            final String pathInProject;
            final Path pathAsFile = Paths.get(path);
            if (pathAsFile.isAbsolute() && Files.exists(pathAsFile)) {
                return pathAsFile;
            }
            if (path.startsWith("/")) {
                final int secondSlashIndex = path.indexOf("/", 1);
                pathInProject = path.substring(secondSlashIndex + 1);
                final Path otherProjectDir = projectLocation(baseDir.getParent(),
                        projectLocationMap);
                return otherProjectDir.resolve(pathInProject);
            }
            return baseDir.resolve(path);
        }

        public boolean isProjectSrc() {
            return path.startsWith("/");
        }

        public String projectRelativePath(Path baseDir, Map<String, Path> projectLocationMap) {
            final Path projectDir = projectLocation(baseDir.getParent(), projectLocationMap);
            return baseDir.relativize(projectDir).toString();
        }

        private Path projectLocation(Path parent, Map<String, Path> projectLocationMap) {
            final int secondSlashIndex = path.indexOf("/", 1);
            final String projectName;
            if (secondSlashIndex == -1) {
                projectName = path.substring(1);
            } else {
                projectName = path.substring(1, secondSlashIndex);
            }
            final Path otherProjectDir = projectLocationMap.get(projectName);
            if (otherProjectDir == null) {
                throw new IllegalStateException("JkEclipseProject " + projectName + " not found in "
                        + parent);
            }
            return otherProjectDir;
        }

    }

}