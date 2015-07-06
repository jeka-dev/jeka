package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.net.ssl.SSLException;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.RepositoryResolver;
import org.apache.ivy.util.ChecksumHelper;
import org.jerkar.api.depmanagement.IvyPublisher.CheckFileFlag;
import org.jerkar.api.depmanagement.JkMavenPublication.JkClassifiedArtifact;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.api.utils.JkUtilsTime;
import org.jerkar.tool.JkException;

/**
 * {@link IvyPublisher} delegates to this class for publishing to Maven
 * repositories.
 */
final class IvyPublisherForMaven {

	private final RepositoryResolver resolver;

	private final CheckFileFlag checkFileFlag;

	private final File descriptorOutputDir;

	private final boolean uniqueSnapshot;

	IvyPublisherForMaven(CheckFileFlag checkFileFlag,
			RepositoryResolver dependencyResolver, File descriptorOutputDir, boolean uniqueSnapshot) {
		super();
		this.resolver = dependencyResolver;
		this.descriptorOutputDir = descriptorOutputDir;
		this.checkFileFlag = checkFileFlag;
		this.uniqueSnapshot = uniqueSnapshot;
	}

	/**
	 * Publish to maven repositories
	 */
	void publish(DefaultModuleDescriptor moduleDescriptor,
			JkMavenPublication publication) {
		final Date date = JkUtilsTime.now();
		final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor
				.getModuleRevisionId();
		try {
			resolver.beginPublishTransaction(ivyModuleRevisionId, true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		final String artifactName = ivyModuleRevisionId.getName();
		final File pomXml = makePom(moduleDescriptor, publication);

		final Artifact pomArtifact = new DefaultArtifact(ivyModuleRevisionId,
				date, artifactName, "pom", "pom", true);
		try {
			resolver.publish(pomArtifact, pomXml, true);
			checkFileFlag.publishChecks(resolver, pomArtifact, pomXml);

			final Artifact mavenMainArtifact = IvyTranslations
					.toPublishedMavenArtifact(publication.mainArtifactFiles().get(0),
							artifactName, null,
							ivyModuleRevisionId, date);
			resolver.publish(mavenMainArtifact, publication.mainArtifactFiles().get(0),
					true);
			checkFileFlag.publishChecks(resolver, mavenMainArtifact,
					publication.mainArtifactFiles().get(0));

			for (final JkClassifiedArtifact classifiedArtifact : publication.classifiedArtifacts()) {
				final String classifier = classifiedArtifact.classifier();
				final File file = classifiedArtifact.file();
				final Artifact mavenArtifact = IvyTranslations
						.toPublishedMavenArtifact(file,
								artifactName, classifier,
								ivyModuleRevisionId, date);
				resolver.publish(mavenArtifact, file, true);
				checkFileFlag.publishChecks(resolver, mavenArtifact, file);
			}

		} catch (final Exception e) {
			abortPublishTransaction(resolver);
			if (JkUtilsThrowable.isInCause(e, SSLException.class)) {
				throw JkUtilsThrowable.unchecked(e,
						"Error related to SSH communication with " + resolver);
			}
			throw JkUtilsThrowable.unchecked(e);
		}
		commitPublication(resolver);
		updateMetadata(ivyModuleRevisionId.getModuleId(),
				ivyModuleRevisionId.getRevision());
	}

	void publish2(DefaultModuleDescriptor moduleDescriptor,
			JkMavenPublication publication) {
		final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor
				.getModuleRevisionId();
		try {
			resolver.beginPublishTransaction(ivyModuleRevisionId, true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		// publish artifacts
		final JkVersionedModule versionedModule = IvyTranslations.toJerkarVersionedModule(ivyModuleRevisionId);
		publish(versionedModule, publication);

		// publish pom
		final File pomXml = makePom(moduleDescriptor, publication);
		final String pomDest = destination(versionedModule, "pom", null);
		putAll(pomXml, pomDest, true);

		//update maven-metadata
		updateMetadata(ivyModuleRevisionId.getModuleId(),
				ivyModuleRevisionId.getRevision());

		commitPublication(resolver);
	}

	private void publish(JkVersionedModule versionedModule, JkMavenPublication mavenPublication) {
		if (!versionedModule.version().isSnapshot()) {
			final String existing = checkNotExist(versionedModule, mavenPublication);
			if (existing != null) {
				throw new JkException("Artifact " + existing + " already exists on repo.");
			}
		}
		if (versionedModule.version().isSnapshot() && this.uniqueSnapshot) {
			final String path = snapshotMetadataPath(versionedModule);
			MavenMetadata mavenMetadata = loadMavenMedatata(path);
			if (mavenMetadata == null) {
				mavenMetadata = MavenMetadata.of(versionedModule);
			}
			mavenMetadata.updateSnapshot();
			push(mavenMetadata, path);
			final int buildNumber = mavenMetadata.currentBuildNumber() + 1;
			for (final File file : mavenPublication.mainArtifactFiles()) {
				publishUniqueSnapshot(versionedModule, null, file, buildNumber);
			}
			for (final JkClassifiedArtifact classifiedArtifact : mavenPublication.classifiedArtifacts()) {
				publishUniqueSnapshot(versionedModule, classifiedArtifact.classifier(), classifiedArtifact.file(), buildNumber);
			}
		} else {
			for (final File file : mavenPublication.mainArtifactFiles()) {
				publishNormal(versionedModule, null, file);
			}
			for (final JkClassifiedArtifact classifiedArtifact : mavenPublication.classifiedArtifacts()) {
				publishNormal(versionedModule, classifiedArtifact.classifier(), classifiedArtifact.file());
			}
		}
	}

	private File makePom(ModuleDescriptor moduleDescriptor, JkMavenPublication publication) {
		final ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
		final String artifactName = ivyModuleRevisionId.getName();
		final File pomXml = new File(targetDir(), "published-pom-"
				+ ivyModuleRevisionId.getOrganisation() + "-" + artifactName + "-"
				+ ivyModuleRevisionId.getRevision() + ".xml");
		final String packaging = JkUtilsString.substringAfterLast(publication
				.mainArtifactFiles().get(0).getName(), ".");
		final PomWriterOptions pomWriterOptions = new PomWriterOptions();
		pomWriterOptions.setArtifactPackaging(packaging);
		File fileToDelete = null;
		if (publication.extraInfo() != null) {
			final File template = PomTemplateGenerator
					.generateTemplate(publication.extraInfo());
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

	private String checkNotExist(JkVersionedModule versionedModule, JkMavenPublication mavenPublication) {
		if (!mavenPublication.mainArtifactFiles().isEmpty()) {
			final String pomDest = destination(versionedModule, "pom", null);
			if (existOnRepo(pomDest)) {
				throw new JkException("The main artifact as already exist for " + versionedModule);
			}
			for (final File file : mavenPublication.mainArtifactFiles()) {
				final String ext = JkUtilsString.substringAfterLast(
						file.getName(), ".");
				final String dest = destination(versionedModule, ext, null);
				if (existOnRepo(dest)) {
					return dest;
				}
			}
		}
		for (final JkClassifiedArtifact classifiedArtifact : mavenPublication.classifiedArtifacts()) {
			final String ext = JkUtilsString.substringAfterLast(classifiedArtifact.file().getName(), ".");
			final String dest = destination(versionedModule, ext, classifiedArtifact.classifier());
			if (existOnRepo(dest)) {
				return dest;
			}
		}
		return null;
	}

	private boolean existOnRepo(String dest) {
		try {
			final Resource resource = resolver.getRepository()
					.getResource(dest);
			return resource.exists();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void publishUniqueSnapshot(JkVersionedModule versionedModule,
			String classifier, File source, int buildNumber) {

		final String extension = JkUtilsString.substringAfterLast(
				source.getName(), ".");
		final String timestamp = JkUtilsTime.now("yyyyMMdd.HHmmss");
		final String version = versionForUniqueSnapshot(versionedModule
				.version().name(), timestamp, buildNumber);
		final String dest = destination(versionedModule, extension, classifier, version);
		putAll(source, dest, false);
		final String path = snapshotMetadataPath(versionedModule);
		MavenMetadata mavenMetadata = loadMavenMedatata(path);
		if (mavenMetadata == null) {
			mavenMetadata = MavenMetadata.of(versionedModule);
		}
		mavenMetadata.addSnapshotVersion(extension, classifier);
		push(mavenMetadata, path);
	}

	private void publishNormal(JkVersionedModule versionedModule,
			String classifier, File source) {

		final String extension = JkUtilsString.substringAfterLast(
				source.getName(), ".");
		final String version = versionedModule.version().name();
		final String dest = destination(versionedModule.withVersion(version), extension, classifier);
		putAll(source, dest, false);
	}

	private static String destination(JkVersionedModule versionedModule,
			String ext, String classifier) {
		return destination(versionedModule, ext, classifier, versionedModule.version().name());
	}

	private static String destination(JkVersionedModule versionedModule,
			String ext, String classifier, String uniqueVersion) {
		final JkModuleId moduleId = versionedModule.moduleId();
		final String version = versionedModule.version().name();
		final StringBuilder result = new StringBuilder(moduleId.group())
		.append("/").append(moduleId.name()).append("/")
		.append(version).append("/").append(moduleId.name())
		.append("-").append(uniqueVersion);
		if (classifier != null) {
			result.append("-").append(classifier);
		}
		result.append(".").append(ext);
		return result.toString();
	}

	private static String versionForUniqueSnapshot(String version,
			String timestamp, int buildNumber) {
		return version.endsWith("-SNAPSHOT") ? JkUtilsString
				.substringBeforeLast(version, "-SNAPSHOT") + "-"
				+ timestamp + "-" + buildNumber : version;
	}

	private void updateMetadata(ModuleId moduleId, String version) {
		final String path = versionMetadataPath(of(moduleId, version));
		MavenMetadata mavenMetadata = loadMavenMedatata(path);
		if (mavenMetadata == null) {
			mavenMetadata = MavenMetadata.of(JkModuleId.of(
					moduleId.getOrganisation(), moduleId.getName()));
		}
		mavenMetadata.addVersion(version);
		push(mavenMetadata, path);
	}

	private void push(MavenMetadata metadata, String path) {
		final File file = JkUtilsFile.createTempFile("metadata-", ".xml");
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			metadata.output(outputStream);
			outputStream.flush();
		} catch (final FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		JkUtilsIO.closeQuietly(outputStream);
		putAll(file, path, true);
	}

	private static JkVersionedModule of(ModuleId moduleId, String version) {
		return JkVersionedModule.of(
				JkModuleId.of(moduleId.getOrganisation(), moduleId.getName()),
				JkVersion.ofName(version));
	}

	private static String versionMetadataPath(JkVersionedModule module) {
		return module.moduleId().group() + "/" + module.moduleId().name()
				+ "/maven-metadata.xml";
	}

	private static String snapshotMetadataPath(JkVersionedModule module) {
		return module.moduleId().group() + "/" + module.moduleId().name() + "/"
				+ module.version().name() + "/maven-metadata.xml";
	}

	private MavenMetadata loadMavenMedatata(String path) {
		try {
			final Resource resource = resolver.getRepository()
					.getResource(path);
			if (resource.exists()) {
				final InputStream inputStream = resource.openStream();
				final MavenMetadata mavenMetadata = MavenMetadata
						.of(inputStream);
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

	private void putAll(File source, String dest, boolean overwrite, boolean signIfneeded) {
		final String[] checksums = this.resolver.getChecksumAlgorithms();
		final Repository repository = this.resolver.getRepository();
		try {
			JkLog.info("publishing to " + dest);
			repository.put(null, source, dest, overwrite);
			for (final String algo : checksums) {
				final File temp = JkUtilsFile.createTempFile("jk-checksum-",
						algo);
				final String checkSum = ChecksumHelper.computeAsString(source,
						algo);
				JkUtilsFile.writeString(temp, checkSum, false);
				final String csDest = dest + "." + algo;
				JkLog.info("publishing to " + csDest);
				repository.put(null, temp, csDest, overwrite);
				temp.delete();
			}
			if (this.checkFileFlag.pgpSigner != null && signIfneeded) {
				final File signed = checkFileFlag.pgpSigner.sign(source)[0];
				final String signedDest = dest + ".asc";
				putAll(signed, signedDest, overwrite, false);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void abortPublishTransaction(DependencyResolver resolver) {
		try {
			resolver.abortPublishTransaction();
		} catch (final IOException e) {
			JkLog.warn("Publish transction hasn't been properly aborted");
			e.printStackTrace(JkLog.warnStream());
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
