package org.jake.depmanagement;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

public abstract class JakeRepo {

	public static JakeRepo ofOptional(String url, String userName, String password) {
		if (JakeUtilsString.isBlank(url)) {
			return null;
		}
		return of(url).withCredential(userName, password);
	}

	public static JakeRepo firstNonNull(JakeRepo ...repos) {
		for (final JakeRepo repo : repos) {
			if (repo != null) {
				return repo;
			}
		}
		return null;
	}

	public static MavenRepository maven(String url) {
		return new MavenRepository(toUrl(url), null, null);
	}

	public static MavenRepository maven(File file) {
		return new MavenRepository(JakeUtilsFile.toUrl(file), null, null);
	}

	public static JakeRepo mavenCentral() {
		return maven(MavenRepository.MAVEN_CENTRAL_URL.toString());
	}

	public static JakeRepo mavenJCenter() {
		return maven(MavenRepository.JCENTERL_URL.toString());
	}

	public static JakeRepo of(String url) {
		if (url.toLowerCase().startsWith("ivy:")) {
			return JakeRepo.ivy(url.substring(4));
		}
		return JakeRepo.maven(url);
	}

	public static JakeRepo.IvyRepository ivy(URL url) {
		return new IvyRepository(url, null, null, null, null);
	}

	public static JakeRepo.IvyRepository ivy(File file) {
		try {
			return ivy(file.toURI().toURL());
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static JakeRepo.IvyRepository ivy(String url) {
		try {
			return ivy(new URL(url));
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private final URL url;

	private final String userName;

	private final String password;

	private JakeRepo(URL url, String userName, String password) {
		this.url = url;
		this.userName = userName;
		this.password = password;
	}

	public final URL url() {
		return url;
	}

	public final String userName() {
		return userName;
	}

	public final String password() {
		return password;
	}

	public boolean hasCredentials() {
		return !JakeUtilsString.isBlank(userName);
	}

	public final JakeRepo withOptionalCredentials(String userName, String password) {
		if (JakeUtilsString.isBlank(userName)) {
			return this;
		}
		return this.withCredential(userName, password);
	}

	public abstract JakeRepo withCredential(String username, String password);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		final JakeRepo other = (JakeRepo) obj;
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else if (!url.equals(other.url)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + url + ")";
	}

	private static URL toUrl(String url) {
		try {
			return new URL(url);
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static final class MavenRepository extends JakeRepo {

		public static final URL MAVEN_CENTRAL_URL = toUrl("http://repo1.maven.org/maven2");

		public static final URL JCENTERL_URL = toUrl("https://jcenter.bintray.com");

		private MavenRepository(URL url, String userName, String password) {
			super(url, userName, password);
		}

		@Override
		public JakeRepo withCredential(String username, String password) {
			return new MavenRepository(this.url(), username, password);
		}



	}

	public static final class IvyRepository extends JakeRepo {

		private final List<String> artifactPatterns;

		private final List<String> ivyPatterns;

		private static final String DEFAULT_IVY_ARTIFACT_PATTERN = "[organisation]/[module]/[type]s/[artifact]-[revision].[ext]";

		private static final String DEFAULT_IVY_IVY_PATTERN = "[organisation]/[module]/ivy-[revision].xml";

		private IvyRepository(URL url, String username, String password, List<String> artifactPatterns, List<String> ivyPatterns) {
			super(url, username, password);
			this.artifactPatterns = artifactPatterns;
			this.ivyPatterns = ivyPatterns;
		}

		public IvyRepository artifactPatterns(String ...patterns) {
			return new IvyRepository(this.url(), this.userName(), this.password(), Collections.unmodifiableList(Arrays.asList(patterns)), ivyPatterns);
		}

		public IvyRepository ivyPatterns(String ...patterns) {
			return new IvyRepository(this.url(), this.userName(), this.password(), artifactPatterns, Collections.unmodifiableList(Arrays.asList(patterns)));
		}

		public List<String> artifactPatterns() {
			if (this.artifactPatterns == null) {
				return JakeUtilsIterable.listOf(DEFAULT_IVY_ARTIFACT_PATTERN);
			}
			return artifactPatterns;
		}

		public List<String> ivyPatterns() {
			if (this.ivyPatterns == null) {
				return JakeUtilsIterable.listOf(DEFAULT_IVY_IVY_PATTERN);
			}
			return ivyPatterns;
		}

		@Override
		public JakeRepo withCredential(String username, String password) {
			return new IvyRepository(this.url(), username, password, this.artifactPatterns, this.ivyPatterns);
		}
	}

}
