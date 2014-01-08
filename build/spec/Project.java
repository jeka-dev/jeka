

import java.io.File;

import org.jake.java.JakeJarBuilder;

public class Project extends JakeJarBuilder {
	
	@Override
	protected String projectName() {
		return "jake";
	}
	
	@Override
	protected String version() {
		return "0.1-SNAPSHOT";
	}
	
	@Override
	public void jar() {
		super.jar();
		File jarFile = buildOuputDir().file("jake.jar");
		classDir().asZip(jarFile, zipLevel());
		logger().info(jarFile.getPath() + " created");
	}
	
	public static void main(String[] args) {
		new Project().doDefault();
	}

}
