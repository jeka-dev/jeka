package org.jake.java.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class JdkUtils {
	
	public static File toolsJar() {
		String jdkLocation = System.getProperty("java.home");
		File javaHome = new File(jdkLocation);
		try {
			return new File(javaHome, "../lib/tools.jar").getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static URLClassLoader classLoaderPlusTools(ClassLoader parent) {
		try {
			final URL[] urls = new URL[]{toolsJar().toURI().toURL()};
			return new URLClassLoader(urls, parent) ;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
	}

}
