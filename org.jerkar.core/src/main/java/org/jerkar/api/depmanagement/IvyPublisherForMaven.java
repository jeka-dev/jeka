package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.jerkar.api.depmanagement.IvyPublisher.CheckFileFlag;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * {@link IvyPublisher} delegates to this class for publishing to Maven repositories.
 */
final class IvyPublisherForMaven {

	private static final String TS_PATTERN = "yyyyMMdd.HHmmss";

	private final DependencyResolver resolver;

	private final CheckFileFlag checkFileFlag;

	private final File descriptorOutputDir;

	IvyPublisherForMaven(CheckFileFlag checkFileFlag, DependencyResolver dependencyResolver, File descriptorOutputDir) {
		super();
		this.resolver = dependencyResolver;
		this.descriptorOutputDir = descriptorOutputDir;
		this.checkFileFlag = checkFileFlag;
	}

	/**
	 * Publish to maven repositories
	 */
	void publish(DefaultModuleDescriptor moduleDescriptor, JkMavenPublication publication, Date date) {
		ModuleRevisionId ivyModuleRevisionId = moduleDescriptor.getModuleRevisionId();
		ivyModuleRevisionId = withPattern(ivyModuleRevisionId, TS_PATTERN, JkUtilsTime.now());
		try {
			resolver.beginPublishTransaction(ivyModuleRevisionId, true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final ModuleRevisionId mrId = moduleDescriptor.getModuleRevisionId();
		final File pomXml = new File(targetDir(), "published-pom-" + mrId.getOrganisation() + "-"
				+ mrId.getName() + "-" + mrId.getRevision() + ".xml");
		final String packaging = JkUtilsString.substringAfterLast(publication.mainArtifactFile().getName(),".");
		final Artifact artifact = new DefaultArtifact(ivyModuleRevisionId, date, publication.artifactName(), "pom", "pom", true);
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
			resolver.publish(artifact, pomXml, true);
			checkFileFlag.publishChecks(resolver, artifact, pomXml);

			final Artifact mavenMainArtifact = IvyTranslations.toPublishedMavenArtifact(publication.mainArtifactFile(), publication.artifactName(),
					null, ivyModuleRevisionId, date);
			resolver.publish(mavenMainArtifact, publication.mainArtifactFile(), true);
			checkFileFlag.publishChecks(resolver, mavenMainArtifact, publication.mainArtifactFile());

			for (final Map.Entry<String, File> extraArtifact : publication.extraArtifacts().entrySet()) {
				final String classifier = extraArtifact.getKey();
				final File file = extraArtifact.getValue();
				final Artifact mavenArtifact = IvyTranslations.toPublishedMavenArtifact(file, publication.artifactName(),
						classifier, ivyModuleRevisionId, date);
				resolver.publish(mavenArtifact, file, true);
				checkFileFlag.publishChecks(resolver, mavenArtifact, file);
			}


		} catch (final Exception e) {
			abortPublishTransaction(resolver);
			if (JkUtilsThrowable.isInCause(e, SSLException.class)) {
				throw JkUtilsThrowable.unchecked(e, "Error related to SSH communication with " + resolver);
			}
			throw JkUtilsThrowable.unchecked(e);
		}
		commitPublication(resolver);
	}

	private static ModuleRevisionId withPattern(ModuleRevisionId original, String pattern, Date time) {
		if (pattern == null) {
			return original;
		}
		if (original.getRevision().contains("-SNAPSHOT")) {
			final String newRev = original.getRevision().replace("-SNAPSHOT", "-" + new SimpleDateFormat(pattern).format(time));
			return ModuleRevisionId.newInstance(original, newRev);
		}
		return original;
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
