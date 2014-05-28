

import org.jake.BuildOption;
import org.jake.java.JakeJarBuild;

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
		return this.projectName();  // Don't need the version info within the name
	}
	
			
	public static void main(String[] args) {
		BuildOption.set(args);
		new Build().doDefault();
	}

}
