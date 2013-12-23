

import org.javake.java.JarProjectBuilder;

public class Project extends JarProjectBuilder {
	
	@Override
	protected String projectName() {
		return "javake";
	}
	
	@Override
	protected String version() {
		return "0.1-SNAPSHOT";
	}
	
	@Override
	public void jar() {
		super.jar();
		classDir().asZip(buildOuputDir().file("javake.jar"), zipLevel());
	}
	
	public static void main(String[] args) {
		new Project().doDefault();
	}

}
