package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.depmanagement.JkAttachedArtifacts;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkModuleDepFile;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsXml;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildDependencySupport;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.JkException;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.eclipse.DotClasspath.ClasspathEntry.Kind;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class DotClasspath {

    private static final String CLASSPATHENTRY = "classpathentry";

    private static final String ENCODING = "UTF-8";

    private static final String JERKAR_HOME = "JERKAR_HOME";

    private static final String JERKAR_REPO = "JERKAR_REPO";

    private final List<ClasspathEntry> classpathentries = new LinkedList<ClasspathEntry>();

    private DotClasspath(List<ClasspathEntry> classpathentries) {
        this.classpathentries.addAll(classpathentries);
    }

    public static DotClasspath from(File dotClasspathFile) {
        final Document document = JkUtilsXml.documentFrom(dotClasspathFile);
        return from(document);
    }

    private static DotClasspath from(Document document) {
        final NodeList nodeList = document.getElementsByTagName(CLASSPATHENTRY);
        final List<ClasspathEntry> classpathEntries = new LinkedList<ClasspathEntry>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final Element element = (Element) node;
            classpathEntries.add(ClasspathEntry.from(element));
        }
        return new DotClasspath(classpathEntries);
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
                    final String optionName = JkBuildPluginEclipse.OPTION_VAR_PREFIX + var;
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
                        "Can only get source dir from classpath entry of kind 'src'.");
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
                        "Can only get files from classpath entry of kind 'con'.");
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
            if (pathAsFile.isAbsolute()) {
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

    static void generate(JkBuild build, File outputFile, String jreContainer, boolean includeJavadoc)
            throws IOException, XMLStreamException, FactoryConfigurationError {
        final OutputStream fos = new FileOutputStream(outputFile);
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos,
                ENCODING);
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("classpath");
        writer.writeCharacters("\n");

        // Build definition sources
        if (build.file(JkConstants.BUILD_DEF_DIR).exists()) {
            writer.writeCharacters("\t");
            writer.writeEmptyElement(CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("path", JkConstants.BUILD_DEF_DIR);
            writer.writeAttribute("output", JkConstants.BUILD_DEF_BIN_DIR);
            writer.writeCharacters("\n");
        }

        // Write entry for JRE container
        writer.writeCharacters("\t");
        writer.writeEmptyElement(CLASSPATHENTRY);
        writer.writeAttribute("kind", "con");
        final String container;
        if (jreContainer != null) {
            container = jreContainer;
        } else if (build instanceof JkJavaBuild) {
            final JkJavaBuild javaBuild = (JkJavaBuild) build;
            container = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-"
                    + javaBuild.sourceJavaVersion();
        } else {
            container = "org.eclipse.jdt.launching.JRE_CONTAINER";
        }
        writer.writeAttribute("path", container);
        writer.writeCharacters("\n");

        if (build instanceof JkJavaBuild) {
            final JkJavaBuild javaBuild = (JkJavaBuild) build;
            generateJava(javaBuild, writer, jreContainer, includeJavadoc);
        }

        // Write entries for file dependencies
        final List<File> fileDeps = build.buildDefDependencyResolver().dependenciesToResolve()
                .fileSystemDependencies().entries();
        writeFileEntries(fileDeps, writer);

        // Write entries for managed dependencies
        if (build instanceof JkBuildDependencySupport) {
            final JkBuildDependencySupport buildDependencySupport = (JkBuildDependencySupport) build;

            // Write project
            for (final JkBuild project : buildDependencySupport.slaves().directs()) {
                writer.writeCharacters("\t");
                writer.writeEmptyElement(CLASSPATHENTRY);
                writer.writeAttribute("kind", "src");
                writer.writeAttribute("path", "/" + project.baseDir().root().getName());
                writer.writeCharacters("\n");
            }
        }

        // Write output
        writer.writeCharacters("\t");
        writer.writeEmptyElement(CLASSPATHENTRY);
        writer.writeAttribute("kind", "output");
        writer.writeAttribute("path", "bin");
        writer.writeCharacters("\n");
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private static void writeFileEntries(Iterable<File> fileDeps, XMLStreamWriter writer)
            throws XMLStreamException {
        for (final File file : fileDeps) {
            final String name = JkUtilsString.substringBeforeLast(file.getName(), ".jar");
            File source = new File(file.getParentFile(), name + "-sources.jar");
            if (!source.exists()) {
                source = new File(file.getParentFile(), "../../libs-sources/" + name
                        + "-sources.jar");
            }
            if (!source.exists()) {
                source = new File(file.getParentFile(), "libs-sources/" + name + "-sources.jar");
            }
            File javadoc = new File(file.getParentFile(), name + "-javadoc.jar");
            if (!javadoc.exists()) {
                javadoc = new File(file.getParentFile(), "../../libs-javadoc/" + name
                        + "-javadoc.jar");
            }
            if (!javadoc.exists()) {
                javadoc = new File(file.getParentFile(), "libs-javadoc/" + name + "-javadoc.jar");
            }
            writeClassEntry(writer, file, source, javadoc);
        }
    }

    private static void generateJava(JkJavaBuild build, XMLStreamWriter writer,
            String jreContainer, boolean includeJavadoc) throws XMLStreamException {
        // Sources
        final Set<String> sourcePaths = new HashSet<String>();
        for (final JkFileTree jkFileTree : build.sources().and(build.resources()).fileTrees()) {
            if (!jkFileTree.root().exists()) {
                continue;
            }
            final String path = JkUtilsFile.getRelativePath(build.file(""), jkFileTree.root())
                    .replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }

        // Test Sources
        for (final JkFileTree jkFileTree : build.unitTestSources().and(build.unitTestResources())
                .fileTrees()) {
            if (!jkFileTree.root().exists()) {
                continue;
            }
            final String path = JkUtilsFile.getRelativePath(build.file(""), jkFileTree.root())
                    .replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute(
                    "output",
                    JkUtilsFile.getRelativePath(build.file(""), build.testClassDir()).replace(
                            File.separator, "/"));
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }

        // Write entries for external module deps
        JkResolveResult resolveResult = JkResolveResult.empty();
        if (build.dependencyResolver().dependenciesToResolve().containsModules()) {
            resolveResult = build.dependencyResolver().resolve(JkJavaBuild.RUNTIME,
                    JkJavaBuild.PROVIDED, JkJavaBuild.TEST);
            writeExternalModuleEntries(build.dependencyResolver(), writer, resolveResult,
                    includeJavadoc);
        }
        if (build.buildDefDependencyResolver().dependenciesToResolve().containsModules()) {
            final JkResolveResult buildresolve = build.buildDefDependencyResolver().resolve();
            writeExternalModuleEntries(build.buildDefDependencyResolver(), writer, buildresolve,
                    includeJavadoc);
        }

        // Write entries for file dependencies
        final Set<File> fileDeps = new HashSet<File>(build.dependencyResolver()
                .dependenciesToResolve()
                .localFileDependencies(JkJavaBuild.TEST, JkJavaBuild.PROVIDED, JkJavaBuild.COMPILE)
                .entries());
        writeFileEntries(fileDeps, writer);
    }

    private static void writeExternalModuleEntries(JkDependencyResolver dependencyResolver,
            final XMLStreamWriter writer, JkResolveResult resolveResult, boolean includeJavadoc)
            throws XMLStreamException {
        final JkAttachedArtifacts jkAttachedArtifacts = dependencyResolver.getAttachedArtifacts(
                new HashSet<JkVersionedModule>(resolveResult.involvedModules()),
                JkJavaBuild.SOURCES, JkJavaBuild.JAVADOC);

        for (final JkVersionedModule versionedModule : resolveResult.involvedModules()) {
            final JkModuleId moduleId = versionedModule.moduleId();
            final Set<JkModuleDepFile> sourcesArtifacts = jkAttachedArtifacts.getArtifacts(
                    moduleId, JkJavaBuild.SOURCES);
            final File source;
            if (!sourcesArtifacts.isEmpty()) {
                source = sourcesArtifacts.iterator().next().localFile();
            } else {
                source = null;
            }
            final Set<JkModuleDepFile> javadocArtifacts = jkAttachedArtifacts.getArtifacts(
                    moduleId, JkJavaBuild.JAVADOC);
            final File javadoc;
            if (!javadocArtifacts.isEmpty() && includeJavadoc) {
                javadoc = javadocArtifacts.iterator().next().localFile();
            } else {
                javadoc = null;
            }
            writeClassEntry(writer, resolveResult.filesOf(moduleId).get(0), source, javadoc);
        }
    }

    private static void writeClassEntry(XMLStreamWriter writer, File bin, File source, File javadoc)
            throws XMLStreamException {
        writer.writeCharacters("\t");
        if (javadoc == null || !javadoc.exists()) {
            writer.writeEmptyElement(CLASSPATHENTRY);
        } else {
            writer.writeStartElement(CLASSPATHENTRY);
        }
        final VarReplacement binReplacement = new VarReplacement(bin);
        if (binReplacement.replaced) {
            writer.writeAttribute("kind", "var");
        } else {
            writer.writeAttribute("kind", "lib");
        }
        writer.writeAttribute("path", binReplacement.path);
        if (source != null && source.exists()) {
            final VarReplacement sourceReplacement = new VarReplacement(source);
            writer.writeAttribute("sourcepath", sourceReplacement.path);
        }
        if (javadoc != null && javadoc.exists()) {
            writer.writeCharacters("\n\t\t");
            writer.writeStartElement("attributes");
            writer.writeCharacters("\n\t\t\t");
            writer.writeEmptyElement("attribute");
            writer.writeAttribute("name", "javadoc_location");
            writer.writeAttribute("value", "jar:file:/"
                    + JkUtilsFile.canonicalPath(javadoc).replace(File.separator, "/"));
            writer.writeCharacters("\n\t\t");
            writer.writeEndElement();
            writer.writeCharacters("\n\t");
            writer.writeEndElement();
        }
        writer.writeCharacters("\n");
    }

    private static class VarReplacement {

        public final boolean replaced;

        public final String path;

        public VarReplacement(File file) {
            final Map<String, String> map = JkOptions
                    .getAllStartingWith(JkBuildPluginEclipse.OPTION_VAR_PREFIX);
            map.put(JkBuildPluginEclipse.OPTION_VAR_PREFIX + JERKAR_REPO, JkLocator
                    .jerkarRepositoryCache().getAbsolutePath());
            if (!JkLocator.jerkarJarFile().isDirectory()) {
                map.put(JkBuildPluginEclipse.OPTION_VAR_PREFIX + JERKAR_HOME, JkLocator
                        .jerkarHome().getAbsolutePath());
            }
            boolean replaced = false;
            String path = JkUtilsFile.canonicalPath(file).replace(File.separator, "/");
            for (final Map.Entry<String, String> entry : map.entrySet()) {
                final File varDir = new File(entry.getValue());
                if (JkUtilsFile.isAncestor(varDir, file)) {
                    final String relativePath = JkUtilsFile.getRelativePath(varDir, file);
                    replaced = true;
                    path = entry.getKey()
                            .substring(JkBuildPluginEclipse.OPTION_VAR_PREFIX.length())
                            + "/"
                            + relativePath;
                    path = path.replace(File.separator, "/");
                    break;
                }
            }
            this.path = path;
            this.replaced = replaced;
        }

    }

}