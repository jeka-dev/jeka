package org.jake;

import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepo.MavenRepository;

class BootstrapOptions {

	@JakeOption({"Maven or Ivy repositories to download dependency artifacts.",
	"Prefix the Url with 'ivy:' if it is an Ivy repostory."})
	private final String downloadRepoUrl = MavenRepository.MAVEN_CENTRAL_URL.toString();

	@JakeOption({"Usename to connect to the download repository (if needed).",
	"Null or blank means that the upload repository will be accessed in an anonymous way."})
	private final String dowloadRepoUsername = null;

	@JakeOption({"Password to connect to the download repository (if needed)."})
	private final String downloadRepoPassword = null;

	public JakeRepo downloadRepo() {
		if (downloadRepoUrl == null) {
			return null;
		}
		return JakeRepo.of(downloadRepoUrl).withOptionalCredentials(dowloadRepoUsername, downloadRepoPassword);
	}

	public static BootstrapOptions createPopulatedWithOptions() {
		final BootstrapOptions result = new BootstrapOptions();
		JakeOptions.populateFields(result);
		return result;
	}

}
