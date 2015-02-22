package org.jake;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jake.depmanagement.JakeModuleId;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepo.MavenRepository;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeVersion;
import org.jake.depmanagement.JakeVersionedModule;
import org.jake.depmanagement.ivy.JakeIvy;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsString;
import org.jake.utils.JakeUtilsTime;

/**
 * Base class defining commons tasks and utilities
 * necessary for building any kind of project, regardless involved technologies.
 * 
 * @author Jerome Angibaud
 */
public class JakeBuild {

	private File baseDirFile = JakeUtilsFile.workingDir();

	private final Date buildTime = JakeUtilsTime.now();

	@JakeOption({"Maven or Ivy repositories to download dependency artifacts.",
	"Prefix the Url with 'ivy:' if it is an Ivy repostory."})
	private final String defaultDownloadRepoUrl = MavenRepository.MAVEN_CENTRAL_URL.toString();

	@JakeOption({"Maven or Ivy repositories to download dependency artifacts.",
	"Prefix the Url with 'ivy:' if it is an Ivy repostory."})
	private final String defaultUploadRepoUrl = null;

	@JakeOption({"Usename to connect to the upload repository (if needed).",
	"Null or blank means that the upload repository will be accessed in an anonymous way."})
	private final String defaultUploadRepoUsername = null;

	@JakeOption({"Password to connect to the upload repository (if needed)."})
	private final String defaultUploadRepoPassword = null;

	@JakeOption("Set it if the releases are uploaded on a distinct repository than the snaphot.")
	private final String defaultUploadRepoReleaseUrl = null;

	@JakeOption("Set it if the releases are uploaded on a distinct repository than the snaphot.")
	private final String defaultUploadRepoReleaseUsername = null;

	@JakeOption("Set it if the releases are uploaded on a distinct repository than the snaphot.")
	private final String defaultUploadRepoReleasePassword = null;

	protected JakeBuild() {
	}

	/**
	 * Defines this project base directory.
	 */
	public void setBaseDir(File baseDir) {
		this.baseDirFile = baseDir;
	}


	/**
	 * Returns the classes accepted as template for plugins.
	 * If you override it, do not forget to add the ones from the super class.
	 */
	protected List<Class<Object>> pluginTemplateClasses() {
		return Collections.emptyList();
	}

	/**
	 * Set the plugins to activate for this build.
	 * This method should be invoked after the {@link #setBaseDir(File)} method, so
	 * plugins can be configured using the proper base dir.
	 */
	protected void setPlugins(Iterable<?> plugins) {
		// Do nothing as no plugin extension as been defined at this level.
	}

	/**
	 * The current version for this project. Might look like "0.6.3", "0.1-SNAPSHOT" or "20141220170532".
	 * Default is the time stamp (formatted as 'yyyyMMdd-HHmmss') this build has been instantiated.
	 */
	public JakeVersion version() {
		return JakeVersion.named(JakeUtilsTime.timestampSec(buildTime));
	}

	/**
	 * The project name. This is likely to used in produced artifacts.
	 */
	public String projectName() {
		final String projectDirName = baseDir().root().getName();
		return projectDirName.contains(".") ? JakeUtilsString.substringAfterLast(projectDirName, ".") : projectDirName;
	}

	/**
	 * The project group name. This is likely to used in produced artifacts.
	 */
	public String groupName() {
		final String projectDirName = baseDir().root().getName();
		return projectDirName.contains(".") ? JakeUtilsString.substringBeforeLast(projectDirName, ".") : projectDirName;
	}

	/**
	 * By default, this method returns the concatenation of the project group and project name. It is likely to
	 * be used as produced artifacts file names.
	 */
	public String projectFullName() {
		if (groupName() == null || groupName().equals(projectName())) {
			return projectName();
		}
		return groupName()+ "." + projectName();
	}

	protected final JakeVersionedModule module() {
		return JakeVersionedModule.of(JakeModuleId.of(groupName(), projectName()), version());
	}

	/**
	 * Returns the parameterized JakeIvy instance to use when dealing with managed dependencies.
	 * If you don't use managed dependencies, this method is never invoked.
	 */
	protected JakeIvy jakeIvy() {
		return JakeIvy.of(uploadRepositories(), downloadRepositories());
	}

	/**
	 * Returns the download repositories where to retrieve artifacts. It has only a meaning in case of using
	 * managed dependencies.
	 */
	protected JakeRepos downloadRepositories() {
		return JakeRepos.of(JakeRepo.of(this.defaultDownloadRepoUrl));
	}

	/**
	 * Returns the upload repositories where to deploy artifacts.
	 */
	protected JakeRepos uploadRepositories() {

		return JakeRepos.of(JakeRepo.of(this.defaultDownloadRepoUrl));
	}

	protected Date buildTime() {
		return (Date) buildTime.clone();
	}


	/**
	 * Returns the base directory for this project. All file/directory path are
	 * resolved from this directory.
	 */
	public JakeDir baseDir() {
		return JakeDir.of(baseDirFile);
	}

	/**
	 * Return a file located at the specified path relative to the base directory.
	 */
	public File baseDir(String relativePath) {
		if (relativePath.isEmpty()) {
			return baseDirFile;
		}
		return baseDir().file(relativePath);
	}

	/**
	 * The output directory where all the final and intermediate
	 * artifacts are generated.
	 */
	public JakeDir ouputDir() {
		return baseDir().sub("build/output").createIfNotExist();
	}

	/**
	 * Returns the file located at the specified path relative to the output directory.
	 */
	public File ouputDir(String relativePath) {
		return ouputDir().file(relativePath);
	}

	// ------------ Operations ------------

	@JakeDoc("Clean the output directory.")
	public void clean() {
		JakeLog.start("Cleaning output directory " + ouputDir().root().getPath() );
		JakeUtilsFile.deleteDirContent(ouputDir().root());
		JakeLog.done();
	}

	@JakeDoc("Conventional method standing for the default operations to perform.")
	public void base() {
		clean();
	}

	@JakeDoc("Display all available methods defined in this build.")
	public void help() {
		HelpDisplayer.help(this);
	}

	@JakeDoc("Display details on all available plugins.")
	public void helpPlugins() {
		HelpDisplayer.helpPlugins(this);
	}




}
