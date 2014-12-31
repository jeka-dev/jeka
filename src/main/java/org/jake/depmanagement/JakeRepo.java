package org.jake.depmanagement;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class JakeRepo {

	public static MavenRepository maven(String url) {
		return new MavenRepository(toUrl(url));
	}

	public static JakeRepo mavenCentral() {
		return maven(MavenRepository.MAVEN_CENTRAL_URL.toString());
	}

	public static JakeRepo mavenJCenter() {
		return maven(MavenRepository.JCENTERL_URL.toString());
	}

	public static JakeRepo.IvyRepository ivy(URL url) {
		return new IvyRepository(url);
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

		private MavenRepository(URL url) {
			super(url);
		}

	}

	public static class IvyRepository extends JakeRepo {

		private IvyRepository(URL url) {
			super(url);
		}
	}

}
