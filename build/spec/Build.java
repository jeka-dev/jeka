

import java.net.URLClassLoader;

import org.jake.java.JakeJarBuild;
import org.jake.java.utils.ClasspathUtils;

public class Build extends JakeJarBuild {
	
	@Override
	protected String projectName() {
		return "jake";
	}
	
	@Override
	protected String version() {
		return "0.1-SNAPSHOT";
	}
	
	@Override
	protected String jarName() {
		return this.projectName() + ".jar";
	}
	
			
	public static void main(String[] args) {
		ClasspathUtils.getSearchPath((URLClassLoader) Build.class.getClassLoader());
		new Build().doDefault();
	}

}
