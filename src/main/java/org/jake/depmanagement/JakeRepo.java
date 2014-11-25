package org.jake.depmanagement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class JakeRepo {

	@SuppressWarnings("unchecked")
	public static MavenRepository maven(String url) {
		return new MavenRepository(toUrl(url), Collections.EMPTY_LIST);
	}

	public static JakeRepo mavenCentral() {
		return maven(MavenRepository.MAVEN_CENTRAL_URL.toString());
	}

	public static JakeRepo mavenJCenter() {
		return maven(MavenRepository.JCENTERL_URL.toString());
	}

	private final URL url;

	private JakeRepo(URL url) {
		this.url = url;
	}

	public URL url() {
		return url;
	}

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

	public static class MavenRepository extends JakeRepo {

		public static final URL MAVEN_CENTRAL_URL = toUrl("http://repo1.maven.org/maven2");

		public static final URL JCENTERL_URL = toUrl("https://jcenter.bintray.com");

		private final List<URL> artifactUrls;

		private MavenRepository(URL url, List<URL> extraArtifactUrls) {
			super(url);
			this.artifactUrls = Collections.unmodifiableList(extraArtifactUrls);
		}

		public MavenRepository withArtifactUrl(String ... urls) {
			final List<URL> list = new LinkedList<URL>();
			for (final String url : urls) {
				list.add(toUrl(url));
			}
			return new MavenRepository(this.url(), list);
		}

		public List<URL> artifactUrls() {
			return artifactUrls;
		}
	}

}
