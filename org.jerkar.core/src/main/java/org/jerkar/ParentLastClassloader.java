package org.jerkar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/*
 * A parent last class loader is necessary if we want to invoke 3rd party libs that are not
 * part of the Jerkar classpath.
 * 
 * Kindly borrowed from http://stackoverflow.com/questions/5445511/how-do-i-create-a-parent-last-child-first-classloader-in-java-or-how-to-overr
 */
class ParentLastClassloader extends URLClassLoader {

	private final ClassLoader system;

	public ParentLastClassloader(URL[] classpath, ClassLoader parent) {
		super(classpath, parent);
		system = getSystemClassLoader();
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);
		if (c == null) {
			if (system != null) {
				try {
					// checking system: jvm classes, endorsed, cmd classpath, etc.
					c = system.loadClass(name);
				}
				catch (final ClassNotFoundException ignored) {
				}
			}
			if (c == null) {
				try {
					// checking local
					c = findClass(name);
				} catch (final ClassNotFoundException e) {
					// checking parent
					// This call to loadClass may eventually call findClass again, in case the parent doesn't find anything.
					c = super.loadClass(name, resolve);
				}
			}
		}
		if (resolve) {
			resolveClass(c);
		}
		return c;
	}

	@Override
	public URL getResource(String name) {
		URL url = null;
		if (system != null) {
			url = system.getResource(name);
		}
		if (url == null) {
			url = findResource(name);
			if (url == null) {
				// This call to getResource may eventually call findResource again, in case the parent doesn't find anything.
				url = super.getResource(name);
			}
		}
		return url;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		/**
		 * Similar to super, but local resources are enumerated before parent resources
		 */
		Enumeration<URL> systemUrls = null;
		if (system != null) {
			systemUrls = system.getResources(name);
		}
		final Enumeration<URL> localUrls = findResources(name);
		Enumeration<URL> parentUrls = null;
		if (getParent() != null) {
			parentUrls = getParent().getResources(name);
		}
		final List<URL> urls = new ArrayList<URL>();
		if (systemUrls != null) {
			while(systemUrls.hasMoreElements()) {
				urls.add(systemUrls.nextElement());
			}
		}
		if (localUrls != null) {
			while (localUrls.hasMoreElements()) {
				urls.add(localUrls.nextElement());
			}
		}
		if (parentUrls != null) {
			while (parentUrls.hasMoreElements()) {
				urls.add(parentUrls.nextElement());
			}
		}
		return new Enumeration<URL>() {
			Iterator<URL> iter = urls.iterator();

			@Override
			public boolean hasMoreElements() {
				return iter.hasNext();
			}
			@Override
			public URL nextElement() {
				return iter.next();
			}
		};
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		final URL url = getResource(name);
		try {
			return url != null ? url.openStream() : null;
		} catch (final IOException e) {
		}
		return null;
	}

}