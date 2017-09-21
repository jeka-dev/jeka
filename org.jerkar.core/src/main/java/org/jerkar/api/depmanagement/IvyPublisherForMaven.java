package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.RepositoryResolver;
import org.apache.ivy.util.ChecksumHelper;
import org.jerkar.api.depmanagement.IvyPublisher.CheckFileFlag;
import org.jerkar.api.depmanagement.JkMavenPublication.JkClassifiedFileArtifact;
import org.jerkar.api.depmanagement.MavenMetadata.Versioning.Snapshot;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * {@link IvyPublisher} delegates to this class for publishing to Maven
 * repositories.
 */
final class IvyPublisherForMaven {

    private final RepositoryResolver resolver;

    private final CheckFileFlag checkFileFlag;

    private final File descriptorOutputDir;

    private final boolean uniqueSnapshot;

    IvyPublisherForMaven(CheckFileFlag checkFileFlag, RepositoryResolver dependencyResolver,
            File descriptorOutputDir, boolean uniqueSnapshot) {
        super();
        this.resolver = dependencyResolver;
        this.descriptorOutputDir = descriptorOutputDir;
        this.checkFileFlag = checkFileFlag;
        this.uniqueSnapshot = uniqueSnapshot;
    }

    void publish(DefaultModuleDescriptor moduleDescriptor, JkMavenPublication publication) {
        final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
        try {
            resolver.beginPublishTransaction(ivyModuleRevisionId, true);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        // publish artifacts
        final JkVersionedModule versionedModule = IvyTranslations
                .toJkVersionedModule(ivyModuleRevisionId);
        final MavenMetadata returnedMetaData = publish(versionedModule, publication);

        // publish pom
        final File pomXml = makePom(moduleDescriptor, publication);
        final String version;
        if (versionedModule.version().isSnapshot() && this.uniqueSnapshot) {
            final String path = snapshotMetadataPath(versionedModule);
            final MavenMetadata mavenMetadata = JkUtilsObject.firstNonNull(loadMavenMedatata(path),
                    returnedMetaData);
            final Snapshot snap = mavenMetadata.currentSnapshot();
            version = versionForUniqueSnapshot(versionedModule.version().name(), snap.timestamp,
                    snap.buildNumber);
            final String pomDest = destination(versionedModule, "pom", null, version);
            putAll(pomXml, pomDest, true);
            mavenMetadata.addSnapshotVersion("pom", null);
            push(mavenMetadata, path);
        } else {
            version = versionedModule.version().name();
            final String pomDest = destination(versionedModule, "pom", null, version);
            putAll(pomXml, pomDest, true);
        }
        if (this.descriptorOutputDir == null) {
            pomXml.delete();
        }

        // update maven-metadata
        if (returnedMetaData != null) {
            updateMetadata(ivyModuleRevisionId.getModuleId(), ivyModuleRevisionId.getRevision(),
                    returnedMetaData.lastUpdateTimestamp());
        }

        commitPublication(resolver);
    }

    private MavenMetadata publish(JkVersionedModule versionedModule,
            JkMavenPublication mavenPublication) {
        if (!versionedModule.version().isSnapshot()) {
            final String existing = checkNotExist(versionedModule, mavenPublication);
            if (existing != null) {
                throw new IllegalArgumentException("Artifact " + existing
                        + " already exists on repo.");
            }
        }
        if (versionedModule.version().isSnapshot() && this.uniqueSnapshot) {
            final String path = snapshotMetadataPath(versionedModule);
            MavenMetadata mavenMetadata = loadMavenMedatata(path);
            final String timestamp = JkUtilsTime.nowUtc("yyyyMMdd.HHmmss");
            if (mavenMetadata == null) {
                mavenMetadata = MavenMetadata.of(versionedModule, timestamp);
            }
            mavenMetadata.updateSnapshot(timestamp);
            push(mavenMetadata, path);
            final int buildNumber = mavenMetadata.currentBuildNumber();

            final String versionUniqueSnapshot = versionForUniqueSnapshot(versionedModule.version()
                    .name(), timestamp, buildNumber);

            for (final File file : mavenPublication.mainArtifactFiles()) {
                publishUniqueSnapshot(versionedModule, null, file, versionUniqueSnapshot,
                        mavenMetadata);
            }
            for (final JkClassifiedFileArtifact classifiedArtifact : mavenPublication
                    .classifiedArtifacts()) {
                publishUniqueSnapshot(versionedModule, classifiedArtifact.classifier(),
                        classifiedArtifact.file(), versionUniqueSnapshot, mavenMetadata);
            }
            return mavenMetadata;
        } else {
            for (final File file : mavenPublication.mainArtifactFiles()) {
                publishNormal(versionedModule, null, file);
            }
            for (final JkClassifiedFileArtifact classifiedArtifact : mavenPublication
                    .classifiedArtifacts()) {
                publishNormal(versionedModule, classifiedArtifact.classifier(),
                        classifiedArtifact.file());
            }
            return null;
        }
    }

    private File makePom(ModuleDescriptor moduleDescriptor, JkMavenPublication publication) {
        final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
        final String artifactName = ivyModuleRevisionId.getName();
        final File pomXml;
        if (this.descriptorOutputDir != null) {
            pomXml = new File(targetDir(), "published-pom-" + ivyModuleRevisionId.getOrganisation()
            + "-" + artifactName + "-" + ivyModuleRevisionId.getRevision() + ".xml");
        } else {
            pomXml = JkUtilsFile.tempFile("published-pom-", ".xml");
        }
        final String packaging = JkUtilsString.substringAfterLast(publication.mainArtifactFiles()
                .get(0).getName(), ".");
        final PomWriterOptions pomWriterOptions = new PomWriterOptions();
        pomWriterOptions.setArtifactPackaging(packaging);
        File fileToDelete = null;
        if (publication.extraInfo() != null) {
            final File template = PomTemplateGenerator.generateTemplate(publication.extraInfo());
            pomWriterOptions.setTemplate(template);
            fileToDelete = template;
        }
        try {
            PomModuleDescriptorWriter.write(moduleDescriptor, pomXml, pomWriterOptions);
            if (fileToDelete != null) {
                JkUtilsFile.delete(fileToDelete);
            }
            return pomXml;
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private String checkNotExist(JkVersionedModule versionedModule,
            JkMavenPublication mavenPublication) {
        if (!mavenPublication.mainArtifactFiles().isEmpty()) {
            final String pomDest = destination(versionedModule, "pom", null);
            if (existOnRepo(pomDest)) {
                throw new IllegalArgumentException("The main artifact as already exist for "
                        + versionedModule);
            }
            for (final File file : mavenPublication.mainArtifactFiles()) {
                final String ext = JkUtilsString.substringAfterLast(file.getName(), ".");
                final String dest = destination(versionedModule, ext, null);
                if (existOnRepo(dest)) {
                    return dest;
                }
            }
        }
        for (final JkClassifiedFileArtifact classifiedArtifact : mavenPublication.classifiedArtifacts()) {
            final String ext = JkUtilsString.substringAfterLast(
                    classifiedArtifact.file().getName(), ".");
            final String dest = destination(versionedModule, ext, classifiedArtifact.classifier());
            if (existOnRepo(dest)) {
                return dest;
            }
        }
        return null;
    }

    private boolean existOnRepo(String dest) {
        try {
            final String path = completePath(dest);
            final Resource resource = resolver.getRepository().getResource(path);
            return resource.exists();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void publishUniqueSnapshot(JkVersionedModule versionedModule, String classifier,
            File source, String versionForUniqueSpshot, MavenMetadata mavenMetadata) {

        final String extension = JkUtilsString.substringAfterLast(source.getName(), ".");
        final String dest = destination(versionedModule, extension, classifier,
                versionForUniqueSpshot);
        putAll(source, dest, false);
        final String path = snapshotMetadataPath(versionedModule);
        mavenMetadata.addSnapshotVersion(extension, classifier);
        push(mavenMetadata, path);
    }

    private void publishNormal(JkVersionedModule versionedModule, String classifier, File source) {

        final String extension = JkUtilsString.substringAfterLast(source.getName(), ".");
        final String version = versionedModule.version().name();
        final String dest = destination(versionedModule.withVersion(version), extension, classifier);
        final boolean overwrite = versionedModule.version().isSnapshot();
        putAll(source, dest, overwrite);
    }

    private static String destination(JkVersionedModule versionedModule, String ext,
            String classifier) {
        return destination(versionedModule, ext, classifier, versionedModule.version().name());
    }

    private static String destination(JkVersionedModule versionedModule, String ext,
            String classifier, String uniqueVersion) {
        final JkModuleId moduleId = versionedModule.moduleId();
        final String version = versionedModule.version().name();
        final StringBuilder result = new StringBuilder(moduleBasePath(moduleId)).append("/")
                .append(version).append("/").append(moduleId.name()).append("-")
                .append(uniqueVersion);
        if (classifier != null) {
            result.append("-").append(classifier);
        }
        result.append(".").append(ext);
        return result.toString();
    }

    private static String versionForUniqueSnapshot(String version, String timestamp, int buildNumber) {
        return version.endsWith("-SNAPSHOT") ? JkUtilsString.substringBeforeLast(version,
                "-SNAPSHOT") + "-" + timestamp + "-" + buildNumber : version;
    }

    private void updateMetadata(ModuleId moduleId, String version, String timestamp) {
        final String path = versionMetadataPath(of(moduleId, version));
        MavenMetadata mavenMetadata = loadMavenMedatata(path);
        if (mavenMetadata == null) {
            mavenMetadata = MavenMetadata.of(JkModuleId.of(moduleId.getOrganisation(),
                    moduleId.getName()));
        }
        mavenMetadata.addVersion(version, timestamp);
        push(mavenMetadata, path);
    }

    private void push(MavenMetadata metadata, String path) {
        final File file = JkUtilsFile.tempFile("metadata-", ".xml");
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            metadata.output(outputStream);
            outputStream.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        JkUtilsIO.closeQuietly(outputStream);
        putAll(file, path, true);
    }

    private static JkVersionedModule of(ModuleId moduleId, String version) {
        return JkVersionedModule.of(JkModuleId.of(moduleId.getOrganisation(), moduleId.getName()),
                JkVersion.name(version));
    }

    private static String versionMetadataPath(JkVersionedModule module) {
        return moduleBasePath(module.moduleId()) + "/maven-metadata.xml";
    }

    private static String moduleBasePath(JkModuleId module) {
        return module.group().replace(".", "/") + "/" + module.name();
    }

    private static String snapshotMetadataPath(JkVersionedModule module) {
        return moduleBasePath(module.moduleId()) + "/" + module.version().name()
                + "/maven-metadata.xml";
    }

    private MavenMetadata loadMavenMedatata(String path) {
        try {
            final Resource resource = resolver.getRepository().getResource(completePath(path));
            if (resource.exists()) {
                final InputStream inputStream = resource.openStream();
                final MavenMetadata mavenMetadata = MavenMetadata.of(inputStream);
                inputStream.close();
                return mavenMetadata;
            }
            return null;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void putAll(File source, String dest, boolean overwrite) {
        putAll(source, dest, overwrite, true);
    }

    private String completePath(String path) {
        if (this.resolver instanceof IBiblioResolver) {
            final IBiblioResolver iBiblioResolver = (IBiblioResolver) this.resolver;
            return iBiblioResolver.getRoot() + path;
        }
        return path;
    }

    private void putAll(File source, String destination, boolean overwrite, boolean signIfneeded) {
        final String[] checksums = this.resolver.getChecksumAlgorithms();
        final Repository repository = this.resolver.getRepository();
        try {
            final String dest = completePath(destination);
            JkLog.info("publishing to " + dest);
            repository.put(null, source, dest, overwrite);
            for (final String algo : checksums) {
                final File temp = JkUtilsFile.tempFile("jk-checksum-", algo);
                final String checkSum = ChecksumHelper.computeAsString(source, algo);
                JkUtilsFile.writeString(temp, checkSum, false);
                final String csDest = dest + "." + algo;
                JkLog.info("publishing to " + csDest);
                repository.put(null, temp, csDest, overwrite);
                temp.delete();
            }
            if (this.checkFileFlag.pgpSigner != null && signIfneeded) {
                final File signed = checkFileFlag.pgpSigner.sign(source)[0];
                final String signedDest = destination + ".asc";
                putAll(signed, signedDest, overwrite, false);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String targetDir() {
        return this.descriptorOutputDir.getAbsolutePath();
    }

    private static void commitPublication(DependencyResolver resolver) {
        try {
            resolver.commitPublishTransaction();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

}
